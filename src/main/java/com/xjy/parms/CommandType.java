package com.xjy.parms;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:00 2018/10/17
 * @Description:
 */
public enum CommandType {
    NONE, //无命令
    READ_SINGLE_METER, //读取单个表
    READ_ALL_METERS, //读取所有表
    COLLECT_FOR_METER,//采集单个表
    COLLECT_FOR_COLLECTOR,//针对采集器采集
    COLLECT_FOR_CENTER, //针对集中器采集
    OPEN_VALVE, //开阀
    CLOSE_VALVE,//关阀
    WRITE_INFO //写入资料到集中器
}
