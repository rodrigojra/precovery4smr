/**
 * 
 */
package bftsmart.recovery.parallel.demo.kvs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Rodrigo Antunes
 *
 */
public class KVSUtils {

	public static byte[] generateRandomOperation(Random random, int maxKey, float sparseness, float conflict) {
		KVSRequestType requestType;
		byte[] byteArrayToReturn = null;
		requestType = generateOperationType(random);

		int key, mid = maxKey / 2;
		do {
			key = (int) Math.round(random.nextGaussian() * (mid * sparseness) + mid);
		} while (key < 0 || key >= maxKey);

		try {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(byteOut);

			objOut.writeObject(requestType);
			objOut.writeObject(key);

			if (requestType == KVSRequestType.PUT) {
				objOut.writeObject(random.nextInt());
			}

			objOut.flush();
			byteOut.flush();

			byteArrayToReturn = byteOut.toByteArray();
		} catch (IOException e) {
			System.out.println("Exception putting value into map: " + e.getMessage());
		}

		return byteArrayToReturn;
	}

	/**
	 * @param random
	 * @return
	 */
	private static KVSRequestType generateOperationType(Random random) {
		KVSRequestType requestType;
		int writableOP = random.nextInt(2);
		int randomOp = random.nextInt(2);
		if (writableOP == 0) {
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
		System.out.println(">> requestType " + requestType);
		return requestType;
	}

	/**
	 * @param reply
	 */
	public static void printBytes(byte[] reply) {
		System.out.print("\t[ ");
		for (byte b : reply) {
			System.out.print(", " + b);
		}
		System.out.println(" ]");
	}
	
	public static void printOperationData(KVSRequestType type, int key, int value) {
		String operation = "";
		switch (type) {
		case PUT:
			operation = "PUT";
			break;
		case GET:
			operation = "GET";
			break;
		case REMOVE:
			operation = "REMOVE";
			break;
		case KEYSET:
			operation = "KEYSET";
			break;
		default:
			operation = "EMPTY";
			break;
		}

		if (value == -1) {
			System.out.println("received [" + operation + "] key [" + key + "]");
		} else {
			System.out.println("received [" + operation + "] key [" + key + "] value[" + value + "]");
		}
	}	

	public static void main(String[] args) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		//int maxKey = 1000;
		//float sparseness = 10.0f;
		//float conflict = 1.0f;
		// generateRandomOperation(random, maxKey, sparseness, conflict);

		for (int i = 0; i < 10; i++) {
			generateOperationType(random);
		}

	}
}
