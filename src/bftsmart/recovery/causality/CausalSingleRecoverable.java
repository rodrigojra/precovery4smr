/**
 * 
 */
package bftsmart.recovery.causality;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import bftsmart.tom.server.defaultservices.DiskStateLog;
import bftsmart.tom.server.defaultservices.StateLog;

/**
 * @author rodrigo
 *
 */
public abstract class CausalSingleRecoverable extends DefaultSingleRecoverable {
	
    private class CausalEntry {
        private final byte[] command;
        private final byte[] causality;

        CausalEntry(byte[] command, byte[] causality) {
            this.command = command;
            this.causality = causality;
        }
    }	

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void installSnapshot(byte[] state) {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] getSnapshot() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected void initLog() {
    	if(log == null) {
    		checkpointPeriod = config.getCheckpointPeriod();
            byte[] state = getSnapshot();
            if(config.isToLog() && config.logToDisk()) {
            	int replicaId = config.getProcessId();
            	boolean isToLog = config.isToLog();
            	boolean syncLog = config.isToWriteSyncLog();
            	boolean syncCkp = config.isToWriteSyncCkp();
            	log = new CausalDiskStateLog(replicaId, state, computeHash(state), isToLog, syncLog, syncCkp);
            } else
            	log = new StateLog(controller.getStaticConf().getProcessId(), checkpointPeriod, state, computeHash(state));
    	}
    }

	@Override
    public byte[] executeOrdered(byte[] command, MessageContext msgCtx, boolean noop) {
        
        int cid = msgCtx.getConsensusId();
        
        byte[] reply = null;
            
        if (!noop) {
            stateLock.lock();
            reply = appExecuteOrdered(command, msgCtx);
            stateLock.unlock();
        }
        
        commands.add(command);
        msgContexts.add(msgCtx);
        
        if(msgCtx.isLastInBatch()) {
	        if ((cid > 0) && ((cid % checkpointPeriod) == 0)) {
	            logger.debug("Performing checkpoint for consensus " + cid);
	            stateLock.lock();
	            byte[] snapshot = getSnapshot();
	            stateLock.unlock();
	            saveState(snapshot, cid);
	        } else {
	            saveCommands(commands.toArray(new byte[0][]), msgContexts.toArray(new MessageContext[0]));
	        }
			getStateManager().setLastCID(cid);
	        commands = new ArrayList<>();
                msgContexts = new ArrayList<>();
        }
        return reply;
    }	
	
}
