package com.xjy.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:27 2018/12/26
 * @Description: 自定义解码工具
 */
public class DecodeUtil {
    /**
     * 给定字节流和报文头，分别返回每个报文头所在的位置
     * @param bytes
     * @param head
     * @return
     */
    public static List<Integer> findHead(byte[] bytes, byte[] head){
        int len = head.length;
        ArrayList<Integer> res = new ArrayList<>();
        boolean match;
        for(int i = 0 ; i < bytes.length - len; i++){//比如内部协议 报文头0x7B 0x01 0x00 0x16 共4个字节
            match = true;
            for(int j = 0; j < len ; j++){
                if(bytes[i+j] != head[j]){
                    match = false;
                    break;
                }
            }
            if(match == true){
                res.add(i);
            }
        }
        return res;
    }
}
