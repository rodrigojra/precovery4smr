/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.parallelism.late;

/**
 *
 * @author eduardo
 */
public abstract class ConflictDefinition<T> {

    public ConflictDefinition() {
    }
 
     public abstract boolean isDependent(T r1, T r2);
    
    
    
}
