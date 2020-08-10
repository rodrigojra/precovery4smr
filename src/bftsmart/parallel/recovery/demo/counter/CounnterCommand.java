/**
 * 
 */
package bftsmart.parallel.recovery.demo.counter;

import java.util.Arrays;

import com.google.common.base.Objects;

import bftsmart.parallel.late.ConflictDefinition;
import bftsmart.tom.MessageContext;

/**
 * This class represents only one command/operation.
 * @author Rodrigo Antunes
 *
 */
public class CounnterCommand implements ConflictDefinition<CounnterCommand>  {
	
	private int id;
	private byte [] data;
	private MessageContext messageContext;
	private Type type; 
	
	public enum Type {PARALLEL, CONFLICT};
	
	public CounnterCommand() {
	}

	public CounnterCommand(int id, byte[] data, MessageContext messageContext) {
		this(id, data, messageContext, Type.PARALLEL);
	}
	
	public CounnterCommand(int id, byte[] data, MessageContext messageContext, Type type) {
		this.id = id;
		this.data = data;
		this.messageContext = messageContext;
		this.type = type;
	}
	
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public MessageContext getMessageContext() {
		return messageContext;
	}
	public void setMessageContext(MessageContext messageContext) {
		this.messageContext = messageContext;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, data, messageContext);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CounnterCommand other = (CounnterCommand) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		if (id != other.id)
			return false;
		if (messageContext == null) {
			if (other.messageContext != null)
				return false;
		} else if (!messageContext.equals(other.messageContext))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CounnterCommand [id=");
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
	public boolean isDependent(CounnterCommand cd2) {
		
		if (this.getType() == Type.CONFLICT && cd2.getType() == Type.CONFLICT) {
			return true;
		}

		return false;
	}
	
	
}

