package org.jrivets.transaction;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

/**
 * SimpleTransaction helps in case of set of actions should be executed in a
 * sequence. It supports rolling back procedure if one of the actions in the
 * sequence fails, what allows to roll-back successfully executed actions.
 * <p>
 * The class supports the contract as follows: <code>Action.doAction()</code> is
 * executed in submit order. If the method throws an exception the whole
 * transaction rolled back immediately (all previously success actions are
 * rolled back by calling <code>Action.rallbackAction()</code> in reverse order
 * of their submissions. In case of the roll-back procedure throws an exception,
 * the exception is silently ignored.) As soon as all actions are rolled back
 * the {@link FailedTransactionException} exception is thrown.
 * <p>
 * This is single tread implementation. All methods of the SimpleTransaction
 * object should be called from the thread which creates the object. Violation
 * of the rule will cause of {@link AssertionError}.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public final class SimpleTransaction {

    private final Logger logger;

    private final String name;

    private final long constructThreadId;

    private final List<Action> actions = new ArrayList<Action>();

    private volatile State state = State.INIT;

    private enum State {
        INIT, RUNNING, FAILED, COMMITED, CANCELLED;
        private boolean isFinal() {
            return this.equals(FAILED) || this.equals(COMMITED) || this.equals(CANCELLED);
        }
    }

    public SimpleTransaction(String transactionName) {
        this.logger = LoggerFactory.getLogger(SimpleTransaction.class, transactionName + " %2$s", null);
        this.name = transactionName;
        this.constructThreadId = Thread.currentThread().getId();
    }

    public SimpleTransaction doAction(Action action) throws FailedTransactionException {
        checkToRun("doAction()");

        try {
            logger.debug("Executing action ", action);
            action.doAction();
            actions.add(action);
            logger.debug("Successfully done.");
        } catch (Throwable t) {
            logger.debug("Failed: ", t);
            state = State.FAILED;
            rollAllBack();
            throw new FailedTransactionException("Transaction failed when running action " + action, t);
        }
        return this;
    }

    public void commit() {
        checkToRun("commit()");
        state = State.COMMITED;
        actions.clear();
    }

    public void cancel() {
        checkToRun("cancel()");
        state = State.CANCELLED;
        rollAllBack();
    }

    private void checkInvoker() {
        if (Thread.currentThread().getId() != constructThreadId) {
            logger.fatal("Usage contract violation: concurrently modification, please investigate: ",
                    new ConcurrentModificationException());
            throw new AssertionError("Usage violation: the instance methods cannot be invoked "
                    + "from different threads: invoker threadId=" + Thread.currentThread() + this);
        }
    }

    private void checkToRun(String actionName) {
        checkInvoker();
        if (state.isFinal()) {
            logger.warn(actionName, " cannot be called in final state ", this);
            throw new IllegalStateException("Wrong state to " + actionName + this);
        }
        if (state.equals(State.INIT)) {
            state = State.RUNNING;
        }
    }

    private void rollAllBack() {
        logger.debug("Rolling back ", actions.size(), " actions");
        while (!actions.isEmpty()) {
            try {
                actions.remove(actions.size() - 1).rollbackAction();
            } catch (Throwable t) {
                logger.error("Ignore exception silently in rolling-back action.",
                        " Action.rollback() should not throw exceptions, please investigate. ", t);
            }
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{\"").append(name).append("\", Tid= ").append(constructThreadId)
                .append(", state=").append(state).append(" }").toString();
    }
}
