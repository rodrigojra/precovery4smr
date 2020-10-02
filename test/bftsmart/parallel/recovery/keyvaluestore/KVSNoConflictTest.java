package bftsmart.parallel.recovery.keyvaluestore;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

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
import bftsmart.parallel.recovery.demo.map.KeyValueStoreCmd;
import bftsmart.parallel.recovery.demo.map.KeyValueStoreServerConcurrent;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class KVSNoConflictTest {

	private int COMMANDS_PER_BATCH;
	private int BATCH_SIZE;
	private int LAST_CID;
	private int CHECKPOINT_CID;
	private int THREAD_POOL_SIZE;
	private KeyValueStoreServerConcurrent kvsServerRecovery;
	private GraphApplicationState recvState;

	public KVSNoConflictTest(int workloadSize, int threadPoolSize) {
		super();
		this.COMMANDS_PER_BATCH = workloadSize;
		this.THREAD_POOL_SIZE = threadPoolSize;
	}

	@Parameters
	public static Collection<Object[]> workloadVersusThreads() {
		return Arrays.asList(new Object[][] {
				// Workload Size | Thread Pool Size
				{ 1000,  2 }
				/*, { 5000,  4 }, { 5000,  6 }, { 5000,  8 },
				{ 10000, 2 }, { 10000, 4 }, { 10000, 6 }, { 10000, 8 },
				{ 20000, 2 }, { 20000, 4 }, { 20000, 6 }, { 20000, 8 }
				*/
		});
	}

	@Before
	public void setUp() throws Exception {
		//this.COMMANDS_PER_BATCH = 1;
		//this.THREAD_POOL_SIZE = 1;		
		BATCH_SIZE = 1;
		CHECKPOINT_CID = 0;
		LAST_CID = BATCH_SIZE;

		kvsServerRecovery = new KeyValueStoreServerConcurrent();
		MessageDigest md = mock(MessageDigest.class);
		mockStatic(TOMUtil.class);
		when(TOMUtil.getHashEngine()).thenReturn(md);

		recvState = mock(GraphApplicationState.class);
		when(recvState.getLastCID()).thenReturn(LAST_CID);
		when(recvState.getLastCheckpointCID()).thenReturn(CHECKPOINT_CID);
		byte[] state = null;

		ConcurrentHashMap<Integer, Integer> replicaMap = new ConcurrentHashMap<>(0);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(outputStream);
			oos.writeObject(replicaMap);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		state = outputStream.toByteArray();
		when(recvState.getState()).thenReturn(state);

		TOMConfiguration configMock = mock(TOMConfiguration.class);
		when(configMock.getCheckpointPeriod()).thenReturn(1000);
		when(configMock.isToLog()).thenReturn(false);
		kvsServerRecovery.setConfig(configMock);
		kvsServerRecovery.setIsJunit(true);

	}

	@Test
	public final void testSetStateFuture_no_dependency() throws NoSuchAlgorithmException {
		System.out.println("********* Workload ["+ COMMANDS_PER_BATCH + "] Number of threads ["+THREAD_POOL_SIZE + "] *********");
		kvsServerRecovery.setNumberOfThreads(THREAD_POOL_SIZE);
		List<KeyValueStoreCmd> commandList = new ArrayList<KeyValueStoreCmd>(1);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int maxKey = COMMANDS_PER_BATCH;
		float conflict = Float.parseFloat("1.0");
		float sparseness = Float.parseFloat("1.0");

		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			KeyValueStoreCmd cmd = KeyValueStoreCmd.random(random, maxKey, sparseness, conflict);
			MessageContext msgContext = mock(MessageContext.class);
			when(msgContext.isNoOp()).thenReturn(false);
			commandList.add(cmd);
		}

		when(recvState.getMessageListBatch(ArgumentMatchers.any(Integer.class))).thenReturn(commandList);
		kvsServerRecovery.setState(recvState);
		System.out.println(">> iterations: " + kvsServerRecovery.getIterations());
	}
}
