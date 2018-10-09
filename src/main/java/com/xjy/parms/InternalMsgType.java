package com.xjy.parms;

/**
 * @Author: Mr.Xu
 * @Date: Created in 9:31 2018/9/29
 * @Description:内部协议的几种消息类型
 */
public enum InternalMsgType {
    INVALID_PACKAGE,//无效包
    HEARTBEAT_PACKAGE, //心跳包
    SEND_PACKAGE,//发送数据包
    RECV_PACKAGE //接收数据包
}
