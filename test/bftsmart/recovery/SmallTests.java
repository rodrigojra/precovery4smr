package bftsmart.recovery;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import bftsmart.tom.MessageContext;

public class SmallTests {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void testConsumer() {
		
		Map<Command, MessageContext> map = new HashMap<Command, MessageContext>();
		map.put(new Command(1111, new byte[] {0,1,2,3,4}, null), null);
		Consumer<Map<Command, MessageContext>> consumer = this::appOrdered;
		consumer.accept(map);
	}
	
	public byte[] appOrdered(Map<Command, MessageContext> map) {
		
		for (Map.Entry<Command, MessageContext> entry : map.entrySet()) {
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

}
