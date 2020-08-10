package bftsmart.parallel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Stopwatch;

import bftsmart.tom.MessageContext;

public class SmallTests {
/*
	@Before
	public void setUp() throws Exception {
	}


	@Test
	public final void testAvailableProcessors() {
		int coreCount = Runtime.getRuntime().availableProcessors();
		System.out.println("testAvailableProcessors coreCount: " + coreCount);
	}	
	
	@Test
	public final void testConsumer() {
		
		Map<CounnterCommand, MessageContext> map = new HashMap<CounnterCommand, MessageContext>();
		map.put(new CounnterCommand(1111, new byte[] {0,1,2,3,4}, null), null);
		Consumer<Map<CounnterCommand, MessageContext>> consumer = this::appOrdered;
		consumer.accept(map);
	}
	
	public byte[] appOrdered(Map<CounnterCommand, MessageContext> map) {
		
		for (Map.Entry<CounnterCommand, MessageContext> entry : map.entrySet()) {
		    System.out.println(entry.getKey() + "/" + entry.getValue());
		}
		
		return new byte [] {0,1,2};
	}
	
	@Test
	public final void testDateFormat() {
		Date date = new Date() ;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SS") ;
		System.out.println( "here is " + dateFormat.format(date));
		
	}
	

	@Test
	public final void testSyntacticDelay2() {
		int minCostNS = 20000000;
		SyntacticDelay delay2 = new SyntacticDelay(minCostNS);
		AtomicInteger counter = new AtomicInteger(0);
		AtomicInteger iterations = new AtomicInteger(0);
		Stopwatch stopwatch = Stopwatch.createStarted();
		delay2.ensureMinCost(() -> {
			iterations.incrementAndGet();
			try {
				 byte [] data = ByteBuffer.allocate(4).putInt(1).array();
				int increment = new DataInputStream(new ByteArrayInputStream(data)).readInt();
				// counter += increment;
				counter.addAndGet(increment);
				// System.out.println(Thread.currentThread().getName() + " - command " +
				// command.toString() + " - Counter was incremented. Current value = " +
				// counter);
				ByteArrayOutputStream out = new ByteArrayOutputStream(4);
				new DataOutputStream(out).writeInt(counter.get());
				return out.toByteArray();
			} catch (IOException ex) {
				System.err.println("Invalid request received!");
				return new byte[0];
			} finally {

			}
		});
		
		stopwatch.stop();
		System.out.println();
		System.out.println("time elapsed MILLISECONDS: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
		System.out.println("time elapsed SECONDS: " + stopwatch.elapsed(TimeUnit.SECONDS));		
	}
	
	
	*/

}
