package com.xiaolan.seriaportdemo;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android_serialport_api.SerialPort;

public class SerialPortHelper {

    private static final String TAG = "SerialPortHelper";
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private SendThread mSendThread;
    private String sPort = "/dev/ttyS3";
    private int iBaudRate = 9600;
    private String mDataBits = "8";
    private String mStopBits = "1";
    private String mParityBits = "N";
    private boolean _isOpen = false;
    private byte[] _bLoopData = new byte[]{0x30};
    private int iDelay = 500;
    private BufferedInputStream mBufferedInputStream;
    private static final int SHOW_LENGTH = 30;

    //----------------------------------------------------
    public SerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

    public SerialPortHelper(String sPort) {
        this(sPort, 9600);
    }

    public SerialPortHelper(String sPort, String sBaudRate) {
        this(sPort, Integer.parseInt(sBaudRate));
    }

    public SerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    //----------------------------------------------------
    public void open() throws SecurityException, IOException, InvalidParameterException {
//        File device = new File(sPort);
//        //检查访问权限，如果没有读写权限，进行文件操作，修改文件访问权限
//        if (!device.canRead() || !device.canWrite()) {
//            try {
//                //通过挂在到linux的方式，修改文件的操作权限
//                Process su = Runtime.getRuntime().exec("/system/bin/su");
//                //一般的都是/system/bin/su路径，有的也是/system/xbin/su
//                String cmd = "chmod 777 " + device.getAbsolutePath() + "\n" + "exit\n";
//                su.getOutputStream().write(cmd.getBytes());
//
//                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
//                    throw new SecurityException();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new SecurityException();
//            }
//        }

        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, mDataBits, mStopBits, mParityBits);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();
        mBufferedInputStream = new BufferedInputStream(mInputStream);
        mReadThread = new ReadThread();
        mReadThread.start();
        mSendThread = new SendThread();
        mSendThread.setSuspendFlag();
        mSendThread.start();
        _isOpen = true;
    }

    //----------------------------------------------------
    public void close() {
        if (mReadThread != null)
            mReadThread.interrupt();
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
                mInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
                mOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mBufferedInputStream != null) {
            try {
                mBufferedInputStream.close();
                mBufferedInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        _isOpen = false;
    }

    //----------------------------------------------------
    public void send(byte[] bOutArray) {
        try {
            mOutputStream.write(bOutArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----------------------------------------------------
    public void sendHex(String sHex) {
        byte[] bOutArray = MyFunc.HexToByteArr(sHex);
        send(bOutArray);
    }

    //----------------------------------------------------
    public void sendTxt(String sTxt) {
        byte[] bOutArray = sTxt.getBytes();
        send(bOutArray);
    }


    //----------------------------------------------------
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    if (mBufferedInputStream == null)
                        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
                    byte[] buffer = new byte[10];
                    int len;
                    if (mBufferedInputStream.available() > 0) {
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        len = mBufferedInputStream.read(buffer);
                        if (len != -1) {
                            Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
                            sendData(buffer,buffer.length);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: 数据读取异常：" + e.toString());
                    break;
                }
            }
        }

        private void sendData(byte[] bytes, int size) {
            ComBean ComRecData = new ComBean(sPort, bytes, size);
            if (mOnDataReceivedListener != null) {
                mOnDataReceivedListener.onDataReceived(ComRecData);
            }
        }
    }

    //----------------------------------------------------
    private class SendThread extends Thread {
        public boolean suspendFlag = true;// 控制线程的执行

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                synchronized (this) {
                    while (suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                send(getbLoopData());
                Log.e(TAG, "sendSerialPort: 串口数据发送成功");
                try {
                    Thread.sleep(iDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //线程暂停
        public void setSuspendFlag() {
            this.suspendFlag = true;
        }

        //唤醒线程
        public synchronized void setResume() {
            this.suspendFlag = false;
            notify();
        }
    }


    //----------------------------------------------------
    private Integer getArrayRealLength(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                return i;
            }

        }
        return bytes.length;
    }

    public int getBaudRate() {
        return iBaudRate;
    }

    public boolean setBaudRate(int iBaud) {
        if (_isOpen) {
            return false;
        } else {
            iBaudRate = iBaud;
            return true;
        }
    }

    public boolean setBaudRate(String sBaud) {
        int iBaud = Integer.parseInt(sBaud);
        return setBaudRate(iBaud);
    }

    //----------------------------------------------------
    public String getPort() {
        return sPort;
    }

    public boolean setPort(String sPort) {
        if (_isOpen) {
            return false;
        } else {
            this.sPort = sPort;
            return true;
        }
    }

    public String getDataBits() {
        return mDataBits;
    }

    public boolean setDataBits(String dataBits) {
        if (_isOpen) {
            return false;
        }else {
            this.mDataBits = dataBits;
            return true;
        }
    }

    public String getStopBits() {
        return mStopBits;
    }

    public boolean setStopBits(String stopBits) {
        if (_isOpen) {
            return false;
        }else {
            this.mStopBits = stopBits;
            return true;
        }
    }

    public String getParityBits() {
        return mParityBits;
    }

    public boolean setParityBits(String parityBits) {
        if (_isOpen) {
            return false;
        }else {
            this.mParityBits = parityBits;
            return true;
        }
    }

    //----------------------------------------------------
    public boolean isOpen() {
        return _isOpen;
    }

    //----------------------------------------------------
    public byte[] getbLoopData() {
        return _bLoopData;
    }

    //----------------------------------------------------
    public void setbLoopData(byte[] bLoopData) {
        this._bLoopData = bLoopData;
    }

    //----------------------------------------------------
    public void setTxtLoopData(String sTxt) {
        this._bLoopData = sTxt.getBytes();
    }

    //----------------------------------------------------
    public void setHexLoopData(String sHex) {
        this._bLoopData = MyFunc.HexToByteArr(sHex);
    }

    //----------------------------------------------------
    public int getiDelay() {
        return iDelay;
    }

    //----------------------------------------------------
    public void setiDelay(int iDelay) {
        this.iDelay = iDelay;
    }

    //----------------------------------------------------
    public void startSend() {
        if (mSendThread != null) {
            mSendThread.setResume();
        }
    }

    //----------------------------------------------------
    public void stopSend() {
        if (mSendThread != null) {
            mSendThread.setSuspendFlag();
        }
    }

    //----------------------------------------------------
    interface OnDataReceivedListener {
        void onDataReceived(ComBean ComRecData);
    }

    private OnDataReceivedListener mOnDataReceivedListener;

    public void setOnDataReceivedListener(OnDataReceivedListener onDataReceivedListener) {
        mOnDataReceivedListener = onDataReceivedListener;
    }
}
