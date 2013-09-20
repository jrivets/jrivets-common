package org.jrivets.log;

import static org.testng.Assert.*;

import org.jrivets.log.Formatter;
import org.testng.annotations.Test;

public class FormatterTest {

    @Test
    public void concatEmptyArgs() {
        String result = Formatter.concatArgs();
        assertTrue(result.isEmpty(), "Expected empty string, but receives \"" + result + "\"");
    }

    @Test
    public void concatNullArgs() {
        String result = Formatter.concatArgs((Object[]) null);
        assertTrue(result.equals("null"), "Expected \"null\" string, but receives \"" + result + "\"");
    }

    @Test
    public void concatNullWithArgs() {
        String result = Formatter.concatArgs(null, "a");
        assertTrue(result.equals("nulla"), "Expected \"nulla\" string, but receives \"" + result + "\"");
    }

    @Test
    public void concatArgsWithOneNullArg() {
        String result = Formatter.concatArgs(new Object[] { null });
        assertTrue(result.equals("null"), "Expected \"null\" string, but receives \"" + result + "\"");
    }

    @Test
    public void concatArgsWithThrowableArg() {
        String result = Formatter.concatArgs(new Throwable());
        assertTrue(result.contains(".concatArgsWithThrowableArg("), "Expected stack trace, but receives \"" + result + "\"");
    }

    @Test
    public void getThrowableStackTraceForNull() {
        String result = Formatter.getThrowableStackTrace(null);
        assertTrue(result.isEmpty(), "Expected empty string, but receives \"" + result + "\"");
    }

    @Test
    public void getThrowableStackTrace() {
        String result = Formatter.getThrowableStackTrace(new Throwable());
        assertTrue(result.contains(".getThrowableStackTrace("), "Expected stack trace, but receives \"" + result + "\"");
    }
}
