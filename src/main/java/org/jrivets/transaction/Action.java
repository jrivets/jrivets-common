package org.jrivets.transaction;

/**
 * The interface defines an action which can be executed in
 * {@link SimpleTransaction} context.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public interface Action {

    /**
     * Executes the action itself. In case of fail should throw an exception
     * 
     * @throws Throwable
     *             describes the fail cause
     */
    void doAction() throws Throwable;

    /**
     * Rolls back the action executed by <tt>doAction()</tt>. It will be invoked
     * ONLY if the action method <tt>doAction()</tt> for the object has been
     * executed successfully (did not throw an exception). The cause of calling
     * this method can be one of the following: an action after this one is
     * failed or transaction was cancelled explicitly (
     * <tt>SimpleTransaction.cancel()</tt> method is called).
     */
    void rollbackAction();

}
