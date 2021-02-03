package bftsmart.recovery.parallel.demo.kvs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

public class KSVServer extends DefaultSingleRecoverable {

	private ConcurrentHashMap<Integer, Integer> replicaMap;
	private Logger logger;

	public KSVServer(int id) {
		replicaMap = new ConcurrentHashMap<Integer, Integer>(0);
		logger = Logger.getLogger(KSVServer.class.getName());
		new ServiceReplica(id, this, this);
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: demo.map.MapServer <server id>");
			System.exit(-1);
		}
		new KSVServer(Integer.parseInt(args[0]));
	}

	@SuppressWarnings("unchecked")
	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		byte[] reply = null;
		Integer key = null;
		Integer value = null;
		boolean hasReply = false;
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
				ObjectInput objIn = new ObjectInputStream(byteIn);
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			KVSRequestType reqType = (KVSRequestType)objIn.readObject();
			switch (reqType) {
				case PUT:
					key = (Integer)objIn.readObject();
					value = (Integer)objIn.readObject();

					Integer oldValue = replicaMap.put(key, value);
					if (oldValue != null) {
						objOut.writeObject(oldValue);
						hasReply = true;
					}
					break;
				case GET:
					key = (Integer)objIn.readObject();
					value = replicaMap.get(key);
					if (value != null) {
						objOut.writeObject(value);
						hasReply = true;
					}
					break;
				case REMOVE:
					key = (Integer)objIn.readObject();
					value = replicaMap.remove(key);
					if (value != null) {
						objOut.writeObject(value);
						hasReply = true;
					}
					break;
				case SIZE:
					int size = replicaMap.size();
					objOut.writeInt(size);
					hasReply = true;
					break;
				case KEYSET:
					keySet(objOut);
					hasReply = true;
					break;
			}
			if (hasReply) {
				objOut.flush();
				byteOut.flush();
				reply = byteOut.toByteArray();
			} else {
				reply = new byte[0];
			}

		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}
		return reply;
	}

	@SuppressWarnings("unchecked")
	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		byte[] reply = null;
		Integer key = null;
		Integer value = null;
		boolean hasReply = false;

		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
				ObjectInput objIn = new ObjectInputStream(byteIn);
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			KVSRequestType reqType = (KVSRequestType)objIn.readObject();
			switch (reqType) {
				case GET:
					key = (Integer)objIn.readObject();
					value = replicaMap.get(key);
					if (value != null) {
						objOut.writeObject(value);
						hasReply = true;
					}
					break;
				case SIZE:
					int size = replicaMap.size();
					objOut.writeInt(size);
					hasReply = true;
					break;
				case KEYSET:
					keySet(objOut);
					hasReply = true;
					break;
				default:
					logger.log(Level.WARNING, "in appExecuteUnordered only read operations are supported");
			}
			if (hasReply) {
				objOut.flush();
				byteOut.flush();
				reply = byteOut.toByteArray();
			} else {
				reply = new byte[0];
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}

		return reply;
	}

	private void keySet(ObjectOutput out) throws IOException, ClassNotFoundException {
		Set<Integer> keySet = replicaMap.keySet();
		int size = replicaMap.size();
		out.writeInt(size);
		for (Integer key : keySet)
			out.writeObject(key);
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
			replicaMap = (ConcurrentHashMap<Integer, Integer>)objIn.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Error while installing snapshot", e);
		}
	}
}