package com.sboiko.smpp.router;


import com.sboiko.smpp.data.Msg;
import com.sboiko.smpp.session.DefaultSessionHandler;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class RouterQueue implements AbstractRouter {

    private final ArrayList<DefaultSessionHandler> sessions;
    private final Logger logger;
    private BlockingQueue<Msg> messages = null;
    private ThreadPoolExecutor executor = null;
    private AtomicBoolean running = new AtomicBoolean(false);
    private long counter = 0;

    public RouterQueue(ThreadPoolExecutor executor, Logger logger) {
        this.sessions = new ArrayList<>();
        this.logger = logger;
        this.messages = new LinkedBlockingQueue<>();
        this.executor = executor;
    }

    public void start() {
        running.set(true);
        this.executor.getThreadFactory().newThread(new Runnable() {
            @Override
            public void run() {
                while(running.get()) {
                    try {
                        Msg msg = messages.take();
                        if (msg != null) {
                            route(msg);
                        }
                    } catch (InterruptedException ex) {
                        logger.info("Message queue interrupted");
                    }
                }
            }
        }).start();
    }

    private void route(Msg msg) {
        DefaultSessionHandler session;
        if (msg == null || sessions.size() == 0) {
            logger.info("Drop message");
            return;
        }
        synchronized (sessions) {
            do {
                ++counter;
                if (counter < 0) {
                    counter = 0;
                }
                session = sessions.get((int) (counter % sessions.size()));
            } while (!session.send(msg));
        }
    }

    @Override
    public void produce(Msg msg) {
        messages.add(msg);
    }

    public void add(DefaultSessionHandler sessionHandler) {
        synchronized (sessions) {
            sessions.add(sessionHandler);
        }
    }

    public boolean remove(DefaultSessionHandler sessionHandler) {
        synchronized (sessions) {
            return sessions.remove(sessionHandler);
        }
    }

    @Override
    public void failToSend(Msg msg) {
        logger.error("Fail to send message");
    }

    public void destroy() {
        running.set(false);
        try {
            messages.put(new Msg());
        } catch (InterruptedException ex) {
            logger.error("{}", ex.getMessage());
        }
    }
}
