package org.jrivets.log;

public interface Logger {

    void fatal(Object ... args);
    
    void error(Object ... args);
    
    void warn(Object ... args);
    
    void info(Object ... args);
    
    void debug(Object ... args);
    
    void trace(Object ... args);
    
    void setMarker(Object marker);
}
