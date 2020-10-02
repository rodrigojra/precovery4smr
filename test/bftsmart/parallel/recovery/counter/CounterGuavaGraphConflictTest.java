package bftsmart.parallel.recovery.counter;

import bftsmart.parallel.recovery.GraphApplicationState;
import bftsmart.parallel.recovery.demo.counter.CounterCommand;
import bftsmart.parallel.recovery.demo.counter.CounterServerGuajaGraph;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.util.TOMUtil;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class CounterGuavaGraphConflictTest {
    private int COMMANDS_PER_BATCH;
    private int BATCH_SIZE;
    private int LAST_CID;
    private int CHECKPOINT_CID;
    private int INCREMENT_VALUE;
    private CounterServerGuajaGraph countServerRecovery;
    private GraphApplicationState recvState;
    private int THREAD_POOL_SIZE;
    private int conflictProbabilityPercentage;
    
	@Test
	public void testHello() {
		System.out.println("No valid test file");
	}

    @Parameterized.Parameters
    public static Collection<Object[]> workloadVersusConflict() {
        return Arrays.asList(new Object[][] {});
    }	
/*
    public CounterGuavaGraphConflictTest(int workloadSize, int conflictProbabilityPercentage) {
        super();
        this.COMMANDS_PER_BATCH = workloadSize;
        this.conflictProbabilityPercentage = conflictProbabilityPercentage;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> workloadVersusConflict() {
        return Arrays.asList(new Object[][] {
                // Workload Size | Thread Pool Size
                { 10,  100 }//, { 5000,  20 }, { 5000,  30 }, { 5000,  40 }, { 5000,  50 }, { 5000,  60 }, { 5000,  70 }, { 5000,  80 }, { 5000,  90 }, { 5000,  100 }
                //{ 10000,  10 }, { 10000,  20 }, { 10000, 30 }, { 10000, 40 }, { 10000, 50 }, { 10000, 60 }, { 10000, 70 }, { 10000, 80 }, { 10000, 90 }, { 10000, 100 }
                //{ 20000,  10 }, { 20000,  20 }, { 20000,  30 }, { 20000,  40 }, { 20000,  50 }, { 20000,  60 }, { 20000,  70 }, { 20000,  80 }, { 20000,  90 }, { 20000,  100 }
        });
    }

    @Before
    public void setUp() throws Exception {
        THREAD_POOL_SIZE = 8;
        //COMMANDS_PER_BATCH = 5000;
        BATCH_SIZE = 1;
        CHECKPOINT_CID = 0;
        LAST_CID = BATCH_SIZE;
        INCREMENT_VALUE = 1;
        System.out.println("Workload size = " + COMMANDS_PER_BATCH * BATCH_SIZE);
        countServerRecovery = new CounterServerGuajaGraph();
        countServerRecovery.setNumberOfThreads(THREAD_POOL_SIZE);
        MessageDigest md = mock(MessageDigest.class);
        mockStatic(TOMUtil.class);
        when(TOMUtil.getHashEngine()).thenReturn(md);

        recvState = mock(GraphApplicationState.class);
        when(recvState.getLastCID()).thenReturn(LAST_CID);
        when(recvState.getLastCheckpointCID()).thenReturn(CHECKPOINT_CID);
        byte[] state = null;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(4);
        try {
            new DataOutputStream(outputStream).writeInt(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        state = outputStream.toByteArray();
        when(recvState.getState()).thenReturn(state);

        TOMConfiguration configMock = mock(TOMConfiguration.class);
        when(configMock.getCheckpointPeriod()).thenReturn(1000);
        when(configMock.isToLog()).thenReturn(false);
        countServerRecovery.setConfig(configMock);
        countServerRecovery.setIsJunit(true);
    }

    @Test
    public final void testSetStateFutureDependencyTest() throws NoSuchAlgorithmException {
        System.out.println("********* Workload ["+ COMMANDS_PER_BATCH + "] Conflict Probability Percentage ["+ this.conflictProbabilityPercentage + "%] *********");
        MutableGraph<CounterCommand> graph = generateGraph();
        when(recvState.getMessageMutableGraphBatch(ArgumentMatchers.any(Integer.class))).thenReturn(graph);
        countServerRecovery.setState(recvState);
        System.out.println(">> counter: " + countServerRecovery.getCounter());
        System.out.println(">> iterations: " + countServerRecovery.getIterations());
    }

    private MutableGraph<CounterCommand> generateGraph() {
        MutableGraph<CounterCommand> graph = GraphBuilder.directed().build();
        MessageContext firstMsgContext = mock(MessageContext.class);
        when(firstMsgContext.isNoOp()).thenReturn(false);
        ByteArrayOutputStream firstOut = new ByteArrayOutputStream(4);
        try {
            new DataOutputStream(firstOut).writeInt(INCREMENT_VALUE);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        CounterCommand firstCommand = new CounterCommand(0, firstOut.toByteArray(), firstMsgContext);
        firstCommand.setType(CounterCommand.Type.PARALLEL);
        graph.addNode(firstCommand);
        Random random = new Random();
        CounterCommand counterCommand = firstCommand;

        for (int i = 1; i < COMMANDS_PER_BATCH; i++) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            try {
                new DataOutputStream(out).writeInt(INCREMENT_VALUE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MessageContext msgContext = mock(MessageContext.class);
            when(msgContext.isNoOp()).thenReturn(false);
            CounterCommand newCommand = new CounterCommand(i, out.toByteArray(), msgContext);
            int r = random.nextInt(100);

            if (r < this.conflictProbabilityPercentage) {
                counterCommand.setType(CounterCommand.Type.CONFLICT);
                graph.putEdge(counterCommand, newCommand);
            } else {
                counterCommand.setType(CounterCommand.Type.PARALLEL);
                graph.addNode(newCommand);
            }
            counterCommand = newCommand;
        }
        return graph;
    }
*/
}
