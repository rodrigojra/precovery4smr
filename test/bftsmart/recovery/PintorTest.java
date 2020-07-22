package bftsmart.recovery;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;

import bftsmart.tom.MessageContext;

public class PintorTest {

	private int COMMANDS_PER_BATCH;
	private int BATCH_SIZE;
	private int LAST_CID;
	private int CHECKPOINT_CID;
	private int INCREMENT_VALUE;

	@Before
	public void setUp() throws Exception {
		COMMANDS_PER_BATCH = 1000;
		BATCH_SIZE = 10;
		CHECKPOINT_CID = 0;
		LAST_CID = BATCH_SIZE;
		INCREMENT_VALUE = 1;
	}

	@Test
	public final void testScheduleWithoutDependency() {
		List<Command> commandList = new ArrayList<Command>(1);

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
			commandList.add(command);
		}
		int nThreads = 4;
		MetricRegistry metrics;
		ForkJoinPool pool = new ForkJoinPool(nThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true,
				nThreads, nThreads, 0, null, 60, TimeUnit.SECONDS);

		PooledScheduler pooledScheduler = new PooledScheduler(nThreads, pool);
		pooledScheduler.setExecutor(a -> System.out.println(Thread.currentThread().getName() + " - " + a));
		// pooledScheduler.setExecutor(a -> System.out.println(a));

		Stopwatch stopwatch = Stopwatch.createStarted();

//		int count = 0;
		for (Command command : commandList) {
//			if (count == 6)
//				pooledScheduler.setConflict(cdTrue);
			pooledScheduler.schedule(command);
//			count++;
		}
		pool.shutdown();

		try {
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		stopwatch.stop();
		System.out.println("Recovery time elapsed: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
		System.out.println("Recovery time elapsed: " + stopwatch.elapsed(TimeUnit.SECONDS));
	}

}
