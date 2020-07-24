/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.recovery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;

import bftsmart.statemanagement.ApplicationState;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Example replica that implements a BFT replicated service (a counter). If the
 * increment > 0 the counter is incremented, otherwise, the counter value is
 * read.
 * 
 * @author alysson
 * @author Rodrigo Antunes
 */

public final class CounterServerFuture extends ParallelRecovery {

	private AtomicInteger counter = new AtomicInteger(0);
	private AtomicInteger iterations = new AtomicInteger(0);
	private int numberOfThreads;
	private Stats stats;
	private MetricRegistry metrics;
	boolean logMetrics = false;
	
	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public CounterServerFuture() {
		File path = createMetricsDirectory();
		metrics = new MetricRegistry();
		stats = new Stats(metrics);
		startReporting(metrics, path);
	}

	public CounterServerFuture(int id) {
		new ServiceReplica(id, this, this);
	}

	public int getCounter() {
		return counter.get();
	}

	public int getIterations() {
		return iterations.get();
	}

	private File createMetricsDirectory() {
		File dir = new File("./metrics");
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

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		iterations.incrementAndGet();
		System.out.println("(" + iterations + ") Counter current value: " + counter);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			new DataOutputStream(out).writeInt(counter.get());
			return out.toByteArray();
		} catch (IOException ex) {
			System.err.println("Invalid request received!");
			return new byte[0];
		}
	}

	@Override
	public byte[] newAppExecuteOrdered(Command command) {
		return delay.ensureMinCost(() -> {
			iterations.incrementAndGet();
			try {
				int increment = new DataInputStream(new ByteArrayInputStream(command.getData())).readInt();
				// counter += increment;
				counter.addAndGet(increment);
				// System.out.println(Thread.currentThread().getName() + " - command " +
				// command.toString() + " - Counter was incremented. Current value = " +
				// counter);
				ByteArrayOutputStream out = new ByteArrayOutputStream(4);
				new DataOutputStream(out).writeInt(counter.get());
				return out.toByteArray();
			} catch (IOException ex) {
				System.err.println("Invalid request received!");
				return new byte[0];
			} finally {
				stats.commands.mark();
			}
		});
	}

	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		iterations.incrementAndGet();
		try {
			int increment = new DataInputStream(new ByteArrayInputStream(command)).readInt();
			// counter += increment;
			counter.addAndGet(increment);

			// System.out.println("(" + iterations + ") Counter was incremented. Current
			// value = " + counter);

			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			new DataOutputStream(out).writeInt(counter.get());
			return out.toByteArray();
		} catch (IOException ex) {
			System.err.println("Invalid request received!");
			return new byte[0];
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Use: java CounterServerConcurrent <processId>");
			System.exit(-1);
		}
		new CounterServerFuture(Integer.parseInt(args[0]));
	}

	@Override
	public void installSnapshot(byte[] state) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(state);
			ObjectInput in = new ObjectInputStream(bis);
			// counter = in.readInt();
			counter.addAndGet(in.readInt());

			in.close();
			bis.close();
		} catch (IOException e) {
			System.err.println("[ERROR] Error deserializing state: " + e.getMessage());
		}
	}

	@Override
	public byte[] getSnapshot() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeInt(counter.get());
			out.flush();
			bos.flush();
			out.close();
			bos.close();
			return bos.toByteArray();
		} catch (IOException ioe) {
			System.err.println("[ERROR] Error serializing state: " + ioe.getMessage());
			return "ERROR".getBytes();
		}
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
			// TODO Mover para o arquivo de configuração
			// int nThreads = 5;
			// ForkJoinPool pool = new ForkJoinPool(this.numberOfThreads,
			// ForkJoinPool.defaultForkJoinWorkerThreadFactory,null, true, nThreads,
			// nThreads, 0, null, 60, TimeUnit.SECONDS);
			ForkJoinPool pool = new ForkJoinPool(this.numberOfThreads);
			// PooledScheduler pooledScheduler = new PooledScheduler(pool);
			PooledScheduler2 pooledScheduler = new PooledScheduler2(pool, metrics);

			logger.debug("Pool parallelism " + pool.getParallelism());
			logger.debug("Pool size " + pool.getPoolSize());
			logger.debug("Pool Active Thread Count " + pool.getActiveThreadCount());
			// pooledScheduler.setExecutor(a ->
			// System.out.println(Thread.currentThread().getName() + " - " +a));
			pooledScheduler.setExecutor(this::newAppExecuteOrdered);

			for (int cid = lastCheckpointCID + 1; cid <= lastCID; cid++) {
				try {
					logger.debug("Processing and verifying batched requests for CID " + cid);
					List<Command> commandList = state.getMessageListBatch(cid);

					for (Command command : commandList) {
						pooledScheduler.schedule(command);
						stats.workloadSize.inc();
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

    private class Stats {
        Counter workloadSize;
        Meter commands;

        Stats(MetricRegistry metrics) {
        	workloadSize = metrics.counter(name(CounterServerFuture.class, "workloadSize"));
        	commands = metrics.meter(name(CounterServerFuture.class, "commands"));
        }
    }
}
