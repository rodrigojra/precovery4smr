/**
 * 
 */
package bftsmart.parallel.recovery;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import bftsmart.parallel.recovery.demo.counter.CounterCommand;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.reconfiguration.util.TOMConfiguration;
import bftsmart.statemanagement.ApplicationState;
import bftsmart.statemanagement.StateManager;
import bftsmart.statemanagement.strategy.StandardStateManager;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ReplicaContext;
import bftsmart.tom.server.Recoverable;
import bftsmart.tom.server.SingleExecutable;
import bftsmart.tom.server.defaultservices.CommandsInfo;
import bftsmart.tom.server.defaultservices.DefaultApplicationState;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import bftsmart.tom.server.defaultservices.DiskStateLog;
import bftsmart.tom.server.defaultservices.StateLog;
import bftsmart.tom.util.TOMUtil;

/**
 * @author rodrigo
 * 
 * This class has the same code from {@link DefaultSingleRecoverable}
 * I created that to do some experiments without affect the original class
 *
 */
public abstract class ParallelRecovery implements Recoverable, SingleExecutable {
    
    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    
    protected ReplicaContext replicaContext;
    private TOMConfiguration config;
    private ServerViewController controller;
    private int checkpointPeriod;

    protected ReentrantLock logLock = new ReentrantLock();
    private ReentrantLock hashLock = new ReentrantLock();
    protected ReentrantLock stateLock = new ReentrantLock();
    
    private MessageDigest md;
        
    protected StateLog log;
    private List<byte[]> commands = new ArrayList<>();
    private List<MessageContext> msgContexts = new ArrayList<>();
    
    private StateManager stateManager;
    
    protected boolean isJunit = false;
    
    public void setIsJunit(boolean isJunit) {
    	this.isJunit = isJunit;
    }
    
    public ParallelRecovery() {

        try {
            md = TOMUtil.getHashEngine();
        } catch (NoSuchAlgorithmException ex) {
            logger.error("Failed to get message digest engine", ex);
        }
    }
    
    @Override
    public byte[] executeOrdered(byte[] command, MessageContext msgCtx) {
        
        return executeOrdered(command, msgCtx, false);
        
    }
    
