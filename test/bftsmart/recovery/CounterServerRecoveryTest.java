package bftsmart.recovery;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import bftsmart.demo.counter.CounterServer;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.CommandsInfo;
import bftsmart.tom.server.defaultservices.DefaultApplicationState;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Parameterized.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class CounterServerRecoveryTest {

	private int COMMANDS_PER_BATCH;
	private int BATCH_SIZE;
	private int LAST_CID;
	private int CHECKPOINT_CID;
	
	public CounterServerRecoveryTest(int workloadSize) {
		super();
		this.COMMANDS_PER_BATCH = workloadSize;
	}

	@Parameters
	public static Collection<Object[]> workloadSize() {
		return Arrays.asList(new Object[][] {
				//Workload size
				{ 5000  }, 
				{ 10000 },
				{ 20000 }
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
		CounterServer countServerRecovery = new CounterServer();
		MessageDigest md = mock(MessageDigest.class);
		mockStatic(TOMUtil.class);
		when(TOMUtil.getHashEngine()).thenReturn(md);

		DefaultApplicationState recvState = mock(DefaultApplicationState.class);
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

		CommandsInfo cmdInfo = new CommandsInfo();
		MessageContext[] msgCtxs = new MessageContext[COMMANDS_PER_BATCH];
		byte[][] commands = new byte[COMMANDS_PER_BATCH][1];
		int incrementValue = 1;

		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			for (int j = 0; j < 1; j++) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(4);
				try {
					new DataOutputStream(out).writeInt(incrementValue);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				commands[i] = out.toByteArray();
			}
		}

		/*
		 * for (int i = 0; i < commands.length; i++) { for (int j = 0; j <
		 * commands[i].length; j++) { System.out.print(commands[i][j] + " "); }
		 * System.out.println(); }
		 */

		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			msgCtxs[i] = mock(MessageContext.class);
		}

		cmdInfo.commands = commands;
		cmdInfo.msgCtx = msgCtxs;
		when(msgCtxs[0].isNoOp()).thenReturn(false);
		when(recvState.getMessageBatch(ArgumentMatchers.any(Integer.class))).thenReturn(cmdInfo);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());

	}
}
