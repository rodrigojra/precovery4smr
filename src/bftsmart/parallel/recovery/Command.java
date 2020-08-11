/**
 * 
 */
package bftsmart.parallel.recovery;

import java.util.Arrays;

import com.google.common.base.Objects;

import bftsmart.tom.MessageContext;

/**
 * @author Rodrigo Antunes
 *
 */
public class Command {
	protected int id;
	protected byte[] data;
	protected MessageContext messageContext;

	public Command() {
	}

	public Command(int id, byte[] data, MessageContext messageContext) {
		this.id = id;
		this.data = data;
		this.messageContext = messageContext;
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
		Command other = (Command) obj;
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
		builder.append("CounterCommand [id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}

}
