package com.liyujie.stringmouth;

import android.os.CountDownTimer;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.dyhdyh.support.countdowntimer.CountDownTimerSupport;
import com.dyhdyh.support.countdowntimer.OnCountDownTimerListener;
import com.liyujie.stringmouth.listener.AddOnClickListener;

import java.io.File;
import java.util.concurrent.Semaphore;

import me.f1reking.serialportlib.SerialPortHelper;
import me.f1reking.serialportlib.entity.DATAB;
import me.f1reking.serialportlib.entity.FLOWCON;
import me.f1reking.serialportlib.entity.PARITY;
import me.f1reking.serialportlib.entity.STOPB;
import me.f1reking.serialportlib.listener.IOpenSerialPortListener;
import me.f1reking.serialportlib.listener.ISerialPortDataListener;
import me.f1reking.serialportlib.listener.Status;

public class ShuaKaPortUtils {
    private static SerialPortHelper serialPortHelper;
    private static StringBuffer buffer = new StringBuffer();
    //倒计时
    private static CountDownTimerSupport countDownTimerSupport = new CountDownTimerSupport(500, 100);
    /**
     * 初始化
     */
    public static void getInstance() {
        serialPortHelper = new SerialPortHelper();
        serialPortHelper.setIOpenSerialPortListener(new IOpenSerialPortListener() {
            @Override
            public void onSuccess(File device) {
                LogUtils.d("OpenPortUtils","成功"+device.getPath());
            }

            @Override
            public void onFail(File device, Status status) {
                switch (status) {
                    case NO_READ_WRITE_PERMISSION:
                        LogUtils.d("OpenPortUtils",device.getPath() + " :没有读写权限");
                        break;
                    case OPEN_FAIL:
                    default:
                        LogUtils.d("OpenPortUtils",device.getPath() + " :串口打开失败");
                        break;
                }
            }
        });
        //倒计时
        countDownTimerSupport.setOnCountDownTimerListener(new OnCountDownTimerListener() {
            @Override
            public void onTick(long millisUntilFinished) {

            }
            @Override
            public void onFinish() {
                if(null != mAddOnClickListener){
                    LogUtils.d(buffer.toString());
                    mAddOnClickListener.onSkipClicked(buffer.toString());
                }
                buffer = new StringBuffer();
            }
        });
    }

    /**
     * 获得所有串口设备的地址
     * @return
     */
    public static String[] getAllDeicesPath(){
        return serialPortHelper.getAllDeicesPath();
    }





    /**
     * 刷卡开启串口
     */
    public static boolean swipingCardSize(String port){
        boolean whetherOpen = false;
        //打开串口
        if(null != serialPortHelper){
            if(serialPortHelper.isOpen()){
                LogUtils.d("串口已经开启成功");
                ToastUtils.showShort("串口已经开启成功");
                whetherOpen = true;
            }else {
                serialPortHelper.setPort(port);
                serialPortHelper.setBaudRate(9600);
                serialPortHelper.setStopBits(STOPB.getStopBit(STOPB.B1));
                serialPortHelper.setDataBits(DATAB.getDataBit(DATAB.CS8));
                serialPortHelper.setParity(PARITY.getParity(PARITY.NONE));
                serialPortHelper.setFlowCon(FLOWCON.getFlowCon(FLOWCON.NONE));
                //定义串口数据接收回调
                serialPortHelper.setISerialPortDataListener(new ISerialPortDataListener() {
                    @Override
                    public void onDataReceived(byte[] bytes) {
                        String swipingCard = CommonUtils.bytesToHexString(bytes);
                        if(countDownTimerSupport.isStart()){
                            if(buffer.length() > 0){
                                buffer.append(swipingCard);
                            }
                        }else {
                            buffer.append(swipingCard);
                            countDownTimerSupport.reset();
                            countDownTimerSupport.start();
                        }
                    }

                    @Override
                    public void onDataSend(byte[] bytes) {

                    }
                });
                if(serialPortHelper.open()){
                    ToastUtils.showShort("串口开启成功");
                    LogUtils.d("串口开启成功");
                    whetherOpen = true;
                }else {
                    whetherOpen = false;
                    ToastUtils.showShort("串口开启失败");
                    LogUtils.d("串口开启失败");
                }
            }
        }
        return whetherOpen;
    }


    /**
     * 回调
     */
    public static AddOnClickListener mAddOnClickListener;
    public static void setAddOnClickListener(AddOnClickListener addOnClickListener){
        mAddOnClickListener = addOnClickListener;
    }

}
