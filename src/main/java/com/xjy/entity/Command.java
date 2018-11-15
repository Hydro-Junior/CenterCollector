package com.xjy.entity;

import com.xjy.parms.CommandType;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:59 2018/10/17
 * @Description: 与命令相关的实体类
 */
public class Command {
    CommandType type;
    Integer id; //命令对应的数据库中的Id
    Integer state; //0-未分派 1-集中器未连接 2-等待执行 3-执行中 4-执行错误，重试中 5-执行成功  6-执行失败
    String[] args; //命令的相关参数
    LocalDateTime startExcuteTime; //开始执行命令的时刻
    LocalDateTime generateTime;
    int minitesLimit = 5; //允许超时时间，默认5分钟

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public LocalDateTime getStartExcuteTime() {
        return startExcuteTime;
    }

    public void setStartExcuteTime(LocalDateTime startExcuteTime) {
        this.startExcuteTime = startExcuteTime;
    }

    public LocalDateTime getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(LocalDateTime generateTime) {
        this.generateTime = generateTime;
    }

    public int getMinitesLimit() {
        return minitesLimit;
    }

    public void setMinitesLimit(int minitesLimit) {
        this.minitesLimit = minitesLimit;
    }

    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", state=" + state +
                ", args=" + Arrays.toString(args) +
                ", startExcuteTime=" + startExcuteTime +
                '}';
    }
}
