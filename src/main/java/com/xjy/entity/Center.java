package com.xjy.entity;

import io.netty.channel.ChannelHandlerContext;

/**
 * @Author: Mr.Xu
 * @Date: Created in 14:31 2018/9/28
 * @Description: 集中器实体类
 */
public class Center {
    String id; //集中器编号
    ChannelHandlerContext ctx; //绑定ChannelHandler的上下文对象，用于发送数据，检测数据流向

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

    @Override
    public String toString() {
        return "Center{" +
                "id='" + id + '\'' +
                ", ctx=" + ctx+
                '}';
    }
}
