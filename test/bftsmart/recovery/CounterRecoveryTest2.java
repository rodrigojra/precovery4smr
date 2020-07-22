package bftsmart.recovery;

import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import bftsmart.demo.counter.CounterServer;
import bftsmart.parallelism.late.graph.COS;
import bftsmart.parallelism.late.graph.LockFreeGraph;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.CommandsInfo;
import bftsmart.tom.server.defaultservices.DefaultApplicationState;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class CounterRecoveryTest2 {
	
	private int COMMANDS_PER_BATCH;
	private int BATCH_SIZE;
	private int LAST_CID;
	private int CHECKPOINT_CID;
	

	@Before
	public void setUp() throws Exception {
		COMMANDS_PER_BATCH = 10000;
		BATCH_SIZE = 1;
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
		byte [] state = null;
		
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
		byte [][] commands = new byte[COMMANDS_PER_BATCH][1];
		int incrementValue = 1;
		
		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			//for (int j = 0; j < 1 ; j++) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(4);
                try {
					new DataOutputStream(out).writeInt(incrementValue);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				commands[i] = out.toByteArray(); 
			//}
		}
		
/*		
		for (int i = 0; i < commands.length; i++) {
		    for (int j = 0; j < commands[i].length; j++) {
		        System.out.print(commands[i][j] + " ");
		    }
		    System.out.println();
		}
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
	
	@Test
	public final void testSetStateWithGraphNoDependecies() throws NoSuchAlgorithmException {
		CounterServer4Graph countServerRecovery = new CounterServer4Graph();
		MessageDigest md = mock(MessageDigest.class);
		mockStatic(TOMUtil.class);
		when(TOMUtil.getHashEngine()).thenReturn(md);
		
		GraphApplicationState recvState = mock(GraphApplicationState.class);
		when(recvState.getLastCID()).thenReturn(LAST_CID);
		when(recvState.getLastCheckpointCID()).thenReturn(CHECKPOINT_CID);
		byte [] state = null;
		
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

		MessageContext[] msgCtxs = new MessageContext[COMMANDS_PER_BATCH];
		byte [][] commands = new byte[COMMANDS_PER_BATCH][1];
		int incrementValue = 1;
		
		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			//for (int j = 0; j < 1 ; j++) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(4);
                try {
					new DataOutputStream(out).writeInt(incrementValue);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				commands[i] = out.toByteArray(); 
			//}
		}
		
		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			msgCtxs[i] = mock(MessageContext.class);
		}
		
		when(msgCtxs[0].isNoOp()).thenReturn(false);

		COS cos0 = new LockFreeGraph(2), cos1 = new LockFreeGraph(2), cos2 = new LockFreeGraph(2), cos3 = new LockFreeGraph(2),
				cos4 = new LockFreeGraph(2), cos5 = new LockFreeGraph(2), cos6 = new LockFreeGraph(2), 
				cos7 = new LockFreeGraph(2), cos8 = new LockFreeGraph(2), cos9 = new LockFreeGraph(2);

		for (int i = 0; i < 2; i++) {
			try {
				CommandsInfo commandsInfo = new CommandsInfo(Integer.valueOf(i));
				commandsInfo.commands = commands;
				commandsInfo.msgCtx = msgCtxs;
				cos0.insert(commandsInfo);
				cos1.insert(commandsInfo);
				cos2.insert(commandsInfo);
				cos3.insert(commandsInfo);
				cos4.insert(commandsInfo);
				cos5.insert(commandsInfo);
				cos6.insert(commandsInfo);
				cos7.insert(commandsInfo);
				cos8.insert(commandsInfo);
				cos9.insert(commandsInfo);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail("LockFreeGraphTest:setUp:fail");
			}
		}
		
		
		when(recvState.getMessageGraphBatch(ArgumentMatchers.any(Integer.class))).thenReturn(cos0, cos1,cos2,cos3, cos4, cos5, cos6, cos7, cos8, cos9);
		//when(recvState.getMessageBatch(ArgumentMatchers.any(Integer.class))).thenReturn(cmdInfo);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
		
	}

	@Test
	public final void testSetStateThreads() throws NoSuchAlgorithmException {
		CounterServerConcurrent countServerRecovery = new CounterServerConcurrent();
		MessageDigest md = mock(MessageDigest.class);
		mockStatic(TOMUtil.class);
		when(TOMUtil.getHashEngine()).thenReturn(md);
		
		GraphApplicationState recvState = mock(GraphApplicationState.class);
		when(recvState.getLastCID()).thenReturn(LAST_CID);
		when(recvState.getLastCheckpointCID()).thenReturn(CHECKPOINT_CID);
		byte [] state = null;
		
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

		MessageContext[] msgCtxs = new MessageContext[COMMANDS_PER_BATCH];
		byte [][] commands = new byte[COMMANDS_PER_BATCH][1];
		int incrementValue = 1;
		
		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			//for (int j = 0; j < 1 ; j++) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(4);
                try {
					new DataOutputStream(out).writeInt(incrementValue);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				commands[i] = out.toByteArray(); 
			//}
		}
		
		for (int i = 0; i < COMMANDS_PER_BATCH; i++) {
			msgCtxs[i] = mock(MessageContext.class);
		}
		
		when(msgCtxs[0].isNoOp()).thenReturn(false);

		COS cos0 =     new LockFreeGraph(1), cos1 = new LockFreeGraph(1), cos2 = new LockFreeGraph(1), cos3 = new LockFreeGraph(1),
				cos4 = new LockFreeGraph(1), cos5 = new LockFreeGraph(1), cos6 = new LockFreeGraph(1), 
				cos7 = new LockFreeGraph(1), cos8 = new LockFreeGraph(1), cos9 = new LockFreeGraph(1);

		for (int i = 0; i < 1; i++) {
			try {
				CommandsInfo commandsInfo = new CommandsInfo(Integer.valueOf(i));
				commandsInfo.commands = commands;
				commandsInfo.msgCtx = msgCtxs;
				cos0.insert(commandsInfo);
				cos1.insert(commandsInfo);
				cos2.insert(commandsInfo);
				cos3.insert(commandsInfo);
				cos4.insert(commandsInfo);
				cos5.insert(commandsInfo);
				cos6.insert(commandsInfo);
				cos7.insert(commandsInfo);
				cos8.insert(commandsInfo);
				cos9.insert(commandsInfo);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fail("LockFreeGraphTest:setUp:fail");
			}
		}
		
		
		when(recvState.getMessageGraphBatch(ArgumentMatchers.any(Integer.class))).thenReturn(cos0, cos1,cos2,cos3, cos4, cos5, cos6, cos7, cos8, cos9);
		//when(recvState.getMessageBatch(ArgumentMatchers.any(Integer.class))).thenReturn(cmdInfo);
		countServerRecovery.setState(recvState);
		System.out.println(">> counter: " + countServerRecovery.getCounter());
		System.out.println(">> iterations: " + countServerRecovery.getIterations());
	}
}
