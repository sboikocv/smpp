package com.sboiko.smpp.server;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.sboiko.smpp.session.DefaultSessionHandler;
import com.sboiko.smpp.data.Msg;
import com.sboiko.smpp.router.AbstractRouter;
import org.slf4j.Logger;

public class DefaultSmppServerHandler implements SmppServerHandler {

    private final Logger logger;
    private AbstractRouter router = null;

    public DefaultSmppServerHandler(Logger logger) {
        this.logger = logger;
    }

    public void setRouter(AbstractRouter router) {
        this.router = router;
    }

    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, BaseBind bindRequest) throws SmppProcessingException {
        logger.info("Connect with {}", bindRequest);
        sessionConfiguration.setName("Session." + sessionId.toString());
    }

    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        DefaultSessionHandler sessionHandler = new ServerSessionHandler(logger);
        sessionHandler.setRouter(router);
        logger.info("Session created {}", sessionId);
        sessionHandler.setSession(session);
        session.serverReady(sessionHandler);
        sessionHandler.setReady();
    }

    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        logger.info("Session destroy {}", sessionId);
        session.destroy();
    }

    public class ServerSessionHandler extends DefaultSessionHandler {

        public ServerSessionHandler(Logger logger) {
            super(logger);
        }

        @Override
        protected PduRequest toPdu(Msg msg) {
            return msg.toPdu(SmppConstants.CMD_ID_DELIVER_SM);
        }

    }

}