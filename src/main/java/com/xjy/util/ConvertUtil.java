package com.xjy.util;

import com.xjy.entity.InternalMsgBody;

import java.net.SocketAddress;

/**
 * @Author: Mr.Xu
 * @Date: Created in 19:29 2018/9/27
 * @Description:
 */
public class ConvertUtil {
    public static int[] bytesToInts(byte[] bytes){
        if(bytes == null || bytes.length == 0) return null;
        int[] data = new int[bytes.length];
        for(int i = 0 ; i < bytes.length; i++){
            data[i] = bytes[i] & 0xff;
        }
        return data;
    }
    //得到定长（默认为2）的16进制数的表示
    public static String fixedLengthHex(int a){
        String s = Integer.toHexString(a);
        while(s.length() % 2 != 0){
            s = "0"+ s;
        }
        return s;
    }
    //获得一个整数（不超过两位）的BCD码
    public static int  getBcdOf2digit(int num){
       return (((num / 10) << 4) & 0xf0) |((num % 10) & 0x0f);
    }
    public static String fixedLengthHex(int[] a){
        if(a == null || a.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < a.length; i++){
            sb.append(fixedLengthHex(a[i])+ " ");
        }
        sb.append("\r\n");
        return sb.toString();
    }
    //把表地址或采集器地址转化为字节数组（内部协议）,返回类型为int[]是为了表示方便
    public static int[] addressToBytes(String address){
        String addr = address.trim();
        while(addr.length() != 12){
            addr = '0' + addr;
        }
        int[] res = new int[6];
        for(int i = 0 ;i < 12; i+=2){
            int idx = (12 - i) / 2;
            res[idx-1] = Integer.valueOf(addr.substring(i,i+1));
            res[idx-1] <<= 4;
            res[idx-1] += Integer.valueOf(addr.substring(i+1,i+2));
        }
        return res;
    }
    public static String getIP(String remoteAddr){
        String remoteIP = remoteAddr.substring(1,remoteAddr.indexOf(":"));
        return  remoteIP;
    }

    public static String getCollectAddressFromMsgBody(InternalMsgBody msgBody) {
        int[] effectiveData = msgBody.getEffectiveBytes();
        int[] collectorBytes = new int[6];
        for(int i = 0; i < 6; i++){
            collectorBytes[5-i] = effectiveData[i+3];
        }
        StringBuffer collectorAddress = new StringBuffer();
        for(int i = 0 ; i < 6 ; i++){
            collectorAddress.append(ConvertUtil.fixedLengthHex(collectorBytes[i]));
        }
        return collectorAddress.toString();
    }

    public static String getMeterAddressFromMsgBody(InternalMsgBody msgBody) {
        int[] effectiveData = msgBody.getEffectiveBytes();
        int[] collectorBytes = new int[6];
        for(int i = 0; i < 6; i++){
            collectorBytes[5-i] = effectiveData[i+9];
        }
        StringBuffer collectorAddress = new StringBuffer();
        for(int i = 0 ; i < 6 ; i++){
            collectorAddress.append(ConvertUtil.fixedLengthHex(collectorBytes[i]));
        }
        return collectorAddress.toString();
    }
}
