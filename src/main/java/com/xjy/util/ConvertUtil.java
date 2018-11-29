package com.xjy.util;

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
}
