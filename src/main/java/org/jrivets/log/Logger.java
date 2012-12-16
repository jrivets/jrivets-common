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

    void info(Object... args);

    void debug(Object... args);

    void trace(Object... args);

    void setMarker(Object marker);
}
