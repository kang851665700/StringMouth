package com.liyujie.stringmouth;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.io.File;
import java.nio.ByteOrder;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import me.f1reking.serialportlib.SerialPortHelper;
import me.f1reking.serialportlib.entity.DATAB;
import me.f1reking.serialportlib.entity.FLOWCON;
import me.f1reking.serialportlib.entity.PARITY;
import me.f1reking.serialportlib.entity.STOPB;
import me.f1reking.serialportlib.listener.IOpenSerialPortListener;
import me.f1reking.serialportlib.listener.ISerialPortDataListener;
import me.f1reking.serialportlib.listener.Status;

/**
 * 开门串口
 */
public class OpenPortUtils {
    private static SerialPortHelper serialPortHelper;
    private static  final Semaphore semaphore=new Semaphore(0,true);
    private static byte[] receiveBuff=null;

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
    }

    /**
     * 开启串口
     * @param port 串口号
     * @return
     */
    public static boolean portSize(String port){
        boolean whetherOpen = false;
        //打开串口
        if(null != serialPortHelper){
            if(serialPortHelper.isOpen()){
                ToastUtils.showShort("串口已经开启成功");
                whetherOpen = true;
                LogUtils.d("串口已经开启成功");
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
                        receiveBuff=new byte[bytes.length];
                        System.arraycopy(bytes,0,receiveBuff,0,bytes.length);
                        LogUtils.d(bytes.length +"长度");
                        semaphore.release();
                    }

                    @Override
                    public void onDataSend(byte[] bytes) {

                    }
                });
                if(serialPortHelper.open()){
                    whetherOpen = true;
                    ToastUtils.showShort("串口开启成功");
                }else {
                    whetherOpen = false;
                    ToastUtils.showShort("串口开启失败");
                }
            }
        }
        return whetherOpen;
    }



    /**
     * 获得所有串口设备的地址
     * @return
     */
    public static String[] getAllDeicesPath(){
        if (serialPortHelper == null) {
            return serialPortHelper.getAllDeicesPath();
        }else {
            return serialPortHelper.getAllDeicesPath();
        }
    }




    /**
     * 1000门的
     * @param event
     * @return
     * @throws InterruptedException
     */
    public static boolean openDoor(int event) throws InterruptedException {
        boolean res = true;
        byte[] sendContentBytes = new byte[7];
        sendContentBytes[0] = 1; //消息方向
        sendContentBytes[1] = 0;//命令
        sendContentBytes[2] = 1; //桢序列
        sendContentBytes[3] = 2;////数据长度
        byte[] information = BitConverterUtils.getBytes16(event);
        sendContentBytes[4] = information[0]; //消息
        sendContentBytes[5] = information[1]; //消息
        sendContentBytes[6] =(byte)( 0 - sendContentBytes[0] - sendContentBytes[1] - sendContentBytes[2] - sendContentBytes[3] - sendContentBytes[4] - sendContentBytes[5]); //校验
        int cnt = 1;  //定义重发次数
        if (serialPortHelper != null) {
            do {
                receiveBuff=null;
                while(semaphore.tryAcquire());
                //发送开门命令
                if(serialPortHelper.sendBytes(sendContentBytes)){
                    LogUtils.d("Tx", CommonUtils.bytesToHexString(sendContentBytes));
                    //从queue中获取串口收到的数据
                    if(semaphore.tryAcquire(800, TimeUnit.MILLISECONDS)) {
                        LogUtils.d("wait success", CommonUtils.bytesToHexString(receiveBuff));
                        if (receiveBuff != null) {
                            //判断校验合
                            byte sum = 0;
                            if (sum == 0  && receiveBuff.length == 10) {
                                res=true;
                                break;
                            } else {
                                LogUtils.d("sum not 0 value or length not 7", "");
                                semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);//收到消息不对，延时300ms重发
                            }
                        } else{
                            LogUtils.d("receiveBuffBuff is null", "");
                            semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);//收到消息不对，延时300ms重发
                        }
                    } else {
                        LogUtils.d("wait semaphore time out","");
                    }
                }else {
                    break;
                }
                //否则重发数据
                cnt--;
            }while (cnt > 0);
        }
        LogUtils.d("EndOpenDoor", CommonUtils.bytesToHexString(sendContentBytes));
        return  res;
    }


    /**
     * 普通开门接口
     * @param event
     * @return
     * @throws InterruptedException
     */
    public static boolean openDoor1(byte event) throws InterruptedException {
        boolean res = true;
        LogUtils.d("OpenDoor Start", "");
        byte[] sendContentBytes = new byte[6];
        sendContentBytes[0] = event; //设备地址
        sendContentBytes[1] = 0;//命令
        sendContentBytes[2] = 1; //桢序列
        sendContentBytes[3] = 1;////数据长度
        sendContentBytes[4] = event; //数据在和
        sendContentBytes[5] =(byte)( 0 - sendContentBytes[0] - sendContentBytes[1] - sendContentBytes[2] - sendContentBytes[3] - sendContentBytes[4]); //校验
        int cnt = 1;  //定义重发次数
        if (serialPortHelper != null) {
            do {
                receiveBuff=null;
                while(semaphore.tryAcquire());
                //发送开门命令
                if(serialPortHelper.sendBytes(sendContentBytes)){
                    LogUtils.d("Tx", CommonUtils.bytesToHexString(sendContentBytes));
                    //从queue中获取串口收到的数据
                    if(semaphore.tryAcquire(800, TimeUnit.MILLISECONDS)) {
                        LogUtils.d("wait success", CommonUtils.bytesToHexString(receiveBuff));
                        if (receiveBuff != null) {
                            //判断校验合
                            byte sum = 0;
                            if ((sum == 0  && receiveBuff.length == 7)||(receiveBuff[0] == event && receiveBuff.length == 1)) {
                                res=true;
                                break;
                            } else {
                                LogUtils.d("sum not 0 value or length not 7", "");
                                semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);//收到消息不对，延时300ms重发
                            }
                        } else{
                            LogUtils.d("receiveBuffBuff is null", "");
                            semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);//收到消息不对，延时300ms重发
                        }
                    } else {
                        LogUtils.d("wait semaphore time out","");
                    }
                }else {
                    break;
                }
                //否则重发数据
                cnt--;
            }while (cnt > 0);
        }
        LogUtils.d("EndOpenDoor", CommonUtils.bytesToHexString(sendContentBytes));
        return  res;
    }



    /**
     * 设置锁板
     * @param door
     * @param cmd 0开门  1是开始编号  2是结束编号 3 查询门状态
     */
    public static boolean sendData(Byte door, String cmd){
        boolean Senddata = false;
        byte[] sendContentBytes = new byte[6];
        sendContentBytes[0] = door; //设备地址
        sendContentBytes[1] =  Byte.valueOf(cmd);//命令
        sendContentBytes[2] = 1; //桢序列
        sendContentBytes[3] = 1;////数据长度
        sendContentBytes[4] =  door; //数据在和
        sendContentBytes[5] =(byte)( 0 - sendContentBytes[0] - sendContentBytes[1] - sendContentBytes[2] - sendContentBytes[3] - sendContentBytes[4]); //校验
        if (serialPortHelper != null) {
            Senddata = serialPortHelper.sendBytes(sendContentBytes);
        }
        return Senddata;
    }


}
