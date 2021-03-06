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
    CommandType type= CommandType.NONE;//命令类型
    Integer id; //命令对应的数据库中的Id
    Integer state; //0-未分派 1-集中器未连接 2-等待执行 3-执行中 4-执行错误，重试中 5-执行成功  6-执行失败
    String[] args; //命令的相关参数
    LocalDateTime startExecuteTime; //开始执行命令的时刻
    LocalDateTime generateTime; //命令生成时间
    int secondsLimit = 5 * 60; //允许超时时间，默认5分钟
    boolean suspend = false; //命令在执行中是否中断
    int retryNum = 0; //重试次数
    Object parameter; //自定义参数
    int allowedRetryTimes = 1; //允许重试次数

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

    public boolean isSuspend() {
        return suspend;
    }

    public void setSuspend(boolean suspend) {
        this.suspend = suspend;
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

    public LocalDateTime getStartExecuteTime() {
        return startExecuteTime;
    }

    public void setStartExecuteTime(LocalDateTime startExecuteTime) {
        this.startExecuteTime = startExecuteTime;
    }

    public LocalDateTime getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(LocalDateTime generateTime) {
        this.generateTime = generateTime;
    }

    public int getSecondsLimit() {
        return secondsLimit;
    }

    public void setSecondsLimit(int secondsLimit) {
        this.secondsLimit = secondsLimit;
    }

    public int getRetryNum() {
        return retryNum;
    }

    public void setRetryNum(int retryNum) {
        this.retryNum = retryNum;
    }

    public int getAllowedRetryTimes() {
        return allowedRetryTimes;
    }

    public void setAllowedRetryTimes(int allowedRetryTimes) {
        this.allowedRetryTimes = allowedRetryTimes;
    }


    public Object getParameter() {
        return parameter;
    }

    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", id=" + id +
                ", state=" + state +
                ", args=" + Arrays.toString(args) +
                ", startExecuteTime=" + startExecuteTime +
                ", generateTime=" + generateTime +
                ", secondsLimit=" + secondsLimit +
                ", suspend=" + suspend +
                ", retryNum=" + retryNum +
                ", parameter=" + parameter +
                ", allowedRetryTimes=" + allowedRetryTimes +
                '}';
    }
}
