/**
 * 
 */
package bftsmart.recovery.causality;

import bftsmart.tom.server.defaultservices.DiskStateLog;

/**
 * @author rodrigo
 *
 */
public class CausalDiskStateLog extends DiskStateLog {

	public CausalDiskStateLog(int id, byte[] initialState, byte[] initialHash, boolean isToLog, boolean syncLog,
			boolean syncCkp) {
		super(id, initialState, initialHash, isToLog, syncLog, syncCkp);
		// TODO Auto-generated constructor stub
	}

}
