package org.jrivets.log;

/**
 * The interface defines methods for producing log messages with different
 * severity levels
 * 
 * @author Dmitry Spasibenko
 * 
 */
public interface Logger {

    void fatal(Object... args);

    void error(Object... args);

    void warn(Object... args);

    void info(Object o1);

    void info(Object o1, Object o2);

    void info(Object o1, Object o2, Object o3);

    void info(Object o1, Object o2, Object o3, Object o4);

    void info(Object o1, Object o2, Object o3, Object o4, Object o5);

    void info(Object... args);

    void debug(Object o1);
    
    void debug(Object o1, Object o2);

    void debug(Object o1, Object o2, Object o3);

    void debug(Object o1, Object o2, Object o3, Object o4);

    void debug(Object o1, Object o2, Object o3, Object o4, Object o5);

    void debug(Object... args);

    void trace(Object o1);
    
    void trace(Object o1, Object o2);

    void trace(Object o1, Object o2, Object o3);

    void trace(Object o1, Object o2, Object o3, Object o4);

    void trace(Object o1, Object o2, Object o3, Object o4, Object o5);

    void trace(Object... args);

    void setMarker(Object marker);
}
