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
}
