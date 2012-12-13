package org.jrivets.log;

public abstract class AbstractLogger implements Logger {

    enum LogLevel {
        FATAL, 
        ERROR,
        WARN, 
        INFO,
        DEBUG,
        TRACE
    }
    
    private Object marker;
    
    private final LogInvoker logInvoker;
    
    private interface LogInvoker {
        void logMessage(LogLevel logLevel, String message);
    }

    private class SimpleLogInvoker implements LogInvoker {
        @Override
        public void logMessage(LogLevel logLevel, String message) {
            AbstractLogger.this.log(logLevel, message);
        }
    }
    
    private class FormattedLogInvoker implements LogInvoker {
        private final String formatString;
        
        private FormattedLogInvoker(String formatString) {
            this.formatString = formatString;
        }
        
        @Override
        public void logMessage(LogLevel logLevel, String message) {
            AbstractLogger.this.log(logLevel, String.format(formatString, marker, message));
        }
    }
    
    AbstractLogger(String formatString, Object marker) {
        this.logInvoker = getNewLogInvokerInstance(formatString); 
        this.marker = marker;
    }
    
    @Override
    public void fatal(Object... args) {
        logWithLevel(LogLevel.FATAL, args);
    }

    @Override
    public void error(Object... args) {
        logWithLevel(LogLevel.ERROR, args);
    }

    @Override
    public void warn(Object... args) {
        logWithLevel(LogLevel.WARN, args);
    }

    @Override
    public void info(Object... args) {
        logWithLevel(LogLevel.INFO, args);
    }

    @Override
    public void debug(Object... args) {
        logWithLevel(LogLevel.DEBUG, args);
    }

    @Override
    public void trace(Object... args) {
        logWithLevel(LogLevel.TRACE, args);
    }

    public void setMarker(Object marker) {
        this.marker = marker;
    }
    
    public abstract boolean isEnabled(LogLevel logLevel);
    public abstract void log(LogLevel logLevel, String message);

    private LogInvoker getNewLogInvokerInstance(String formatString) {
        if (formatString == null || formatString.trim().length() == 0) {
            return new SimpleLogInvoker();
        }
        return new FormattedLogInvoker(formatString);
    }
    
    private void logWithLevel(LogLevel logLevel, Object ... args) {
        if (isEnabled(logLevel)) {
            logInvoker.logMessage(logLevel, Formatter.concatArgs(args));
        }
    }
}
