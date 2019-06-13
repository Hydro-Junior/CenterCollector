package com.xjy.entity;

/**
 * @Author: Mr.Xu
 * @Date: Created in 9:38 2019/5/16
 * @Description:消息体接口，定义了转为字节流的方法
 */
public interface MsgBody {
    byte[] toBytes();
}
