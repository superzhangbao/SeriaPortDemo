package com.xiaolan.seriaportdemo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

import android_serialport_api.SerialPortFinder;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, SerialPortHelper.OnDataReceivedListener {
    private static final String TAG = "MainActivity";
    @BindView(R.id.btn_set_port)
    Button mBtnSetPort;
    @BindView(R.id.btn_set_baud)
    Button mBtnSetBaud;
    @BindView(R.id.btn_set_data)
    Button mBtnSetData;
    @BindView(R.id.btn_set_stop)
    Button mBtnSetStop;
    @BindView(R.id.btn_set_verify)
    Button mBtnSetVerify;
    @BindView(R.id.cb_hex)
    RadioButton mCbHex;
    @BindView(R.id.cb_text)
    RadioButton mCbText;
    @BindView(R.id.rg)
    RadioGroup mRg;
    @BindView(R.id.et)
    EditText mEt;
    @BindView(R.id.btn_send)
    Button mBtnSend;
    @BindView(R.id.btn_open_port)
    Button mBtnOpenPort;
    @BindView(R.id.btn_clear)
    Button mBtnClear;
    @BindView(R.id.cb_hex_rev)
    RadioButton mCbHexRev;
    @BindView(R.id.cb_text_rev)
    RadioButton mCbTextRev;
    @BindView(R.id.rg_rev)
    RadioGroup mRgRev;
    @BindView(R.id.editTextRecDisp)
    EditText mEditTextRecDisp;


    private MyHandler mMyHandler;
    private int sendType = 0;
    private int revType = 0;
    private DispQueueThread mDispQueueThread;
    private SerialPortFinder mPortFinder;
    private SerialPortHelper mPortHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        KeybordUtils.isSoftInputShow(this);
        mEt.clearFocus();

        mCbHex.setChecked(true);
        mCbHexRev.setChecked(true);

        mRg.setOnCheckedChangeListener(this);
        mRgRev.setOnCheckedChangeListener(this);
        mMyHandler = new MyHandler();
        mDispQueueThread = new DispQueueThread();
        mDispQueueThread.start();
        mPortFinder = new SerialPortFinder();
        mPortHelper = new SerialPortHelper();
        mPortHelper.setOnDataReceivedListener(this);
    }

    @OnClick({R.id.btn_set_port, R.id.btn_set_baud, R.id.btn_set_data, R.id.btn_set_stop,
            R.id.btn_set_verify, R.id.btn_send, R.id.btn_open_port,
            R.id.btn_clear})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_set_port:
                new AlertDialog.Builder(this)
                        .setTitle("选择串口")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getAllDevices(), 0, (dialog, which) -> {
                            String device = mPortFinder.getAllDevicesPath()[which];
                            mBtnSetPort.setText(device);
                            mPortHelper.setPort(device);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_baud:
                new AlertDialog.Builder(this)
                        .setTitle("选择波特率")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getBaudRateList(), 0, (dialog, which) -> {
                            String bRate = mPortFinder.getBaudRateList()[which];
                            Integer baudRate = Integer.valueOf(bRate);
                            mBtnSetBaud.setText(bRate);
                            mPortHelper.setBaudRate(baudRate);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_data:
                new AlertDialog.Builder(this)
                        .setTitle("选择数据位")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getDataBits(), 0, (dialog, which) -> {
                            String dataBits = mPortFinder.getDataBits()[which];
                            mBtnSetData.setText(dataBits);
                            mPortHelper.setDataBits(dataBits);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_stop:
                new AlertDialog.Builder(this)
                        .setTitle("选择停止位")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getStopBits(), 0, (dialog, which) -> {
                            String stopBits = mPortFinder.getStopBits()[which];
                            mBtnSetStop.setText(stopBits);
                            mPortHelper.setStopBits(stopBits);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_verify:
                new AlertDialog.Builder(this)
                        .setTitle("选择停止位")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getParityBits(), 0, (dialog, which) -> {
                            String parityBits = mPortFinder.getParityBits()[which];
                            mBtnSetVerify.setText(parityBits);
                            mPortHelper.setParityBits(parityBits);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_send:
                if (mPortHelper != null && !mPortHelper.isOpen()) {
                    Toast.makeText(this, "请打开串口", Toast.LENGTH_SHORT).show();
                    return;
                }
                String trim = mEt.getText().toString().trim();
                if (!TextUtils.isEmpty(trim)) {
                    if (sendType == 0) {
                        mPortHelper.sendHex(trim);
                    } else {
                        mPortHelper.sendTxt(trim);
                    }
                } else {
                    Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_open_port:
                //打开/关闭串口
                if (mBtnOpenPort.getText().equals("打开串口")) {
                    try {
                        mBtnSetPort.setText(mPortHelper.getPort());
                        mBtnSetBaud.setText(String.valueOf(mPortHelper.getBaudRate()));
                        mBtnSetData.setText(mPortHelper.getDataBits());
                        mBtnSetStop.setText(mPortHelper.getStopBits());
                        mBtnSetVerify.setText(mPortHelper.getParityBits());
                        mPortHelper.open();
                        mBtnOpenPort.setText("关闭串口");
                    } catch (Exception e) {
                        e.printStackTrace();
                        mBtnOpenPort.setText("打开串口");
                    }
                } else {
                    mPortHelper.close();
                    mBtnOpenPort.setText("打开串口");
                }
                break;
            case R.id.btn_clear:
                //清空数据接收区
                mEditTextRecDisp.setText("");
                count = 0;
                mEt.clearFocus();
                KeybordUtils.closeKeybord(mEt, this);
                break;
        }
    }


    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == mCbHex.getId()) {
            sendType = 0;
        } else if (checkedId == mCbText.getId()) {
            sendType = 1;
        } else if (checkedId == mCbHexRev.getId()) {
            revType = 0;
        } else if (checkedId == mCbTextRev.getId()) {
            revType = 1;
        }
    }

    @Override
    public void onDataReceived(ComBean comBean) {
        runOnUiThread(() -> {
            mDispQueueThread.AddQueue(comBean);//线程定时刷新显示
        });
    }

    //----------------------------------------------------刷新显示线程
    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    runOnUiThread(() -> DispRecData(ComData));
                    try {
                        Thread.sleep(50);//显示性能高的话，可以把此数值调小。
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }

    private int count = 0;

    //----------------------------------------------------显示接收数据
    private void DispRecData(ComBean comBean) {
        StringBuilder sMsg = new StringBuilder();
        sMsg.append(comBean.sRecTime);
        sMsg.append("[");
        sMsg.append(comBean.sComPort);
        sMsg.append("]");
        if (revType == 0) {
            sMsg.append("[Hex] ");
            String hex = MyFunc.ByteArrToHex(comBean.bRec);
            String[] split = hex.split(" ");
            sMsg.append(hex);
            sMsg.append("------->").append(count).append("------->size:").append(split.length);
            Log.w(TAG, hex);
        } else {
            sMsg.append("[Text] ");
            String text = null;
            try {
                text = new String(comBean.bRec,"utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            sMsg.append(text);
            sMsg.append("------->").append(count).append("------->size:").append(text.length());
            Log.w(TAG, text);
        }
        count++;
        if (comBean.mRecOrSend == 0) {
            sMsg.append("---[收]");
        } else if (comBean.mRecOrSend == 1) {
            sMsg.append("---[发]");
        }
        sMsg.append("\r\n");
        mEditTextRecDisp.append(sMsg);
    }

    private static class MyHandler extends Handler {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyHandler != null) {
            mMyHandler.removeCallbacksAndMessages(null);
            mMyHandler = null;
        }
        mPortHelper.close();
    }
}
