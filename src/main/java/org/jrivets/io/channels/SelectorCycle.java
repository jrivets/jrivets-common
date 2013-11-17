package org.jrivets.io.channels;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jrivets.log.Logger;

abstract class SelectorCycle {

    protected final Logger logger;

    protected final Selector selector;

    private final ConcurrentLinkedQueue<Runnable> commandQueue = new ConcurrentLinkedQueue<Runnable>();

    private final Thread cycleThread;

    private final Runnable mainCycle = new Runnable() {
        @Override
        public void run() {
            try {
                mainCycle();
            } catch (IOException e) {
                logger.error("Finalizing main cycle with the exception ", e);
            }
        }
    };

    protected SelectorCycle(Logger logger) throws IOException {
        this.logger = logger;
        this.selector = Selector.open();
        this.cycleThread = new Thread(mainCycle);
    }

    void onCommand(Runnable command) {
        commandQueue.add(command);
        selector.wakeup();
    }

    void start() {
        cycleThread.start();
    }

    void stop() {
        try {
            cycleThread.interrupt();
            cycleThread.join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Cannot wait cycle finalization, interrupted.");
        }
    }

    protected abstract void onSelect(SelectionKey sKey);

    protected void miscCycle() {

    }

    /**
     * The implementation should never throw!
     */
    protected void finalized() {

    }

    private void mainCycle() throws IOException {
        logger.info("Starting main cycle");
        try {
            while (!Thread.interrupted()) {
                selectCycle();
                commandCycle();
                miscCycle();
            }
        } finally {
            finalized();
            logger.info("Finishing main cycle");
            selector.close();
        }
    }

    private void selectCycle() throws IOException {
        selector.select();
        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            SelectionKey sKey = keyIterator.next();
            keyIterator.remove();
            onSelect(sKey);
        }
    }

    private void commandCycle() {
        Runnable command = commandQueue.poll();
        while (command != null) {
            try {
                command.run();
            } catch (Throwable t) {
                logger.debug("Error while executing command ", command, t);
            }
            command = commandQueue.poll();
        }
    }

    @Override
    public String toString() {
        return "{queueSize=" + commandQueue.size() + "}";
    }

}
