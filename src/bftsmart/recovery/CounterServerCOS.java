/**
 * 
 */
package bftsmart.recovery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import bftsmart.parallelism.late.graph.COS;
import bftsmart.parallelism.late.graph.DependencyGraph;
import bftsmart.statemanagement.ApplicationState;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.CommandsInfo;

/**
 * @author Rodrigo Antunes
 *
 */

public final class CounterServerCOS extends SequentialRecovery {

	private int counter = 0;
	private int iterations = 0;

	public CounterServerCOS() {
	}

	public CounterServerCOS(int id) {
		new ServiceReplica(id, this, this);
	}

	public int getCounter() {
		return counter;
	}

	public int getIterations() {
		return iterations;
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		iterations++;
		System.out.println("(" + iterations + ") Counter current value: " + counter);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			new DataOutputStream(out).writeInt(counter);
			return out.toByteArray();
		} catch (IOException ex) {
			System.err.println("Invalid request received!");
			return new byte[0];
		}
	}

	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		iterations++;
		try {
			int increment = new DataInputStream(new ByteArrayInputStream(command)).readInt();
			counter += increment;

			System.out.println("(" + iterations + ") Counter was incremented. Current value = " + counter);

			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			new DataOutputStream(out).writeInt(counter);
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
		new CounterServerCOS(Integer.parseInt(args[0]));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void installSnapshot(byte[] state) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(state);
			ObjectInput in = new ObjectInputStream(bis);
			counter = in.readInt();
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
			out.writeInt(counter);
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

			for (int cid = lastCheckpointCID + 1; cid <= lastCID; cid++) {
				try {
					logger.debug("Processing and verifying batched requests for CID " + cid);

					// CommandsInfo cmdInfo = state.getMessageBatch(cid);
					COS cmdGraph = state.getMessageGraphBatch(cid);

					Object node = cmdGraph.get();

					while (node != null) {
						DependencyGraph.vNode vNode = (DependencyGraph.vNode) node;
						CommandsInfo cmdInfo = (CommandsInfo) vNode.getData();
						if (cmdInfo != null) {
							// System.out.println("===> testGetNext: " + cmdInfo.getID());

							byte[][] cmds = cmdInfo.commands; // take a batch
							MessageContext[] msgCtxs = cmdInfo.msgCtx;

							if (cmds == null || msgCtxs == null || msgCtxs[0].isNoOp()) {
								continue;
							}

							for (int i = 0; i < cmds.length; i++) {
								appExecuteOrdered(cmds[i], msgCtxs[i]);
							}
						}

						// node = vNode.getNext();
						// cmdGraph.remove(node);
						// node = cmdGraph.get();
						break;
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

			stopwatch.stop();
			logger.info("Recovery time elapsed: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
			logger.info("Recovery time elapsed: " + stopwatch.elapsed(TimeUnit.SECONDS));

			stateLock.unlock();

		}

		return lastCID;
	}
}
