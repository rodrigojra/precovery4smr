package bftsmart.parallel.recovery.demo.map;

import static com.codahale.metrics.MetricRegistry.name;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;

import bftsmart.parallel.recovery.GraphApplicationState;
import bftsmart.parallel.recovery.ParallelRecovery;
import bftsmart.parallel.recovery.demo.counter.CounterCommand;
import bftsmart.parallel.recovery.demo.counter.CounterServerFuture;
import bftsmart.statemanagement.ApplicationState;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;

public class KeyValueStoreServerConcurrent extends ParallelRecovery {

	private ConcurrentHashMap<Integer, Integer> replicaMap;
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private AtomicInteger iterations = new AtomicInteger(0);
	private MetricRegistry metrics;
	private Stats stats;
	boolean logMetrics = false;
	
	public KeyValueStoreServerConcurrent() {
		File path = createMetricsDirectory();
		metrics = new MetricRegistry();
		stats = new Stats(metrics);
		startReporting(metrics, path);		
	}
    private class Stats {
        Counter workloadSize;
        Meter commands;

        Stats(MetricRegistry metrics) {
        	workloadSize = metrics.counter(name(CounterServerFuture.class, "workloadSize"));
        	commands = metrics.meter(name(CounterServerFuture.class, "commands"));
        }
    }
    
	private File createMetricsDirectory() {
		Date date = new Date() ;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SS") ;
		File dir = new File("./metrics/" + dateFormat.format(date));
		
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				System.out.println("Can not create ./metrics directory.");
				System.exit(1);
			}
		} else if (!dir.isDirectory()) {
			System.out.println("./metrics must be a directory");
			System.exit(1);
		}
		return dir;
	}

	private void startReporting(MetricRegistry metrics, File path) {

        CsvReporter csvReporter =
                CsvReporter
                        .forRegistry(metrics)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .build(path);
        csvReporter.start(1, TimeUnit.SECONDS);

        if (logMetrics) {
            ConsoleReporter consoleReporter =
                    ConsoleReporter
                            .forRegistry(metrics)
                            .convertRatesTo(TimeUnit.SECONDS)
                            .build();
            consoleReporter.start(10, TimeUnit.SECONDS);
        }		
		
	}    
    
	public KeyValueStoreServerConcurrent(int id) {
		replicaMap = new ConcurrentHashMap<Integer, Integer>(0);
		new ServiceReplica(id, this, this);
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: demo.map.MapServer <server id>");
			System.exit(-1);
		}
		new KeyValueStoreServerConcurrent(Integer.parseInt(args[0]));
	}

	@Override
	public byte[] executeOrdered(byte[] bytes, MessageContext ctx) {
		return execute(bytes);
	}

	@Override
	public byte[] executeUnordered(byte[] bytes, MessageContext ctx) {
		return execute(bytes);
	}

	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		return execute(command);
	}

	private byte[] execute(byte[] bytes) {
		return delay.ensureMinCost(() -> {
			KeyValueStoreCmd cmd = KeyValueStoreCmd.wrap(bytes);
			ByteBuffer resp = ByteBuffer.allocate(4);
			resp.putInt(cmd.execute(replicaMap));
			iterations.incrementAndGet();
			return resp.array();
		});
	}

	@Override
	public byte[] getSnapshot() {
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
			objOut.writeObject(replicaMap);
			return byteOut.toByteArray();
		} catch (IOException e) {
			logger.error("Error while taking snapshot", e);
		}
		return new byte[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void installSnapshot(byte[] state) {
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(state);
				ObjectInput objIn = new ObjectInputStream(byteIn)) {
			replicaMap = (ConcurrentHashMap<Integer, Integer>) objIn.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.error("Error while installing snapshot", e);
		}
	}

	@Override
	public byte[] newAppExecuteOrdered(CounterCommand counterCommand) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getIterations() {
		return iterations.get();
	}
	
	@Override
	public int setState(ApplicationState recvState) {
		int lastCID = -1;
		if (recvState instanceof GraphApplicationState) {
			GraphApplicationState state = (GraphApplicationState) recvState;
			logger.info("Last CID in state: " + state.getLastCID());

			logLock.lock();
			if (!isJunit) {
				initLog();
				// log.update(state);
			}

			logLock.unlock();

			int lastCheckpointCID = state.getLastCheckpointCID();
			lastCID = state.getLastCID();
			logger.debug("I'm going to update myself from CID " + lastCheckpointCID + " to CID " + lastCID);
			stateLock.lock();
			installSnapshot(state.getState());
			Stopwatch stopwatch = Stopwatch.createStarted();
			ForkJoinPool pool = new ForkJoinPool(this.getNumberOfThreads());
			RecoveryDispatcherKVS recoveryDispatcher = new RecoveryDispatcherKVS(pool, metrics);
			logger.debug("Pool parallelism " + pool.getParallelism());
			recoveryDispatcher.setExecutor(this::newAppExecuteOrdered);

			for (int cid = lastCheckpointCID + 1; cid <= lastCID; cid++) {
				try {
					logger.debug("Processing and verifying batched requests for CID " + cid);
					List<KeyValueStoreCmd> commandList = state.getMessageListBatch(cid);

					for (KeyValueStoreCmd cmd : commandList) {
						recoveryDispatcher.post(cmd);
					}
				} catch (Exception e) {
					logger.error("Failed to process and verify batched requests", e);
					if (e instanceof ArrayIndexOutOfBoundsException) {
						logger.info("Last checkpoint, last consensus ID (CID): " + state.getLastCheckpointCID());
						logger.info("Last CID: " + state.getLastCID());
						logger.info("number of messages expected to be in the batch: "
								+ (state.getLastCID() - state.getLastCheckpointCID() + 1));
						logger.info("number of messages in the batch: " + state.getMessageBatches().length);
					}
				}
			}

			pool.shutdown();
			// next line will block till all tasks finishes
			try {
				pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			stopwatch.stop();
			logger.info("Recovery time elapsed MILLISECONDS: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
			logger.info("Recovery time elapsed SECONDS: " + stopwatch.elapsed(TimeUnit.SECONDS));

			stateLock.unlock();

		}

		return lastCID;
	}

	@Override
	public byte[] newAppExecuteOrdered(KeyValueStoreCmd cmd) {
		return delay.ensureMinCost(() -> {
			ByteBuffer resp = ByteBuffer.allocate(4);
			resp.putInt(cmd.execute(replicaMap));
			iterations.incrementAndGet();
			return resp.array();
		});
	}	
	
}