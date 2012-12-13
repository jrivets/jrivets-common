package org.jrivets.log;

public final class Formatter {

    private Formatter() {
        throw new AssertionError("Formatter is static class, cannot be instantiated.");
    }

    public static String concatArgs(Object... args) {
        if (args == null) {
            return "null";
        }

        if (args.length == 0) {
            return "";
        }

        if (args.length == 1) {
            return parseOneArg(args[0]);
        }

        return parseArgs(args);
    }

    private static String parseOneArg(Object arg) {
        if (arg == null) {
            return "null";
        } else if (arg instanceof Throwable) {
            return getThrowableStackTrace((Throwable) arg);
        }
        return arg.toString();
    }

    private static String parseArgs(Object[] args) {
        StringBuilder result = new StringBuilder(80);

        for (int idx = 0; idx < args.length; ++idx) {
            if (args[idx] != null && args[idx] instanceof Throwable) {
                appendThrowableStackTrace(result, (Throwable) args[idx]);
            } else {
                result.append(args[idx]);
            }
        }

        return result.toString();
    }

    public static String getThrowableStackTrace(Throwable t) {
        return appendThrowableStackTrace(new StringBuilder(), t).toString();
    }

    private static StringBuilder appendThrowableStackTrace(StringBuilder sb, Throwable t) {
        return appendThrowableStackTrace(sb, t, 3, 100);
    }

    private static StringBuilder appendThrowableStackTrace(StringBuilder sb, Throwable t, int maxCallDepth,
            int maxStackLines) {

        while (t != null && maxCallDepth-- > 0) {
            sb.append(t);
            appendStackTrace(sb, t.getStackTrace(), maxStackLines);
            sb.append("\n");
            t = t.getCause();
            if (t != null) {
                sb.append("Which caused by ");
            }
        }

        while (t != null && maxStackLines-- > 0) {
            sb.append(t.toString());
            sb.append("(stack trace is cut down)");
            sb.append("\n");
            t = t.getCause();
            if (t != null) {
                sb.append("Which caused by ");
            }
        }

        return sb;
    }

    private static StringBuilder appendStackTrace(StringBuilder sb, StackTraceElement[] trace, int maxStackLines) {
        if (trace != null) {
            int printCount = Math.min(trace.length, maxStackLines);
            for (int i = 0; i < printCount; i++) {
                sb.append("\n\tat ");
                sb.append(trace[i]);
            }
            if (printCount < trace.length) {
                sb.append("\nAnd ");
                sb.append(trace.length - maxStackLines);
                sb.append(" lines more...");
            }
        }
        return sb;
    }

}
