package org.jrivets.util;

public abstract class StaticSingleton {

    protected StaticSingleton() {
        throw new AssertionError("The class " + this.getClass() 
                + " cannot be instantiated. Only static methods are supposed to be used.");
    }
    
}
