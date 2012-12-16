package org.jrivets.log;

/**
 * Lazy log message producer which implements {@link Logger} interface.
 * <p>
 * The implementation composes a log message from the provided list of objects
 * only in case of requested log level is enabled. This is abstract class which
 * doesn't care about the logging mechanism. Extended classes should implement
 * two methods: <tt>isEnabled()</tt> and <tt>log()</tt>.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public abstract class AbstractLogger implements Logger {

    enum LogLevel {
        FATAL, ERROR, WARN, INFO, DEBUG, TRACE
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

    /**
     * Returns whether the requested log level is enabled or not
     * 
     * @param logLevel - checked log level
     * @return true if the logLevel is enabled.
     */
    public abstract boolean isEnabled(LogLevel logLevel);

    /**
     * Puts a log message with specified level to the log.
     * 
     * @param logLevel - message log level
     * @param message - logged message
     */
    protected abstract void log(LogLevel logLevel, String message);

    private LogInvoker getNewLogInvokerInstance(String formatString) {
        if (formatString == null || formatString.trim().length() == 0) {
            return new SimpleLogInvoker();
        }
        return new FormattedLogInvoker(formatString);
    }

    private void logWithLevel(LogLevel logLevel, Object... args) {
        if (isEnabled(logLevel)) {
            logInvoker.logMessage(logLevel, Formatter.concatArgs(args));
        }
    }
}