    private byte[] executeOrdered(byte[] command, MessageContext msgCtx, boolean noop) {
        
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
    
    private final byte[] computeHash(byte[] data) {
        byte[] ret = null;
        hashLock.lock();
        ret = md.digest(data);
        hashLock.unlock();

        return ret;
    }
    
    private StateLog getLog() {
       	initLog();
    	return log;
    }
    
    private void saveState(byte[] snapshot, int lastCID) {
        StateLog thisLog = getLog();

        logLock.lock();

        logger.debug("Saving state of CID " + lastCID);

        thisLog.newCheckpoint(snapshot, computeHash(snapshot), lastCID);
        thisLog.setLastCID(-1);
        thisLog.setLastCheckpointCID(lastCID);

        logLock.unlock();
        /*System.out.println("fiz checkpoint");
        System.out.println("tamanho do snapshot: " + snapshot.length);
        System.out.println("tamanho do log: " + thisLog.getMessageBatches().length);*/
        logger.debug("Finished saving state of CID " + lastCID);
    }

    private void saveCommands(byte[][] commands, MessageContext[] msgCtx) {
        
        if (commands.length != msgCtx.length) {
            logger.debug("----SIZE OF COMMANDS AND MESSAGE CONTEXTS IS DIFFERENT----");
            logger.debug("----COMMANDS: " + commands.length + ", CONTEXTS: " + msgCtx.length + " ----");
        }
        logLock.lock();

        int cid = msgCtx[0].getConsensusId();
        int batchStart = 0;
        for (int i = 0; i <= msgCtx.length; i++) {
            if (i == msgCtx.length) { // the batch command contains only one command or it is the last position of the array
                byte[][] batch = Arrays.copyOfRange(commands, batchStart, i);
                MessageContext[] batchMsgCtx = Arrays.copyOfRange(msgCtx, batchStart, i);
                log.addMessageBatch(batch, batchMsgCtx, cid);
            } else {
                if (msgCtx[i].getConsensusId() > cid) { // saves commands when the CID changes or when it is the last batch
                    byte[][] batch = Arrays.copyOfRange(commands, batchStart, i);
                    MessageContext[] batchMsgCtx = Arrays.copyOfRange(msgCtx, batchStart, i);
                    log.addMessageBatch(batch, batchMsgCtx, cid);
                    cid = msgCtx[i].getConsensusId();
                    batchStart = i;
                }
            }
        }
        
        logLock.unlock();
    }

    @Override
    public ApplicationState getState(int cid, boolean sendState) {
        logLock.lock();
        ApplicationState ret = (cid > -1 ? getLog().getApplicationState(cid, sendState) : new DefaultApplicationState());

        // Only will send a state if I have a proof for the last logged decision/consensus
        //TODO: I should always make sure to have a log with proofs, since this is a result
        // of not storing anything after a checkpoint and before logging more requests        
        if (ret == null || (config.isBFT() && ret.getCertifiedDecision(this.controller) == null)) ret = new DefaultApplicationState();

        logger.info("Getting log until CID " + cid + ", null: " + (ret == null));
        logLock.unlock();
        return ret;
    }
    
    @Override
    public int setState(ApplicationState recvState) {
        int lastCID = -1;
        if (recvState instanceof DefaultApplicationState) {
            
            DefaultApplicationState state = (DefaultApplicationState) recvState;
            
            logger.info("Last CID in state: " + state.getLastCID());
            
            logLock.lock();
            if (!isJunit) {
                initLog();
                log.update(state);
            }
            logLock.unlock();
            
            int lastCheckpointCID = state.getLastCheckpointCID();
            
            lastCID = state.getLastCID();

            logger.debug("I'm going to update myself from CID "
                    + lastCheckpointCID + " to CID " + lastCID);

            stateLock.lock();
            installSnapshot(state.getState());
            
            Stopwatch stopwatch = Stopwatch.createStarted();
            /// inserir no gapho ... criar um objeto c- comandos e o contexto
            // 
            //
            for (int cid = lastCheckpointCID + 1; cid <= lastCID; cid++) {
                try {
                    logger.debug("Processing and verifying batched requests for CID " + cid);

                    CommandsInfo cmdInfo = state.getMessageBatch(cid); 
                    byte[][] cmds = cmdInfo.commands; // take a batch
                    MessageContext[] msgCtxs = cmdInfo.msgCtx;
                    
                    if (cmds == null || msgCtxs == null || msgCtxs[0].isNoOp()) {
                        continue;
                    }
                    
                    for(int i = 0; i < cmds.length; i++) {
                    	appExecuteOrdered(cmds[i], msgCtxs[i]);
                    }
                } catch (Exception e) {
                    logger.error("Failed to process and verify batched requests",e);
                    if (e instanceof ArrayIndexOutOfBoundsException) {
                        logger.info("Last checkpoint, last consensus ID (CID): " + state.getLastCheckpointCID());
                        logger.info("Last CID: " + state.getLastCID());
                        logger.info("number of messages expected to be in the batch: " + (state.getLastCID() - state.getLastCheckpointCID() + 1));
                        logger.info("number of messages in the batch: " + state.getMessageBatches().length);
                    }
                }
            }
            
            stopwatch.stop();
            //System.out.println("Time elapsed: "+ stopwatch.elapsed(TimeUnit.MILLISECONDS));
            logger.info("Recovery time elapsed: "+ stopwatch.elapsed(TimeUnit.MILLISECONDS));
            logger.info("Recovery time elapsed: "+ stopwatch.elapsed(TimeUnit.SECONDS));
            //System.out.println("Time elapsed: "+ stopwatch.elapsed(TimeUnit.SECONDS));
            
            stateLock.unlock();

        }

        return lastCID;
    }

    @Override
    public void setReplicaContext(ReplicaContext replicaContext) {
        this.replicaContext = replicaContext;
        this.config = replicaContext.getStaticConfiguration();
        this.controller = replicaContext.getSVController();

        if (log == null) {
            checkpointPeriod = config.getCheckpointPeriod();
            byte[] state = getSnapshot();
            if (config.isToLog() && config.logToDisk()) {
                int replicaId = config.getProcessId();
                boolean isToLog = config.isToLog();
                boolean syncLog = config.isToWriteSyncLog();
                boolean syncCkp = config.isToWriteSyncCkp();
                log = new DiskStateLog(replicaId, state, computeHash(state), isToLog, syncLog, syncCkp);

                ApplicationState storedState = ((DiskStateLog) log).loadDurableState();
                if (storedState.getLastCID() > 0) {
                    setState(storedState);
                    getStateManager().setLastCID(storedState.getLastCID());
                }
            } else {
                log = new StateLog(this.config.getProcessId(), checkpointPeriod, state, computeHash(state));
            }
        }
        getStateManager().askCurrentConsensusId();
    }

    @Override
    public StateManager getStateManager() {
    	if(stateManager == null)
    		stateManager = new StandardStateManager();
    	return stateManager;
    }
	
    protected void initLog() {
    	if(log == null) {
    		checkpointPeriod = config.getCheckpointPeriod();
            byte[] state = getSnapshot();
            if(config.isToLog() && config.logToDisk()) {
            	int replicaId = config.getProcessId();
            	boolean isToLog = config.isToLog();
            	boolean syncLog = config.isToWriteSyncLog();
            	boolean syncCkp = config.isToWriteSyncCkp();
            	log = new DiskStateLog(replicaId, state, computeHash(state), isToLog, syncLog, syncCkp);
            } else
            	log = new StateLog(controller.getStaticConf().getProcessId(), checkpointPeriod, state, computeHash(state));
    	}
    }
    
    public void setConfig(TOMConfiguration conf) {
    	this.config = conf;
    }
          
    @Override
    public byte[] executeUnordered(byte[] command, MessageContext msgCtx) {
        return appExecuteUnordered(command, msgCtx);
    }
    
    @Override
    public void Op(int CID, byte[] requests, MessageContext msgCtx) {
        //Requests are logged within 'executeOrdered(...)' instead of in this method.
    }

    @Override
    public void noOp(int CID, byte[][] operations, MessageContext[] msgCtx) {
         
        for (int i = 0; i < msgCtx.length; i++) {
            executeOrdered(operations[i], msgCtx[i], true);
        }
    }
    
    /**
     * Given a snapshot received from the state transfer protocol, install it
     * @param state The serialized snapshot
     */
    public abstract void installSnapshot(byte[] state);
    
    /**
     * Returns a serialized snapshot of the application state
     * @return A serialized snapshot of the application state
     */
    public abstract byte[] getSnapshot();
    
    /**
     * Execute a batch of ordered requests
     * 
     * @param command The ordered request
     * @param msgCtx The context associated to each request
     * 
     * @return the reply for the request issued by the client
     */
    public abstract byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx);
    
    public abstract byte[] newAppExecuteOrdered(CounterCommand counterCommand);
    
    /**
     * Execute an unordered request
     * 
     * @param command The unordered request
     * @param msgCtx The context associated to the request
     * 
     * @return the reply for the request issued by the client
     */
    public abstract byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx);
}
