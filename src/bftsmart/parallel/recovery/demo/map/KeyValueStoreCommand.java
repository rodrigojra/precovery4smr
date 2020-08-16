/**
 * 
 */
package bftsmart.parallel.recovery.demo.map;

import bftsmart.parallel.late.ConflictDefinition;
import bftsmart.parallel.recovery.Command;

/**
 * @author Rodrigo Antunes
 *
 */
public class KeyValueStoreCommand extends Command  implements ConflictDefinition<KeyValueStoreCommand>   {
	
	private KeyValueStoreType type;
	private Integer key;

	@Override
	public boolean isDependent(KeyValueStoreCommand r2) {
		// TODO Auto-generated method stub
		return false;
	}

	public Integer getKey() {
		return key;
	}

	public void setKey(Integer key) {
		this.key = key;
	}

	public KeyValueStoreType getType() {
		return type;
	}

	public void setType(KeyValueStoreType type) {
		this.type = type;
	}

}
