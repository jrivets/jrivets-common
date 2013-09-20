package org.jrivets.log;

import java.util.HashSet;
import java.util.Set;

import org.jrivets.log.AbstractLogger.LogLevel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AbstractLoggerTest {

    private TestLogger logger;
    
    private static class TestLogger extends AbstractLogger {
        
        private String logMessage;
        
        private final Set<LogLevel> enabledLevels = new HashSet<AbstractLogger.LogLevel>();

        TestLogger(String formatString, Object marker) {
            super(formatString, marker);
        }

        @Override
        public boolean isEnabled(LogLevel logLevel) {
            return enabledLevels.contains(logLevel);
        }

        @Override
        protected void log(LogLevel logLevel, String message) {
            logMessage = logLevel.name() + " " + message;
        }
        
        void setEnabled(LogLevel logLevel) {
            enabledLevels.add(logLevel);
        }
        
        String getLogMessage() {
            return logMessage;
        }
    }
    
    @BeforeMethod
    public void initSimple() {
        this.logger = new TestLogger(null, null);
    }
    
    @Test
    public void infoEnabled() {
        logger.setEnabled(LogLevel.INFO);
        logger.info("test");
        assertEquals("INFO test", logger.getLogMessage());
    }
    
    @Test
    public void infoDisabled() {
        logger.info("test");
        assertEquals(null, logger.getLogMessage());
    }
    
    @Test
    public void warnEnabled() {
        logger.setEnabled(LogLevel.WARN);
        logger.warn("test");
        assertEquals("WARN test", logger.getLogMessage());
    }
    
    @Test
    public void warnDisabled() {
        logger.warn("test");
        assertEquals(null, logger.getLogMessage());
    }
    
    @Test
    public void debugEnabled() {
        logger.setEnabled(LogLevel.DEBUG);
        logger.debug("test");
        assertEquals("DEBUG test", logger.getLogMessage());
    }
    
    @Test
    public void debugDisabled() {
        logger.debug("test");
        assertEquals(null, logger.getLogMessage());
    }
    
    @Test
    public void traceEnabled() {
        logger.setEnabled(LogLevel.TRACE);
        logger.trace("test");
        assertEquals("TRACE test", logger.getLogMessage());
    }
    
    @Test
    public void traceDisabled() {
        logger.trace("test");
        assertEquals(null, logger.getLogMessage());
    }
    
    @Test
    public void errorEnabled() {
        logger.setEnabled(LogLevel.ERROR);
        logger.error("test");
        assertEquals("ERROR test", logger.getLogMessage());
    }
    
    @Test
    public void errorDisabled() {
        logger.error("test");
        assertEquals(null, logger.getLogMessage());
    }
    
    @Test
    public void fatalEnabled() {
        logger.setEnabled(LogLevel.FATAL);
        logger.fatal("test");
        assertEquals("FATAL test", logger.getLogMessage());
    }
    
    @Test
    public void fatalDisabled() {
        logger.fatal("test");
        assertEquals(null, logger.getLogMessage());
    }
    
    @Test
    public void testFormatter() {
        logger = new TestLogger("%1$s %2$s", "test");
        logger.setEnabled(LogLevel.INFO);
        logger.info("message");
        assertEquals("INFO test message", logger.getLogMessage());
    }
}
