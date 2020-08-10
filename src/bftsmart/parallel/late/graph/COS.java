package bftsmart.parallel.late.graph;

import java.util.concurrent.Semaphore;

import bftsmart.parallel.MessageContextPair;
import bftsmart.parallel.late.ConflictDefinition;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author eduardo
 * @author Rodrigo Antunes
 */
public abstract class COS {

	private ConflictDefinition conflictDefinition = null;

    private Semaphore space = null;                // counting semaphore for size of graph
    private Semaphore ready = new Semaphore(0);  // tells if there is ready to execute
    
    //protected CBASEScheduler scheduler;
    
    
    public COS(int limit/*, CBASEScheduler scheduler*/) {
        this.space = new Semaphore(limit);
        //this.scheduler = scheduler;
    } 
     
     
    
    protected boolean isDependent(Object thisRequest, Object otherRequest){
    	//FIXME: ver como fica melhor de fazer, só trocar por Object ou por Generics
        //return this.conflictDefinition.isDependent(thisRequest, otherRequest);
    	return true;
    }
    
    public void insert(Object request) throws InterruptedException {
        space.acquire();
        int readyNum = COSInsert(request);
        this.ready.release(readyNum);
    }
    
    public void remove(Object requestNode) throws InterruptedException {
        int readyNum = COSRemove(requestNode);
        this.space.release();
        this.ready.release(readyNum);
    }

    public Object get() throws InterruptedException {
        this.ready.acquire();
        return COSGet();
    }
    
    protected abstract int COSInsert(Object request) throws InterruptedException;

    protected abstract Object COSGet() throws InterruptedException;

    protected abstract int COSRemove(Object request) throws InterruptedException;

	public void setConflictDefinition(ConflictDefinition conflictDefinition) {
		this.conflictDefinition = conflictDefinition;
	}

}