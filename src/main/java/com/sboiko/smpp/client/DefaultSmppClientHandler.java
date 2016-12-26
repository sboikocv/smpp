package com.sboiko.smpp.client;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.sboiko.smpp.session.DefaultSessionHandler;
import com.sboiko.smpp.session.SmppCallback;
import com.sboiko.smpp.data.Msg;
import com.sboiko.smpp.router.AbstractRouter;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultSmppClientHandler extends DefaultSmppClient implements SmppCallback {

    private final Logger logger;
    private ScheduledExecutorService scheduler = null;
    private SmppSessionConfiguration config = null;
    private AbstractRouter router = null;
    private Integer reconnectDelay = 0;
    private int expectedSessions = 1;
    private List<SmppSession> sessions = null;

    public DefaultSmppClientHandler(ExecutorService executors, int expectedSessions, ScheduledExecutorService monitorExecutor, Logger logger) {
        super(executors, expectedSessions, monitorExecutor);
        this.scheduler = monitorExecutor;
        this.logger = logger;
        this.expectedSessions = expectedSessions;
        this.sessions = new LinkedList<SmppSession>();
    }

    public void setReconnectDelay(Integer delay) {
        this.reconnectDelay = delay;
    }

    public void setRouter(AbstractRouter router) {
        this.router = router;
    }

    public void start(SmppSessionConfiguration config) {
        this.config = config;
        for(int i =0; i < expectedSessions; ++i) {
            onConnectionClose(null);
        }
    }

    private void bind() {
        try {
            DefaultSessionHandler sessionHandler = new ClientSessionHandler(logger);
            SmppSession session = bind(this.config, sessionHandler);
            sessionHandler.setRouter(router);
            sessionHandler.setSmppCallback(this);
            sessionHandler.setSession(session);
            sessionHandler.setReady();
            this.sessions.add(session);
        } catch (Exception ex) {
            logger.error("Could not connect:");
            onConnectionClose(null);
        }
    }

    public void onConnectionClose(SmppSession session) {
        Runnable runnable = new Runnable() {
            public void run() {
                bind();
            }
        };
        if (session != null) {
            this.sessions.remove(session);
            session.destroy();
        }
        scheduler.schedule(runnable, reconnectDelay, TimeUnit.SECONDS);
    }

    public class ClientSessionHandler extends DefaultSessionHandler {

        public ClientSessionHandler(Logger logger) {
            super(logger);
        }

        @Override
        protected PduRequest toPdu(Msg msg) {
            return msg.toPdu(SmppConstants.CMD_ID_SUBMIT_SM);
        }

    }
}
