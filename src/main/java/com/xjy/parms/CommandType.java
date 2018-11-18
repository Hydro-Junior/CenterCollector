package com.xjy.parms;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:00 2018/10/17
 * @Description:
 */
public enum CommandType {
    NONE(""), //无命令
    READ_SINGLE_METER("000106"), //读取单个表
    READ_ALL_METERS("000102"), //读取所有表
    READ_CENTER_INFO("000103"),//读取集中器信息
    COLLECT_FOR_METER("000105"),//采集单个表
    COLLECT_FOR_COLLECTOR("待定"),//针对采集器采集
    COLLECT_FOR_CENTER("000101"), //针对集中器采集
    OPEN_VALVE("000302"), //开阀
    CLOSE_VALVE("000304"),//关阀
    OPEN_VALVE_BATCH("000301"),//批量开阀
    CLOSE_VALVE_BATCH("000303"),//批量关阀
    WRITE_INFO("000208") ; //写入资料到集中器
    private String value;
    private CommandType(String value){
        this.value = value;
    }
    public String getValue(){
        return value;
    }
}
