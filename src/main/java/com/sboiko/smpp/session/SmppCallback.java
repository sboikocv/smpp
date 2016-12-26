package com.sboiko.smpp.session;

import com.cloudhopper.smpp.SmppSession;

public interface SmppCallback {

    void onConnectionClose(SmppSession session);

}
