package bftsmart.parallel.recovery.demo.map;

import java.util.Map;

/**
 * Types of operation for BFT Map implementation
 * 
 */

public enum KeyValueStoreType {

	PUT(true) {
		@Override
		Integer execute(Map<Integer, Integer> state, KeyValueStoreCmd cmd) {
			Integer key = cmd.getKey();
			Integer value = key;
			state.put(key, value);
			return value;
		}
	},

	GET(false) {
		@Override
		Integer execute(Map<Integer, Integer> state, KeyValueStoreCmd cmd) {
			return state.get(cmd.getKey());
		}
	},

	REMOVE(true) {
		@Override
		Integer execute(Map<Integer, Integer> state, KeyValueStoreCmd cmd) {
			return state.get(cmd.getKey());
		}
	};

	boolean isWrite;

	KeyValueStoreType(boolean isWrite) {
		this.isWrite = isWrite;
	}

	abstract Integer execute(Map<Integer, Integer> state, KeyValueStoreCmd cmd);

}