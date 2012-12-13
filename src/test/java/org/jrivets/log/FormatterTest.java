package org.jrivets.log;

import static org.junit.Assert.*;

import org.jrivets.log.Formatter;
import org.junit.Test;

public class FormatterTest {

    @Test
    public void concatEmptyArgs() {
        String result = Formatter.concatArgs();
        assertTrue("Expected empty string, but receives \"" + result + "\"", result.isEmpty());
    }

    @Test
    public void concatNullArgs() {
        String result = Formatter.concatArgs((Object[]) null);
        assertTrue("Expected \"null\" string, but receives \"" + result + "\"", result.equals("null"));
    }

    @Test
    public void concatNullWithArgs() {
        String result = Formatter.concatArgs(null, "a");
        assertTrue("Expected \"nulla\" string, but receives \"" + result + "\"", result.equals("nulla"));
    }

    @Test
    public void concatArgsWithOneNullArg() {
        String result = Formatter.concatArgs(new Object[] { null });
        assertTrue("Expected \"null\" string, but receives \"" + result + "\"", result.equals("null"));
    }

    @Test
    public void concatArgsWithThrowableArg() {
        String result = Formatter.concatArgs(new Throwable());
        assertTrue("Expected stack trace, but receives \"" + result + "\"",
                result.contains(".concatArgsWithThrowableArg("));
    }

    @Test
    public void getThrowableStackTraceForNull() {
        String result = Formatter.getThrowableStackTrace(null);
        assertTrue("Expected empty string, but receives \"" + result + "\"", result.isEmpty());
    }

    @Test
    public void getThrowableStackTrace() {
        String result = Formatter.getThrowableStackTrace(new Throwable());
        assertTrue("Expected stack trace, but receives \"" + result + "\"", result.contains(".getThrowableStackTrace("));
    }
}
