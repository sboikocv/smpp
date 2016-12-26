package com.sboiko.smpp.router;

import com.sboiko.smpp.session.DefaultSessionHandler;
import com.sboiko.smpp.data.Msg;

public interface AbstractRouter {
    void produce(Msg msg);
    void add(DefaultSessionHandler sessionHandler);
    boolean remove(DefaultSessionHandler sessionHandler);
    void failToSend(Msg msg);
}
