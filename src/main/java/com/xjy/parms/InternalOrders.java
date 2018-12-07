package com.xjy.parms;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:16 2018/9/29
 * @Description:内部协议指令字集合
 *
 */
public final class InternalOrders {
    public static final String READ = "RRR"; //读取 0x52
    public static final String D_READ = "DDD";//读取当前表
    public static final String COLLECT = "MMM"; //采集 0x4D
    public static final String CHANGE = "ccc"; //换表
    public static final String ADD = "aaa"; //添加表
    public static final String DELETE = "ddd"; //删除表
    public static final String CLOCK = "TTT"; //设置时钟 0x54
    public static final String SCHEDUEL = "PPP";//设置定时采集 0x50
    public static final String DOWNLOAD = "WWW";//下载编号列表 0x57
    public static final String SUCCESE ="UUU";//成功返回 0x55
    public static final String MODEM = "AAA";//配置集中器的目的IP和端口 0x41

    //有线表开关阀流程： 开通道 -> 开关阀 -> 集中器返回DDD数据 -> 关通道
    public static final String OPEN_CHANNEL = "OOO";//打开采集通道 0x4F
    public static final String CLOSE_CHANNEL = "CCC";//关闭采集通道 0x43
    public static final String OPEN_VALVE = "KKK";//开阀指令 0x4B
    public static final String CLOSE_VALVE = "GGG";//关阀指令 0x47
    public static final String BEFORE_CLOSE = "DDD";//集中器发送的读节点表命令
    public static final String OPCHANNEL_FAILED = "EEE";//打开通道失败

}
