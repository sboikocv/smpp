package com.sboiko.smpp.data;


import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppSessionConfiguration;

public class SmppConfiguration {

    private static SmppServerConfiguration serverConfiguration= null;
    private static SmppSessionConfiguration sessionConfiguration = null;

    public static SmppServerConfiguration getServerConfiguration() {
        if (serverConfiguration == null) {
            serverConfiguration = new SmppServerConfiguration();
            serverConfiguration.setPort(2776);
            serverConfiguration.setMaxConnectionSize(10);
            serverConfiguration.setNonBlockingSocketsEnabled(true);
            serverConfiguration.setDefaultRequestExpiryTimeout(30000);
            serverConfiguration.setDefaultWindowMonitorInterval(15000);
            serverConfiguration.setDefaultWindowSize(5);
            serverConfiguration.setDefaultWindowWaitTimeout(serverConfiguration.getDefaultRequestExpiryTimeout());
            serverConfiguration.setDefaultSessionCountersEnabled(true);
            serverConfiguration.setJmxEnabled(true);
        }
        return serverConfiguration;
    }

    public static SmppSessionConfiguration getSessionConfiguration() {
        if (sessionConfiguration == null) {
            sessionConfiguration = new SmppSessionConfiguration();
            sessionConfiguration.setWindowSize(1);
            sessionConfiguration.setName("Session1");
            sessionConfiguration.setType(SmppBindType.TRANSCEIVER);
            sessionConfiguration.setHost("127.0.0.1");
            sessionConfiguration.setPort(2525);
            sessionConfiguration.setSystemId("");
            sessionConfiguration.setPassword("");
            sessionConfiguration.setSystemType("");
            sessionConfiguration.setInterfaceVersion(SmppConstants.VERSION_3_4);
            sessionConfiguration.setRequestExpiryTimeout(30000);
            sessionConfiguration.setWindowMonitorInterval(10000);
            sessionConfiguration.setCountersEnabled(true);
        }
        return sessionConfiguration;
    }

}
