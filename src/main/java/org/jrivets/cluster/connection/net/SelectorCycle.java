package org.jrivets.cluster.connection.net;

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

    protected SelectorCycle(Logger logger) throws IOException {
        this.logger = logger;
        this.selector = Selector.open();
        this.cycleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mainCycle();
                } catch (IOException e) {
                }
            }
        });
    }

    void onCommand(Runnable command) {
        commandQueue.add(command);
        selector.wakeup();
    }

    void start() {
        cycleThread.start();
    }

    void stop() throws InterruptedException {
        cycleThread.interrupt();
        cycleThread.join();
    }

    protected abstract void onSelect(SelectionKey sKey);

    private void mainCycle() throws IOException {
        logger.info("Starting main cycle");
        try {
            while (!Thread.interrupted()) {
                selectCycle();
                commandCycle();
            }
        } finally {
            logger.info("Ending main cycle");
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
