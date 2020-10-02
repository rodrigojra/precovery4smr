package bftsmart.parallel.recovery.demo.map;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;

import bftsmart.parallel.late.ConflictDefinition;

/**
 * @author Rodrigo Antunes
 *
 */
public class KeyValueStoreCmd implements ConflictDefinition<KeyValueStoreCmd> {

	private final ByteBuffer data;

	@Override
	public boolean isDependent(KeyValueStoreCmd r2) {
		return conflictWith(r2);
	}

    KeyValueStoreType getType() {
        data.position(0);
        return KeyValueStoreType.values()[data.get()];
    }

    Integer getKey() {
        data.position(1);
        return data.getInt();
    }
	
    public KeyValueStoreCmd(KeyValueStoreType type, int key) {
        // [1|type][4|key]
        data = ByteBuffer.allocate(5);
        data.put((byte) type.ordinal());
        data.putInt(key);
        data.flip();
    }

    private KeyValueStoreCmd(ByteBuffer data) {
        this.data = data;
    }    
    
    public static KeyValueStoreCmd wrap(byte[] data) {
        return new KeyValueStoreCmd(ByteBuffer.wrap(data));
    }	

	public static KeyValueStoreCmd random(Random random, int maxKey, float sparseness, float conflict) {
		KeyValueStoreType type;
		if (random.nextFloat() >= conflict) {
			type = KeyValueStoreType.GET;
		} else {
			type = KeyValueStoreType.PUT;
		}

		int key, mid = maxKey / 2;
		do {
			key = (int) Math.round(random.nextGaussian() * (mid * sparseness) + mid);
		} while (key < 0 || key >= maxKey);

		return new KeyValueStoreCmd(type, key);
	}


	boolean conflictWith(KeyValueStoreCmd other) {
		return (getType().isWrite || other.getType().isWrite) && getKey().equals(other.getKey());
	}

	public Integer execute(Map<Integer, Integer> state) {
		return getType().execute(state, this);
	}
	
    public byte[] encode() {
        return data.array();
    }

}
