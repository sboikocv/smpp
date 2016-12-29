package com.sboiko.smpp.session;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.sboiko.smpp.data.Msg;
import com.sboiko.smpp.router.AbstractRouter;
import org.slf4j.Logger;

public class DefaultSessionHandler extends DefaultSmppSessionHandler {
    private final Logger logger;
    private SmppCallback smppCallback = null;
    private SmppSession session = null;
    private boolean ready = false;
    protected AbstractRouter router = null;

    public DefaultSessionHandler(Logger logger) {
        super(logger);
        this.logger = logger;
    }

    public void setSmppCallback(SmppCallback smppCallback) {
        this.smppCallback = smppCallback;
    }

    public void setSession(SmppSession session) {
        this.session = session;
    }

    public void setReady() {
        this.ready = true;
        this.router.add(this);
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setRouter(AbstractRouter router) {
        this.router = router;
    }

    protected PduRequest toPdu(Msg msg) {
        return null;
    }

    public boolean send(Msg msg) {
        logger.info("Send message");
        if (isReady() && session.isBound()) {
            try {
                PduRequest pdu = toPdu(msg);
                pdu.setReferenceObject(msg);
                session.sendRequestPdu(pdu, 1000, false);
                return true;
            } catch (Exception ex) {
                logger.error("Error happens during message send. Retry on reconnect.");
                router.failToSend(msg);
                return false;
            }
        } else {
            /* Throw some other exception */
            logger.error("Could not send message to uninitialized session. Store into list");
        }

        return false;
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        logger.warn("Pdu request expired: {}", pduRequest);
        router.failToSend((Msg)pduRequest.getReferenceObject());
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        PduResponse response = pduRequest.createResponse();
        logger.debug("Receive request: {}", pduRequest);
        Msg msg = Msg.fromPdu(pduRequest);
        if (msg != null) {
            this.router.produce(msg);
        }
        return response;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        logger.debug("{}", pduAsyncResponse.getResponse());
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        this.router.remove(this);
        ready = false;
        logger.error("Unexpectedly closed smpp connection");
        if (smppCallback != null) {
            smppCallback.onConnectionClose(this.session);
        }
    }
}
