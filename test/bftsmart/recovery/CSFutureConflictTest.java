package bftsmart.recovery;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.recovery.Command.Type;
import bftsmart.tom.MessageContext;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class CSFutureConflictTest {

	private int COMMANDS_PER_BATCH;
	private int BATCH_SIZE;
	private int LAST_CID;
	private int CHECKPOINT_CID;
	private int INCREMENT_VALUE;
	private CounterServerFuture countServerRecovery;
	private GraphApplicationState recvState;
	private int THREAD_POOL_SIZE;
	private int conflictProbabilityPercentage;
	
	
	public  CSFutureConflictTest(int workloadSize, int conflictProbabilityPercentage) {
		super();
		this.COMMANDS_PER_BATCH = workloadSize;
		this.conflictProbabilityPercentage = conflictProbabilityPercentage;
	}

	@Parameters
	public static Collection<Object[]> workloadVersusConflict() {
		return Arrays.asList(new Object[][] {
				// Workload Size | Thread Pool Size
				{ 1000, 5 }, { 1000, 10 }, { 1000, 25 }, { 1000, 50 }, { 1000, 75 }, 
				{ 10000, 5 }, { 10000, 10 }, { 10000, 25 }, { 10000, 50 }, { 10000, 75 },
				{ 100000, 5 }, { 100000, 10 }, { 100000, 25 }, { 100000, 50 }, { 100000, 75 }
		});
	}

	@Before
	public void setUp() throws Exception {
		THREAD_POOL_SIZE = 6;
		COMMANDS_PER_BATCH = 5000;
		BATCH_SIZE = 1;
		CHECKPOINT_CID = 0;
		LAST_CID = BATCH_SIZE;
		INCREMENT_VALUE = 1;
		System.out.println("Workload size = " + COMMANDS_PER_BATCH * BATCH_SIZE);
		countServerRecovery = new CounterServerFuture();
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
		List<Command> commandList = new ArrayList<Command>(1);
		generateCommandListConflictProbabilityBased(commandList);
		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}

	private void generateCommandListConflictProbabilityBased(List<Command> commandList) {
        Random random = new Random();
		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			ByteArrayOutputStream out = new ByteArrayOutputStream(4);
			try {
				new DataOutputStream(out).writeInt(INCREMENT_VALUE);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			MessageContext msgContext = mock(MessageContext.class);
			when(msgContext.isNoOp()).thenReturn(false);
			Command command = new Command(i, out.toByteArray(), msgContext);

	        int r = random.nextInt(100);
	        
	        if (r < this.conflictProbabilityPercentage) {
	        	command.setType(Type.CONFLICT);
	        } else {
	        	command.setType(Type.PARALLEL);
	        }

			commandList.add(command);
		}
	}
/*
	@Test
	public final void testSetStateFuture_dependency_25_percent() throws NoSuchAlgorithmException {
		//countServerRecovery.setNumberOfThreads(4);
		List<Command> commandList = new ArrayList<Command>(1);
		int dependency_25_percent = 25;
		System.out.println(dependency_25_percent + "% of conflict probaility ---");
		
		generateCommandListConflictProbabilityBased(commandList, dependency_25_percent);

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	
	@Test
	public final void testSetStateFuture_dependency_50_percent() throws NoSuchAlgorithmException {
		//countServerRecovery.setNumberOfThreads(4);
		List<Command> commandList = new ArrayList<Command>(1);
		int dependency_50_percent = 50;
		System.out.println(dependency_50_percent + "% of conflict probaility ---");

		generateCommandListConflictProbabilityBased(commandList, dependency_50_percent);

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	
	@Test
	public final void testSetStateFuture_dependency_75_percent() throws NoSuchAlgorithmException {
		//countServerRecovery.setNumberOfThreads(4);
		List<Command> commandList = new ArrayList<Command>(1);
		int dependency_75_percent = 75;
		System.out.println(dependency_75_percent + "% of conflict probaility ---");

		generateCommandListConflictProbabilityBased(commandList, dependency_75_percent);

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}	
	
	@Test
	public final void testSetStateFuture_dependency_90_percent() throws NoSuchAlgorithmException {
		//countServerRecovery.setNumberOfThreads(4);
		List<Command> commandList = new ArrayList<Command>(1);
		int dependency_90_percent = 90;
		System.out.println(dependency_90_percent + "% of conflict probaility ---");
		generateCommandListConflictProbabilityBased(commandList, dependency_90_percent);
		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}	
*/	
}
