package org.jrivets.log;

/**
 * The logger factory produces Log4j wrappers.
 * 
 * @author Dmitry Spasibenko 
 *
 */
public class LoggerFactory {
    private LoggerFactory() {
        throw new AssertionError("LoggerFactory is class with static methods only");
    }

    /**
     * Returns Logger instance which wraps Log4j Logger with a purpose to add some formatting there.
     * The parameters will be fed to <code>String.format(formatString, marker, message)</code> whose
     * result will be printed to the log then.
     *    
     * @param clazz
     * @param formatString
     * @param marker
     * @return
     */
    public static <T> Logger getLogger(Class<T> clazz, String formatString, Object marker) {
        return new Log4jLoggerAdapter(org.apache.log4j.Logger.getLogger(clazz), formatString, marker);
    }
    
    public static <T> Logger getLogger(Class<T> clazz) {
        return new Log4jLoggerAdapter(org.apache.log4j.Logger.getLogger(clazz), null, null);
    }
    
    public static Logger getLogger(String name, String formatString, Object marker) {
        return new Log4jLoggerAdapter(org.apache.log4j.Logger.getLogger(name), formatString, marker);
    }
}
