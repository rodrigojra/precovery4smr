/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.parallel.late;

/**
 *
 * @author Rodrigo Antunes
 */
public interface ConflictDefinition<T> {

     public boolean isDependent(T r2);
    
}
