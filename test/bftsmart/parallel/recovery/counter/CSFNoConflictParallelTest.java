package bftsmart.parallel.recovery.counter;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import bftsmart.parallel.recovery.GraphApplicationState;
import bftsmart.parallel.recovery.demo.counter.CounterCommand;
import bftsmart.parallel.recovery.demo.counter.CounterServerFuture;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class CSFNoConflictParallelTest {

	private int COMMANDS_PER_BATCH;
	private int BATCH_SIZE;
	private int LAST_CID;
	private int CHECKPOINT_CID;
	private int INCREMENT_VALUE;
	private int THREAD_POOL_SIZE;
	private CounterServerFuture countServerRecovery;
	private GraphApplicationState recvState;

	public CSFNoConflictParallelTest(int workloadSize, int threadPoolSize) {
		super();
		this.COMMANDS_PER_BATCH = workloadSize;
		this.THREAD_POOL_SIZE = threadPoolSize;
	}

	@Parameters
	public static Collection<Object[]> workloadVersusThreads() {
		return Arrays.asList(new Object[][] {
				// Workload Size | Thread Pool Size
				{ 5000,  2 }, { 5000,  4 }, { 5000,  6 }, { 5000,  8 },
				{ 10000, 2 }, { 10000, 4 }, { 10000, 6 }, { 10000, 8 },
				{ 20000, 2 }, { 20000, 4 }, { 20000, 6 }, { 20000, 8 }
		});
	}

	@Before
	public void setUp() throws Exception {
		//this.COMMANDS_PER_BATCH = 1;
		//this.THREAD_POOL_SIZE = 1;		
		BATCH_SIZE = 1;
		CHECKPOINT_CID = 0;
		LAST_CID = BATCH_SIZE;
		INCREMENT_VALUE = 1;

		countServerRecovery = new CounterServerFuture();
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
	public final void testSetStateFuture_no_dependency_02_threads() throws NoSuchAlgorithmException {
		System.out.println("********* Workload ["+ COMMANDS_PER_BATCH + "] Number of threads ["+THREAD_POOL_SIZE + "] *********");
		countServerRecovery.setNumberOfThreads(THREAD_POOL_SIZE);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand counterCommand = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(counterCommand);
		}

		when(recvState.getMessageListBatch1(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
/*
	@Test
	public final void testSetStateFuture_no_dependency_03_threads() throws NoSuchAlgorithmException {
		countServerRecovery.setNumberOfThreads(3);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand command = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(command);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	
	@Test
	public final void testSetStateFuture_no_dependency_04_threads() throws NoSuchAlgorithmException {
		countServerRecovery.setNumberOfThreads(4);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand command = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(command);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	
	@Test
	public final void testSetStateFuture_no_dependency_05_threads() throws NoSuchAlgorithmException {
		countServerRecovery.setNumberOfThreads(5);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand command = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(command);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	
	@Test
	public final void testSetStateFuture_no_dependency_06_threads() throws NoSuchAlgorithmException {
		countServerRecovery.setNumberOfThreads(6);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand command = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(command);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}

	@Test
	public final void testSetStateFuture_no_dependency_08_threads() throws NoSuchAlgorithmException {
		countServerRecovery.setNumberOfThreads(8);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand command = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(command);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	
	@Test
	public final void testSetStateFuture_no_dependency_16_threads() throws NoSuchAlgorithmException {
		countServerRecovery.setNumberOfThreads(16);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand command = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(command);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	
	@Test
	public final void testSetStateFuture_no_dependency_32_threads() throws NoSuchAlgorithmException {
		countServerRecovery.setNumberOfThreads(32);
		List<CounterCommand> commandList = new ArrayList<CounterCommand>(1);

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
			CounterCommand command = new CounterCommand(i, out.toByteArray(), msgContext);
			commandList.add(command);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	*/
}
