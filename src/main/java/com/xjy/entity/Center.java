package com.xjy.entity;

import io.netty.channel.ChannelHandlerContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Author: Mr.Xu
 * @Date: Created in 14:31 2018/9/28
 * @Description: 集中器实体类
 */
public class Center {
    private String id; //集中器编号
    private List<Collector> collectors = new ArrayList<>();
    private LocalDateTime heartBeatTime; //心跳时间
    private LocalDateTime readTime;//最近一次读取（成功）时间
    private ChannelHandlerContext ctx; //绑定ChannelHandler的上下文对象，用于发送数据，检测数据流向
    private String enprNo ; //所属水司
    private Command curCommand; // 当前正在执行的命令
    private ConcurrentLinkedQueue<Command> commandQueue; //待执行的命令队列
    private ConcurrentLinkedDeque<Command> failedCommands; // 执行过且失败的命令<读表指令考虑重新执行>，每天写入日志并清空

    public Center(String id,ChannelHandlerContext ctx){
        this.id = id;
        this.ctx = ctx;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public List<Collector> getCollectors() {
        return collectors;
    }

    public void setCollectors(List<Collector> collectors) {
        this.collectors = collectors;
    }

    public LocalDateTime getHeartBeatTime() {
        return heartBeatTime;
    }

    public void setHeartBeatTime(LocalDateTime heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }

    @Override
    public String toString() {
        return "Center{" +
                "id='" + id + '\'' +
                ", collectors=" + collectors +
                ", heartBeatTime=" + heartBeatTime +
                ", ctx=" + ctx +
                ", enprNo='" + enprNo + '\'' +
                ", curCommand=" + curCommand +
                ", commandQueue=" + commandQueue +
                ", failedCommands=" + failedCommands +
                '}';
    }
}
