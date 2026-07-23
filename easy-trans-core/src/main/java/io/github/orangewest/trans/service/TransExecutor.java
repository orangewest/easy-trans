package io.github.orangewest.trans.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lazy virtual-thread executor for {@link TransService}. Creates a
 * {@link Executors#newThreadPerTaskExecutor per-task virtual thread}
 * executor on first use; users may inject their own via
 * {@link #set(ExecutorService)} instead. Only the default executor
 * is closed on {@link #close()}.
 */
final class TransExecutor implements AutoCloseable {

    private volatile ExecutorService executor;
    private volatile boolean defaultCreated;

    ExecutorService get() {
        ExecutorService e = executor;
        if (e == null) {
            synchronized (this) {
                e = executor;
                if (e == null) {
                    e = Executors.newThreadPerTaskExecutor(
                            Thread.ofVirtual().name("trans-", 0).factory());
                    executor = e;
                    defaultCreated = true;
                }
            }
        }
        return e;
    }

    void set(ExecutorService executor) {
        ExecutorService previous = this.executor;
        if (defaultCreated && previous != null && previous != executor) {
            previous.close();
        }
        this.executor = executor;
        this.defaultCreated = false;
    }

    @Override
    public void close() {
        ExecutorService e = executor;
        if (defaultCreated && e != null) {
            e.close();
            executor = null;
            defaultCreated = false;
        }
    }
}
