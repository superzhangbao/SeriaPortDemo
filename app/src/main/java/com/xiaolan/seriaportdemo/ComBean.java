package com.xiaolan.seriaportdemo;

import java.text.SimpleDateFormat;

public class ComBean {
    public byte[] bRec;
    public String sRecTime;
    public String sComPort;
    public int mRecOrSend = 0;
    public ComBean(String sPort, byte[] buffer, int recOrSend){
        sComPort=sPort;
        bRec = buffer;
        mRecOrSend = recOrSend;
//        bRec=new byte[recOrSend];
//        System.arraycopy(buffer, 0, bRec, 0, recOrSend);

        SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
        sRecTime = sDateFormat.format(new java.util.Date());
    }
}
