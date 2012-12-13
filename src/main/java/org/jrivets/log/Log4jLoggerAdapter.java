package org.jrivets.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

final class Log4jLoggerAdapter extends AbstractLogger {

    private final Logger log4jLogger;

    Log4jLoggerAdapter(Logger log4jLogger, String formatString, Object marker) {
        super(formatString, marker);
        this.log4jLogger = log4jLogger;
    }
    
    @Override
    public boolean isEnabled(LogLevel logLevel) {
        switch(logLevel) {
        case FATAL: return true;
        case ERROR: return log4jLogger.isEnabledFor(Level.ERROR);
        case WARN: return log4jLogger.isEnabledFor(Level.WARN);
        case INFO: return log4jLogger.isInfoEnabled();
        case DEBUG: return log4jLogger.isDebugEnabled();
        case TRACE: return log4jLogger.isTraceEnabled();
        }
        return true;
    }

    @Override
    public void log(LogLevel logLevel, String message) {
        switch(logLevel) {
        case FATAL: log4jLogger.fatal(message); break;
        case ERROR: log4jLogger.error(message); break;
        case WARN: log4jLogger.warn(message); break;
        case INFO: log4jLogger.info(message); break;
        case DEBUG: log4jLogger.debug(message); break;
        case TRACE: log4jLogger.trace(message); break;
        }
    }
}
