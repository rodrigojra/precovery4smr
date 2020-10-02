package bftsmart.parallel.recovery.demo.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import bftsmart.parallel.recovery.ParallelRecovery;
import bftsmart.parallel.recovery.demo.counter.CounterCommand;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;

public class KeyValueStoreServer extends ParallelRecovery {

	private ConcurrentHashMap<Integer, Integer> replicaMap;
	private static final Logger logger = Logger.getLogger(KeyValueStoreServer.class.getName());
	private AtomicInteger iterations = new AtomicInteger(0);

	public KeyValueStoreServer() {
	}

	public KeyValueStoreServer(int id) {
		replicaMap = new ConcurrentHashMap<Integer, Integer>(0);
		new ServiceReplica(id, this, this);
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: demo.map.MapServer <server id>");
			System.exit(-1);
		}
		new KeyValueStoreServer(Integer.parseInt(args[0]));
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
			logger.log(Level.SEVERE, "Error while taking snapshot", e);
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
			logger.log(Level.SEVERE, "Error while installing snapshot", e);
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
	public byte[] newAppExecuteOrdered(KeyValueStoreCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}
}