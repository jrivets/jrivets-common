package org.jrivets.transaction;

/**
 * The exception is thrown by {@link SimpleTransaction} if an action executed in
 * context of the <tt>SimpleTransaction</tt> is failed. The
 * <tt>FailedTransactionException.getCause()</tt> returns the exception thrown
 * by the failed action.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public class FailedTransactionException extends Exception {

    private static final long serialVersionUID = -7167632229709480881L;

    public FailedTransactionException(String message, Throwable causedBy) {
        super(message, causedBy);
    }
}
