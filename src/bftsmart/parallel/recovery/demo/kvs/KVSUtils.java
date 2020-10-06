/**
 * 
 */
package bftsmart.parallel.recovery.demo.kvs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Random;

/**
 * @author Rodrigo Antunes
 *
 */
public class KVSUtils {

	public static byte[] generateRandomOperation(Random random, int maxKey, float sparseness, float conflict) {
		KVSRequestType requestType;
		byte[] byteArrayToReturn = null;
		int randomOp = random.nextInt(1);
		if (random.nextFloat() >= conflict) {
			if (randomOp == 0) {
				requestType = KVSRequestType.GET;	
			} else {
				requestType = KVSRequestType.SIZE;
			}
		} else {
			if (randomOp == 0) {
				requestType = KVSRequestType.PUT;	
			} else {
				requestType = KVSRequestType.REMOVE;
			}
		}

		int key, mid = maxKey / 2;
		do {
			key = (int) Math.round(random.nextGaussian() * (mid * sparseness) + mid);
		} while (key < 0 || key >= maxKey);
		
		try {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(byteOut);
			
			objOut.writeObject(requestType);
			objOut.writeObject(key);
			objOut.writeObject(random.nextInt());
			
			objOut.flush();
			byteOut.flush();
			
			byteArrayToReturn = byteOut.toByteArray();

		} catch (IOException e) {
			System.out.println("Exception putting value into map: " + e.getMessage());
		}			

		return byteArrayToReturn;
	}
}
