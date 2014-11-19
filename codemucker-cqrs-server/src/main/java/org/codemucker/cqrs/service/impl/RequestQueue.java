package org.codemucker.cqrs.service.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.codemucker.cqrs.service.RequestContext;

/**
 * Queues requests and processes in executor threads
 */
public abstract class RequestQueue implements Closeable {

    final int executorCorePoolSize = 5;
    final int executorMaxPoolSize = 10;
    final long executorKeepAliveTimeMs = 5000;
    final int requestQueueSize = 1000;
    final long requestPollTimeoutMs = 50;
    
    final ExecutorService executor;
    // TODO:replace with disruptor
    final BlockingQueue<RequestContext> requestQueue;
    volatile boolean keepRunning = true;
    private final Thread queueThread;

    public RequestQueue() {
        requestQueue = new LinkedBlockingQueue<>(requestQueueSize);
        executor = new ThreadPoolExecutor(executorCorePoolSize, executorMaxPoolSize, executorKeepAliveTimeMs, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        queueThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (keepRunning) {
                    try {
                        final RequestContext ctxt = requestQueue.take();
                        if (ctxt != null) {
                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    processRequest(ctxt);
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        queueThread.start();
    }

    public void push(RequestContext context) throws InterruptedException {
        requestQueue.put(context);
    }

    public abstract void processRequest(RequestContext ctxt);
    

    @Override
    public void close() throws IOException {
        stop();
    }

    public void stop() {
        keepRunning = false;
        queueThread.interrupt();
    }

}
