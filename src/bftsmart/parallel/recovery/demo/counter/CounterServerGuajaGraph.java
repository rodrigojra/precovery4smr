/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.parallel.recovery.demo.counter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import bftsmart.parallel.recovery.ParallelRecovery;

import com.google.common.base.Stopwatch;
import com.google.common.graph.MutableGraph;

import bftsmart.parallel.recovery.GraphApplicationState;
import bftsmart.parallel.recovery.SequentialRecovery;
import bftsmart.parallel.recovery.demo.map.KeyValueStoreCmd;
import bftsmart.statemanagement.ApplicationState;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;

/**
 * Example replica that implements a BFT replicated service (a counter). If the
 * increment > 0 the counter is incremented, otherwise, the counter value is
 * read.
 *
 * @author alysson
 * @author Rodrigo Antunes
 */

public final class CounterServerGuajaGraph extends ParallelRecovery {

    private AtomicInteger counter = new AtomicInteger(0);
    private AtomicInteger iterations = new AtomicInteger(0);

    public CounterServerGuajaGraph() {
    }

    public CounterServerGuajaGraph(int id) {
        new ServiceReplica(id, this, this);
    }

    public int getCounter() {
        return counter.get();
    }

    public int getIterations() {
        return iterations.get();
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

    @Override
    public byte[] newAppExecuteOrdered(CounterCommand counterCommand) {
        return delay.ensureMinCost(() -> {
            iterations.incrementAndGet();
            try {
                int increment = new DataInputStream(new ByteArrayInputStream(counterCommand.getData())).readInt();
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
            }
        });
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Use: java CounterServerConcurrent <processId>");
            System.exit(-1);
        }
        new CounterServerGuajaGraph(Integer.parseInt(args[0]));
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

            ForkJoinPool pool = new ForkJoinPool(this.getNumberOfThreads());
            logger.debug("Pool parallelism " + pool.getParallelism());
            Consumer<CounterCommand> executor = this::newAppExecuteOrdered;

            for (int cid = lastCheckpointCID + 1; cid <= lastCID; cid++) {
                try {
                    logger.debug("Processing and verifying batched requests for CID " + cid);
                    MutableGraph<CounterCommand> graph = state.getMessageMutableGraphBatch(cid);

                    while (graph.nodes().iterator().hasNext()) {
                        CounterCommand cmd =  graph.nodes().iterator().next();

                        Set<CounterCommand> successorNodes = graph.successors(cmd);
                        if (!successorNodes.isEmpty()) {
                            //System.out.println(Thread.currentThread().getName() + " - node " + cmd);
                            List<CompletableFuture<byte[]>> dependencies = new ArrayList<>();
                            while (successorNodes.iterator().hasNext()) {
                                CounterCommand succNode = successorNodes.iterator().next();
                                CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(
                                        () -> {
                                            return newAppExecuteOrdered(succNode);
                                        });
                                dependencies.add(future);
                            }
                            CompletableFuture[] cfDependencies = dependencies.toArray(new CompletableFuture[dependencies.size()]);
                            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(cfDependencies);
                            CompletableFuture<Void> cmdFuture = combinedFuture.thenRun(
                                    () -> {
                                        newAppExecuteOrdered(cmd);
                                    });

                            try {
                                combinedFuture.get();
                                cmdFuture.get();
                                graph.removeNode(cmd);
                            } catch (InterruptedException | ExecutionException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            pool.execute(() -> {
                                newAppExecuteOrdered(cmd);
                            });
                        }
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
            logger.info("Recovery time elapsed: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
            logger.info("Recovery time elapsed: " + stopwatch.elapsed(TimeUnit.SECONDS));

            stateLock.unlock();

        }

        return lastCID;
    }

	@Override
	public byte[] newAppExecuteOrdered(KeyValueStoreCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}
}
