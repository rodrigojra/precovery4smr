/**
 * 
 */
package bftsmart.parallel.recovery.demo.kvs;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import bftsmart.demo.microbenchmarks.ThroughputLatencyServer;
import bftsmart.tom.ServiceProxy;

/**
 * @author Rodrigo Antunes
 *
 */
public class KVSNewClient {
	private static Logger logger = Logger.getLogger(KVSNewClient.class.getName());
	
	public static void main(String[] args) {
		System.out.println("args.length " + args.length);
		
		for (int i = 0; i < args.length; i++) {
			System.out.println(i + " arg== "+ args[i]);
		}
		
        if (args.length < 1) {
            System.out.println("Usage: java ... KVSNewClient <process id> <conflict percentage> [<worload size>]");
            System.out.println("       if <conflict percentage> equals 0 the request will be read-only");
            System.out.println("       default <number of operations> equals 10000");
            System.exit(-1);
        }
		
		logger.info("process id " + args[0]);
		logger.info("conflict percentage " + args[1]);
		logger.info("worload size " + args[2]);
		
    	int processId = Integer.parseInt(args[0]);
    	ServiceProxy kvsProxy = new ServiceProxy(processId);
        
        try {
        	ThreadLocalRandom random = ThreadLocalRandom.current();
            int sparseness = Integer.parseInt(args[1]);
            float conflict = (sparseness > 0) ? 1.0f : 0.0f;
            int worloadSize = (args.length > 2) ? Integer.parseInt(args[2]) : 1000;
            

            for (int i = 0; i < worloadSize; i++) {
                System.out.print("Invocation " + i);
                byte[] inputCommand = KVSUtils.generateRandomOperation(random, worloadSize, sparseness, conflict);
                byte[] reply = kvsProxy.invokeOrdered(inputCommand); //magic happens here
                
                if(reply != null) {
                    int newValue = new DataInputStream(new ByteArrayInputStream(reply)).readInt();
                    System.out.println(", returned value: " + newValue);
                    
            		try {
            			ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                		ObjectInput objIn = new ObjectInputStream(byteIn);
                		//ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                		//ObjectOutput objOut = new ObjectOutputStream(byteOut);
                		KVSRequestType reqType = (KVSRequestType)objIn.readObject();
                		Integer key = (Integer)objIn.readObject();
                		Integer value = (Integer)objIn.readObject();
                		System.out.println(", returned value: operation[" + reqType + "] key["+key+"] value["+value+"]");
            		} catch (Exception e) {
            			logger.info(e.getLocalizedMessage());
					}
                } else {
                    System.out.println(", ERROR! Exiting.");
                    break;
                }
            }
        } catch(Throwable e){
        	logger.info(e.getLocalizedMessage());
        } finally {
        	kvsProxy.close();
		}
	}
}
