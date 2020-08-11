/**
 * 
 */
package bftsmart.parallel.recovery.demo.counter;

import bftsmart.parallel.late.ConflictDefinition;
import bftsmart.parallel.recovery.Command;
import bftsmart.tom.MessageContext;

/**
 * This class represents only one command/operation.
 * @author Rodrigo Antunes
 *
 */
public class CounterCommand extends Command implements ConflictDefinition<CounterCommand>  {
	
	private Type type;
	public enum Type {PARALLEL, CONFLICT};
	

	public CounterCommand(int id, byte[] data, MessageContext messageContext) {
		this(id, data, messageContext, Type.PARALLEL);
	}
	
	public CounterCommand(int id, byte[] data, MessageContext messageContext, Type type) {
		super(id, data, messageContext);
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CounterCommand [id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public boolean isDependent(CounterCommand cd2) {
		if (this.getType() == Type.CONFLICT && cd2.getType() == Type.CONFLICT) {
			return true;
		}

		return false;
	}
	
	
}

