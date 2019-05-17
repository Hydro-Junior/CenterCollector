package com.xjy.entity;

import com.xjy.parms.Constants;
import com.xjy.util.CheckUtil;
import com.xjy.util.ConvertUtil;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @Author: Mr.Xu
 * @Date: Created in 9:30 2018/9/28
 * @Description: 新天协议（130）消息实体类,实际上新天通讯协议也就是中原油田协议是130的改版，这里使用标准的130协议。
 * 消息的类型由 控制域C AFN功能码 FN 3者唯一确定 文档有详细描述
 */
public class XtMsgBody implements MsgBody{
    /**
     * 控制码，一个字节
     * D7 传输方向 0-主站（服务器）发出的下行报文 1-终端(集中器)发出的上行报文
     * D6 启动标志位 1-发起方（请求） 0-从动方（响应）
     * D3-D0 为功能码 根据是请求还是响应有不同的功能码（详见文档）
     * D4-D5 保留
     */
    private int C;
    /**
     * 地址域，共5个字节
     * 行政区划码A1（BCD） 2字节 ，终端地址A2（BIN） 2字节，主站地址和组地址标志A3（BIN）1字节
     * 3者合一为集中器地址（A3标志组地址一般用不到，所以地址范围 4位行政区码 拼上 0到65535，共9位）
     */
    private int[] A = new int[5]; //整体的A的内容，在发送时用到
    private int[] A1 = new int[2];
    private int[] A2 = new int[2];
    private int A3;
    /**
     * 链路用户数据域，分四个部分
     * 1. 应用层功能码 AFN（上述功能码的具体化）1字节
     * 2. 帧序列域 SEQ  1字节  D7-D0
     * D6:FIR  D5:FIN  00 多帧的中间帧 01 多帧的结束帧 10 多帧的第一帧 11单帧
     * 3. 数据单元标识 Fn  4字节 BIN 1-登录 3-心跳
     * 4. 数据单元(数据单元标识组织的数据)
     */
    private int AFN;//应用层功能码
    private int SEQ;//帧序列域
    private int[] Fn = new int[4];//数据单元标识
    private int[] data;//数据单元

    //用于解析收到字节数组的构造方法
    public XtMsgBody(int[] effectiveData) {
        if(effectiveData == null || effectiveData.length < 12) return;
        //初始化控制域
        C = effectiveData[0];
        //高低位转移,初始化地址域
        A1[0] = effectiveData[2]; A[0] = effectiveData[1];
        A1[1] = effectiveData[1]; A[1] = effectiveData[2];
        A2[0] = effectiveData[4]; A[2] = effectiveData[3];
        A2[1] = effectiveData[3]; A[3] = effectiveData[4];
        A3 = effectiveData[5];//标志组地址和单地址
        AFN = effectiveData[6];
        SEQ = effectiveData[7];
        Fn[0] = effectiveData[8];
        Fn[1] = effectiveData[9];
        Fn[2] = effectiveData[10];
        Fn[3] = effectiveData[11];
        if (effectiveData.length >= 12) {
            data = new int[effectiveData.length - 12];
            for (int i = 0; i < data.length; i++) {
                data[i] = effectiveData[12 + i];
            }
        }
    }
    //构造待发送命令时使用此构造方法
    public XtMsgBody(int c, int[] a, int afn, int seq, int[] fn, int[] data) {
        C = c; A = a;

        A1= new int[2]; A2 = new int[2];
        A1[0] = A[1]; A1[1] = A[0]; A2[0] = A[3]; A2[1] = A[2]; //这两行可写可不写，为数据一致性还是给A1，A2分别赋值

        AFN = afn;
        SEQ = seq;
        Fn = fn;
        this.data = data;
    }

    //将新天协议（130）的消息实体转为字节数组，发送命令前调用
    public byte[] toBytes(){
        /**
         *  6 表示报文头 0x68 L(2字节) L(2字节) 0x68
         *  12 表示控制域1字节 + 地址域5字节 + 功能码AFN 1字节 + 序列域SEQ 1字节 + 数据单元表示 4字节
         *  data.length 表示数据单元部分长度
         *  2 校验和1字节 + 报文尾0x16
         */
        if(data == null) data = new int[0];
        int[] msg = new int[6 + 12 + data.length + 2];
        // 根据协议，L 表示12 + data.length左移2位，最后两位为0和1
        int L = ((12 + data.length)<< 2) | 0x01;
        msg[0] = Constants.HEADOf_130; msg[5] = msg[0];
        msg[1] = L & 0xff;  msg[3] = msg[1];//低8位
        msg[2] = (L >>> 8) & 0xff; msg[4] = msg[2]; //高8位
        msg[6] = C;
        for(int i = 0; i < 5; i++) msg[7 + i] = A[i];
        msg[12] = AFN;
        msg[13] = SEQ;
        for(int i = 0 ; i < 4; i++) msg[14+i] = Fn[i];
        for(int i = 0 ; i < data.length; i++){
            msg[18 + i] = data[i];
        }
        int checkSum = 0;
        for(int i = 6; i < msg.length - 2; i++){
            checkSum += msg[i];
            checkSum &= 0xff;
        }
        msg[msg.length-2] = checkSum;
        msg[msg.length-1] = Constants.TAILOf_130;
        byte[] res = new byte[msg.length];
        for(int i = 0 ; i < msg.length ;i ++){
            res[i] = (byte)msg[i];
        }
        return res;
    }
    public String getCenterAddress() {
        return ConvertUtil.fixedLengthHex(A1[0])  + ConvertUtil.fixedLengthHex(A1[1])
                + String.format("%05d", (A2[0] << 8 | A2[1]));
    }
    public XtControlArea getControlArea(){
        if(C == 0) return null;
        return new XtControlArea(C);
    }
    @Override
    public String toString() {
        return "XtMsgBody{" +
                "C=" + ConvertUtil.fixedLengthHex(C) +
                ", A1=" + ConvertUtil.fixedLengthHex(A1[0])  + ConvertUtil.fixedLengthHex(A1[1]) +
                ", A2=" + Arrays.toString(A2) +
                ", A3=" + ConvertUtil.fixedLengthHex(A3) +
                ", AFN=" + ConvertUtil.fixedLengthHex(AFN) +
                ", SEQ=" + ConvertUtil.fixedLengthHex(SEQ) +
                ", Fn=" + ConvertUtil.fixedLengthHex(Fn)+
                ", data=" + ConvertUtil.fixedLengthHex(data) +
                '}'+ "  centerAddr:" + getCenterAddress();
    }

    public int getC() {
        return C;
    }

    public void setC(int c) {
        C = c;
    }

    public int[] getA1() {
        return A1;
    }

    public void setA1(int[] a1) {
        A1 = a1;
    }

    public int[] getA2() {
        return A2;
    }

    public void setA2(int[] a2) {
        A2 = a2;
    }

    public int getA3() {
        return A3;
    }

    public void setA3(int a3) {
        A3 = a3;
    }

    public int getAFN() {
        return AFN;
    }

    public void setAFN(int AFN) {
        this.AFN = AFN;
    }

    public int getSEQ() {
        return SEQ;
    }

    public void setSEQ(int SEQ) {
        this.SEQ = SEQ;
    }

    public int[] getFn() {
        return Fn;
    }

    public void setFn(int[] fn) {
        Fn = fn;
    }

    public int[] getData() {
        return data;
    }

    public void setData(int[] data) {
        this.data = data;
    }
}
