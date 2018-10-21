package com.xjy.parms;

/**
 * @Author: Mr.Xu
 * @Date: Created in 8:52 2018/10/18
 * @Description:
 */
public class CommandState {
    public static final int UN_ENQUEUED = 0; //命令写入数据库待分派
    public static final int DISCONNECTED = 1; //集中器未连接无法执行命令
    public static final int WAITING_IN_QUEUE = 2; //命令在等待队列中
    public static final int EXECUTING = 3;//命令正在执行
    public static final int RETRYING = 4;//命令首次执行遇到问题，重试中
    public static final int SUCCESSED = 5; //命令执行成功
    public static final int FAILED = 6; //命令执行失败
}
