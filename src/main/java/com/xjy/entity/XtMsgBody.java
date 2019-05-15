package com.xjy.entity;

import com.xjy.util.ConvertUtil;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @Author: Mr.Xu
 * @Date: Created in 9:30 2018/9/28
 * @Description: 新天协议（130）消息实体类,实际上新天通讯协议也就是中原油田协议是130的改版，这里使用标准的130协议。
 * 消息的类型由 控制域C AFN功能码 FN 3者唯一确定 文档有详细描述
 */
public class XtMsgBody {
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
    private int[] A1 = new int[2];
    private int[] A2 = new int[2];
    private int A3;
    /**
     * 链路用户数据域，分四个部分
     * 1. 应用层功能码 AFN（上述功能码的具体化）1字节
     * 2. 帧序列域 SEQ  1字节  D7-D0
     * D6:FIR  D5:FIN  00 多帧的中间帧 01 多帧的结束帧 10 多帧的第一帧 11单帧
     * 3. 数据单元标识 Fn  1字节 BIN 1-登录 3-心跳
     * 4. 数据单元(数据单元标识组织的数据)
     */
    private int AFN;//应用层功能码
    private int SEQ;//帧序列域
    private int[] Fn = new int[4];//数据单元标识
    private int[] data;//数据单元

    public XtMsgBody(int[] effectiveData) {
        if(effectiveData == null || effectiveData.length < 9) return;
        //初始化控制域
        C = effectiveData[0];
        //高低位转移,初始化地址域
        A1[0] = effectiveData[2];
        A1[1] = effectiveData[1];
        A2[0] = effectiveData[4];
        A2[1] = effectiveData[3];
        A3 = effectiveData[5];//标志组地址和单地址
        AFN = effectiveData[6];
        SEQ = effectiveData[7];
        Fn[0] = effectiveData[8];
        Fn[1] = effectiveData[9];
        Fn[2] = effectiveData[10];
        Fn[3] = effectiveData[11];
        if (effectiveData.length > 12) {
            data = new int[effectiveData.length - 12];
            for (int i = 0; i < data.length; i++) {
                data[i] = effectiveData[9 + i];
            }
        }
    }
    //将新天协议（130）的消息实体转为字节数组，发送命令前调用
    public byte[] toBytes(){

        return null;
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
