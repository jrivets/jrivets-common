package org.jrivets.transaction;

import java.io.IOException;

import org.jrivets.util.testing.Example;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleTransactionExample {

    @Example("General approach and it has 3 steps only...")
    public void threeStepCode() {
        int step = 0;
        try {
            step1();
            step++;
            step2();
            step++;
            step3();
        } catch (Exception e) {
            if (step == 2) {
                step--;
                try {
                    step2Roolback();
                } catch (Throwable t) {
                    // oops, we don't want it...
                }
            }
            if (step == 1) {
                try {
                    step1Roolback();
                } catch (Throwable t) {
                    // oops, we want it never throws...
                }
            }
            // do something with the result if you want.
        }
    }

    @Example("Using the SimpleTransaction class")
    public void simpleTransactionCode() {
        try {
            new SimpleTransaction("Example")
                .doAction(new Step1Action())
                .doAction(new Step2Action())
                .doAction(new Step3Action())
                .commit();
        } catch (FailedTransactionException fte) {
            // do something with the result if you want.
        }
    }

    private String result;
    
    private boolean step1Throws;
    
    private boolean step2RollbackThrows;
    
    private boolean step2Throws;
    
    // Time to check simpleTransactionCode() correctness
    @Test
    public void simpleTransactionStep1() {
        result = "";
        step1Throws = true;
        simpleTransactionCode();
        assertEquals(" step1Throws", result);
    }
    
    @Test
    public void simpleTransactionStep2() {
        result = "";
        step2Throws = true;
        simpleTransactionCode();
        assertEquals(" step1 step2Throws step1Rollback", result);
    }
    
    @Test
    public void simpleTransactionStep3() {
        result = "";
        simpleTransactionCode();
        assertEquals(" step1 step2 step3Throws step2Rollback step1Rollback", result);
    }
    
    @Test
    public void simpleTransactionStep3RolbackStep2() {
        result = "";
        step2RollbackThrows = true;
        simpleTransactionCode();
        assertEquals(" step1 step2 step3Throws step2RollbackThrows step1Rollback", result);
    }
    
    // Time to check threeStepCode() correctness
    @Test
    public void threeStepCodeStep1() {
        result = "";
        step1Throws = true;
        threeStepCode();
        assertEquals(" step1Throws", result);
    }
    
    @Test
    public void threeStepCodeStep2() {
        result = "";
        step2Throws = true;
        threeStepCode();
        assertEquals(" step1 step2Throws step1Rollback", result);
    }
    
    @Test
    public void threeStepCodeStep3() {
        result = "";
        threeStepCode();
        assertEquals(" step1 step2 step3Throws step2Rollback step1Rollback", result);
    }
    
    @Test
    public void threeStepCodeStep3RolbackStep2() {
        result = "";
        step2RollbackThrows = true;
        threeStepCode();
        assertEquals(" step1 step2 step3Throws step2RollbackThrows step1Rollback", result);
    }
    
    private void step1() throws IOException {
        result += " step1";
        if (step1Throws) {
            result += "Throws";
            throw new IOException();
        }
    }

    private class Step1Action implements Action {

        @Override
        public void doAction() throws Throwable {
            step1();
        }

        @Override
        public void rollbackAction() {
            step1Roolback();
        }
    }

    private class Step2Action implements Action {

        @Override
        public void doAction() throws Throwable {
            step2();
        }

        @Override
        public void rollbackAction() {
            step2Roolback();
        }
    }

    private class Step3Action implements Action {

        @Override
        public void doAction() throws Throwable {
            step3();
        }

        @Override
        public void rollbackAction() {
        }
    }

    private void step2() throws Exception {
        result += " step2";
        if (step2Throws) {
            result += "Throws";
            throw new Exception();
        }
    }
    
    private void step3() throws Exception {
        result += " step3Throws";
        throw new Exception();
    }
    
    private void step1Roolback() {
        result += " step1Rollback";
    }
    
    private void step2Roolback() {
        result += " step2Rollback";
        if (step2RollbackThrows) {
            result += "Throws";
            throw new RuntimeException();
        }
    }
}
