package com.sboiko.smpp.data;


import com.amazonaws.util.Base64;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;

import java.io.*;


public class Msg implements Serializable {

    private String sourceAddress = null;
    private String destAddress = null;
    private byte dataCoding = 0x0;
    private byte [] textBytes = null;

    public Msg() {
        /* nothing */
    }

    public String toString() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(this);
            so.flush();
            return new String(Base64.encode(bo.toByteArray()));
        } catch (Exception ex) {
            return null;
        }
    }

    public static Msg fromString(String string) {
        try {
            byte b[] = Base64.decode(string.getBytes());
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream oi = new ObjectInputStream(bi);
            return (Msg)oi.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    private Msg (SubmitSm pdu) {
        sourceAddress = pdu.getSourceAddress().getAddress();
        destAddress = pdu.getDestAddress().getAddress();
        dataCoding = pdu.getDataCoding();
        textBytes = pdu.getShortMessage();
    }

    private Msg (DeliverSm pdu) {
        sourceAddress = pdu.getSourceAddress().getAddress();
        destAddress = pdu.getDestAddress().getAddress();
        dataCoding = pdu.getDataCoding();
        textBytes = pdu.getShortMessage();
    }

    public static Msg fromPdu(PduRequest pdu) {
        switch (pdu.getCommandId()) {
            case SmppConstants.CMD_ID_SUBMIT_SM:
                return new Msg((SubmitSm)pdu);
            case SmppConstants.CMD_ID_DELIVER_SM:
                return new Msg((DeliverSm)pdu);
        }
        return null;
    }

    public byte getDataCoding() {
        return dataCoding;
    }

    public void setDataCoding(byte dataCoding) {
        this.dataCoding = dataCoding;
    }

    public String getDestAddress() {
        return destAddress;
    }

    public void setDestAddress(String destAddress) {
        this.destAddress = destAddress;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public byte[] getTextBytes() {
        return textBytes;
    }

    public void setTextBytes(byte[] textBytes) {
        this.textBytes = textBytes;
    }

    private SubmitSm toSubmitSm() {
        SubmitSm sm = new SubmitSm();
        sm.setSourceAddress(new Address((byte)0x1, (byte)0x1, sourceAddress));
        sm.setDestAddress(new Address((byte)0x1, (byte)0x1, destAddress));
        sm.setDataCoding(dataCoding);
        try {
            sm.setShortMessage(textBytes);
        } catch (SmppInvalidArgumentException ex) {
            return null;
        }
        return sm;
    }

    private DeliverSm toDeliverSm() {
        DeliverSm sm = new DeliverSm();
        sm.setSourceAddress(new Address((byte)0x1, (byte)0x1, sourceAddress));
        sm.setDestAddress(new Address((byte)0x1, (byte)0x1, destAddress));
        sm.setDataCoding(dataCoding);
        try {
            sm.setShortMessage(textBytes);
        } catch (SmppInvalidArgumentException ex) {
            return null;
        }
        return sm;
    }

    public PduRequest toPdu(int type) {
        switch (type) {
            case SmppConstants.CMD_ID_SUBMIT_SM:
                return toSubmitSm();
            case SmppConstants.CMD_ID_DELIVER_SM:
                return toDeliverSm();
            default:
                return null;
        }
    }
}
