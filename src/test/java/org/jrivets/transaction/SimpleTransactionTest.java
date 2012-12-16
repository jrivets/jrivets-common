package org.jrivets.transaction;

import static org.junit.Assert.*;

import org.jrivets.transaction.Action;
import org.jrivets.transaction.FailedTransactionException;
import org.jrivets.transaction.SimpleTransaction;
import org.junit.Test;

public class SimpleTransactionTest {

    static class GoodAction implements Action {

        volatile long actionTS = 0L;
        volatile long rolledBackTS = 0L;

        public void doAction() {
            actionTS = System.currentTimeMillis();
            try {
                Thread.sleep(1L);
            } catch (InterruptedException e) {
            }
        }

        public void rollbackAction() {
            rolledBackTS = System.currentTimeMillis();
            try {
                Thread.sleep(1L);
            } catch (InterruptedException e) {
            }
        }
    }

    static class FailedBrutalAction extends GoodAction {
        @Override
        public void doAction() {
            super.doAction();
            throw new RuntimeException("FailedBrutalAction");
        }
    }

    static class FailedRollback extends FailedBrutalAction {
        @Override
        public void rollbackAction() {
            super.rollbackAction();
            throw new IllegalArgumentException("FailedRollback");
        }
    }

    private void checkFinal(SimpleTransaction transaction) throws FailedTransactionException {
        try {
            transaction.doAction(new GoodAction());
        } catch (IllegalStateException iae) {
            return;
        }
        fail("Transaction should throw exception in the state");
    }

    @Test
    public void oneGoodAction() throws FailedTransactionException {
        GoodAction goodAction = new GoodAction();
        SimpleTransaction trans = new SimpleTransaction("oneGoodAction()");
        trans.doAction(goodAction).commit();
        assertTrue(goodAction.actionTS != 0L && goodAction.rolledBackTS == 0L);
        checkFinal(trans);
    }

    @Test
    public void twoGoodAction() throws FailedTransactionException {
        GoodAction goodAction = new GoodAction();
        GoodAction goodAction2 = new GoodAction();
        new SimpleTransaction("twoGoodAction()").doAction(goodAction).doAction(goodAction2).commit();
        assertTrue(goodAction.actionTS != 0L && goodAction.rolledBackTS == 0L);
        assertTrue(goodAction.actionTS < goodAction2.actionTS);
        assertTrue(goodAction.rolledBackTS == goodAction2.rolledBackTS && goodAction2.rolledBackTS == 0L);
    }

    @Test
    public void twoGoodCancelledAction() throws FailedTransactionException {
        GoodAction goodAction = new GoodAction();
        GoodAction goodAction2 = new GoodAction();
        new SimpleTransaction("twoGoodCancelledAction()").doAction(goodAction).doAction(goodAction2).cancel();
        assertTrue(goodAction.actionTS != 0L && goodAction.rolledBackTS != 0L);
        assertTrue(goodAction.actionTS < goodAction2.actionTS);
        assertTrue(goodAction.rolledBackTS > goodAction2.rolledBackTS && goodAction2.rolledBackTS != 0L);
    }
    
    @Test
    public void failedTransactionTest2() {
        FailedBrutalAction action = new FailedBrutalAction();
        try {
            new SimpleTransaction("failedTransactionTest2()").doAction(action);
            fail("Should throw");
        } catch (FailedTransactionException tfe) {
            assertTrue(tfe.getCause() instanceof RuntimeException);
        }
    }

    @Test
    public void failedTransactionTest3() {
        try {
            new SimpleTransaction("failedTransactionTest3()").doAction(null);
            fail("Should throw");
        } catch (FailedTransactionException tfe) {
            assertTrue(tfe.getCause() instanceof RuntimeException);
        }

    }

    @Test
    public void failedTransactionTest4() throws FailedTransactionException {
        FailedBrutalAction action = new FailedRollback();
        SimpleTransaction trans = null;
        try {
            trans = new SimpleTransaction("failedTransactionTest4()");
            trans.doAction(action);
            fail("Should throw");
        } catch (FailedTransactionException tfe) {
            assertTrue(tfe.getCause() instanceof RuntimeException);
            assertFalse(tfe.getCause() instanceof IllegalArgumentException);
        } catch (IllegalArgumentException iae) {
            fail("Never happen");
        }
        checkFinal(trans);
    }

    @Test
    public void rollbackOrderTest() throws FailedTransactionException {
        GoodAction action = new GoodAction();
        FailedBrutalAction action2 = new FailedBrutalAction();
        SimpleTransaction trans = null;
        try {
            trans = new SimpleTransaction("rollbackOrderTest()");
            trans.doAction(action).doAction(action2);
            fail("Should throw");
        } catch (FailedTransactionException tfe) {
            assertTrue(action.actionTS < action2.actionTS);
            assertTrue(action.rolledBackTS > action2.rolledBackTS);
            assertTrue(action2.rolledBackTS == 0L);
        } catch (IllegalArgumentException iae) {
            fail("Never happen");
        }
        checkFinal(trans);
    }

    @Test
    public void concurrentDoActionTest() throws InterruptedException {
        final SimpleTransaction trans = new SimpleTransaction("concurrentDoActionTest()");
        synchronized (trans) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        trans.doAction(null);
                    } catch (AssertionError ae) {
                        synchronized (trans) {
                            trans.notify();
                        }
                    } catch (Throwable t) {
                    }
                }
            }).start();
            long start = System.currentTimeMillis();
            trans.wait(2000L);
            assertTrue(System.currentTimeMillis() - start < 2000L);
        }
    }

    @Test
    public void concurrentCommit() throws InterruptedException {
        final SimpleTransaction trans = new SimpleTransaction("concurrentCommit()");
        synchronized (trans) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        trans.commit();
                    } catch (AssertionError ae) {
                        synchronized (trans) {
                            trans.notify();
                        }
                    } catch (Throwable t) {
                    }
                }
            }).start();
            long start = System.currentTimeMillis();
            trans.wait(2000L);
            assertTrue(System.currentTimeMillis() - start < 2000L);
        }
    }
}
