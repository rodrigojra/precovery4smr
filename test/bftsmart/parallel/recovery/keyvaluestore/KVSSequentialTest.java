package bftsmart.parallel.recovery.keyvaluestore;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
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

import bftsmart.parallel.recovery.demo.map.KeyValueStoreCmd;
import bftsmart.parallel.recovery.demo.map.KeyValueStoreServer;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.CommandsInfo;
import bftsmart.tom.server.defaultservices.DefaultApplicationState;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class KVSSequentialTest {

	private int COMMANDS_PER_BATCH;
	private int BATCH_SIZE;
	private int LAST_CID;
	private int CHECKPOINT_CID;
	float sparseness;

	public KVSSequentialTest(int workloadSize, float sparseness) {
		super();
		this.COMMANDS_PER_BATCH = workloadSize;
		this.sparseness = sparseness;
	}

	@Parameters
	public static Collection<Object[]> workloadSize() {
		return Arrays.asList(new Object[][] {
				// Workload size "0.01" "0.05" "0.1" "0.25" "0.5" "1"
				{ 5000, Float.parseFloat("0.1")}//,  { 5000, Float.parseFloat("0.2")},  { 5000, Float.parseFloat("0.3")},  { 5000, Float.parseFloat("0.4")},  { 5000, Float.parseFloat("0.5")}, 
//				{ 10000, Float.parseFloat("0.1")}, { 10000, Float.parseFloat("0.2")}, { 10000, Float.parseFloat("0.3")}, { 10000, Float.parseFloat("0.4")}, { 10000, Float.parseFloat("0.5")},
//				{ 20000, Float.parseFloat("0.1")}, { 20000, Float.parseFloat("0.2")}, { 20000, Float.parseFloat("0.3")}, { 20000, Float.parseFloat("0.4")}, { 20000, Float.parseFloat("0.5")} 
				});
	}

	@Before
	public void setUp() throws Exception {
		BATCH_SIZE = 1;
		System.out.println("Workload size = " + COMMANDS_PER_BATCH * BATCH_SIZE);
		CHECKPOINT_CID = 0;
		LAST_CID = BATCH_SIZE;
	}

	@Test
	public final void testSetState() throws NoSuchAlgorithmException {
		KeyValueStoreServer recovery = new KeyValueStoreServer();
		MessageDigest md = mock(MessageDigest.class);
		mockStatic(TOMUtil.class);
		when(TOMUtil.getHashEngine()).thenReturn(md);

		DefaultApplicationState recvState = mock(DefaultApplicationState.class);
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
		
		recovery.setConfig(configMock);
		recovery.setIsJunit(true);

		CommandsInfo cmdInfo = new CommandsInfo();
		MessageContext[] msgCtxs = new MessageContext[COMMANDS_PER_BATCH];
		byte[][] commands = new byte[COMMANDS_PER_BATCH][1];

		ThreadLocalRandom random = ThreadLocalRandom.current();
		int maxKey = COMMANDS_PER_BATCH;
		float conflict = Float.parseFloat("1");
		
		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			KeyValueStoreCmd cmd = KeyValueStoreCmd.random(random, maxKey, sparseness, conflict);
			commands[i] = cmd.encode();
		}

		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			msgCtxs[i] = mock(MessageContext.class);
		}

		cmdInfo.commands = commands;
		cmdInfo.msgCtx = msgCtxs;
		when(msgCtxs[0].isNoOp()).thenReturn(false);
		when(recvState.getMessageBatch(ArgumentMatchers.any(Integer.class))).thenReturn(cmdInfo);
		recovery.setState(recvState);
//		System.out.println(">> counter: " + recovery.getCounter());
		System.out.println(">> iterations: " + recovery.getIterations());

	}
}
