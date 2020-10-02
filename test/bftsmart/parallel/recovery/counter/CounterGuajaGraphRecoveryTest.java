package bftsmart.parallel.recovery.counter;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import bftsmart.parallel.recovery.GraphApplicationState;
import bftsmart.parallel.recovery.demo.counter.CounterCommand;
import bftsmart.parallel.recovery.demo.counter.CounterServerFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import bftsmart.parallel.recovery.demo.counter.CounterServerGuajaGraph;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class CounterGuajaGraphRecoveryTest {
	
	@Test
	public void testHello() {
		System.out.println("No valid test file");
	}
/*
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
	public final void testSetStateGuajaGraphNoDep() throws NoSuchAlgorithmException {
		CounterServerGuajaGraph countServerRecovery = new CounterServerGuajaGraph();
		MessageDigest md = mock(MessageDigest.class);
		mockStatic(TOMUtil.class);
		when(TOMUtil.getHashEngine()).thenReturn(md);

		GraphApplicationState recvState = mock(GraphApplicationState.class);
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

		// MessageContext[] msgCtxs = new MessageContext[COMMANDS_PER_BATCH];
		// byte [][] commands = new byte[COMMANDS_PER_BATCH][1];

		MutableGraph<CounterCommand> graph = GraphBuilder.directed().build();

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
			graph.addNode(command);
		}


		when(recvState.getMessageMutableGraphBatch(ArgumentMatchers.any(Integer.class))).thenReturn(graph);
		// when(recvState.getMessageBatch(ArgumentMatchers.any(Integer.class))).thenReturn(cmdInfo);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
	*/
}
