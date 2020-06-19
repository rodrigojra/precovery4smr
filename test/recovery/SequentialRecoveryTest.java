package recovery;

import static org.junit.Assert.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.powermock.api.mockito.PowerMockito.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import bftsmart.recovery.SequentialRecovery;
import bftsmart.statemanagement.ApplicationState;
import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultApplicationState;
import bftsmart.tom.util.TOMUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "bftsmart.*")
public class SequentialRecoveryTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void testSequentialRecovery() {
		fail("Not yet implemented");
	}

	@Test
	public final void testExecuteOrdered() {
		fail("Not yet implemented");
	}

	@Test
	public final void testGetState() {
		fail("Not yet implemented");
	}

	@Test
	public final void testSetState() throws NoSuchAlgorithmException {
		MessageDigest md = mock(MessageDigest.class);
		mockStatic(TOMUtil.class);
		when(TOMUtil.getHashEngine()).thenReturn(md);
		SequentialRecoveryTestImplementation recoveryTestImplementation = new SequentialRecoveryTestImplementation();
		
		DefaultApplicationState recvState = mock(DefaultApplicationState.class);
		when(recvState.getLastCID()).thenReturn(10);
		when(recvState.getLastCheckpointCID()).thenReturn(1);
		byte [] state = null;
		when(recvState.getState()).thenReturn(state);
		
		
		recoveryTestImplementation.setState(recvState);

	}

	@Test
	public final void testSetReplicaContext() {
		fail("Not yet implemented");
	}

	@Test
	public final void testGetStateManager() {
		fail("Not yet implemented");
	}

	@Test
	public final void testExecuteUnordered() {
		fail("Not yet implemented");
	}

	@Test
	public final void testOp() {
		fail("Not yet implemented");
	}

	@Test
	public final void testNoOp() {
		fail("Not yet implemented");
	}

	@Test
	public final void testInstallSnapshot() {
		fail("Not yet implemented");
	}

	@Test
	public final void testGetSnapshot() {
		fail("Not yet implemented");
	}

	@Test
	public final void testAppExecuteOrdered() {
		fail("Not yet implemented");
	}

	@Test
	public final void testAppExecuteUnordered() {
		fail("Not yet implemented");
	}

}

final class SequentialRecoveryTestImplementation extends SequentialRecovery {

	@Override
	public void installSnapshot(byte[] state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] getSnapshot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
