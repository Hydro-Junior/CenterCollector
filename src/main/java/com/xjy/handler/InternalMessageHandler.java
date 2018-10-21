package com.xjy.handler;

import com.xjy.entity.*;
import com.xjy.parms.Constants;
import com.xjy.parms.InternalMsgType;
import com.xjy.parms.InternalOrders;
import com.xjy.processor.InternalMsgProcessor;
import com.xjy.util.ConvertUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:23 2018/9/27
 * @Description: 内部协议业务消息处理器
 */
public class InternalMessageHandler extends ChannelHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            InternalMsgBody msgBody = (InternalMsgBody)msg;
            String address = msgBody.getDeviceId();
            ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
            if(!map.containsKey(address)){
                map.put(address,new Center(address,ctx));
                //todo 更新数据库中集中器状态
            }else{
                map.get(address).setCtx(ctx);
            }
            Center currentCenter = map.get(address);
            if(currentCenter.getHeartBeatTime() == null || LocalDateTime.now().getMinute() - currentCenter.getHeartBeatTime().getMinute() > 3){
                currentCenter.setHeartBeatTime(LocalDateTime.now());
                //todo 更新数据库中集中器的心跳时间
            }
            if(msgBody.getMsgType() != InternalMsgType.HEARTBEAT_PACKAGE){//如果不是心跳包，进一步处理
                String instruction = msgBody.getInstruction().trim();
                System.out.println("操作类型:" + instruction);
                if(instruction.equals(InternalOrders.READ)){
                        //读取指令,按页解析读数
                        InternalMsgProcessor.readProcessor(currentCenter,msgBody);
                }
            }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("业务处理时异常");
        cause.printStackTrace();
        ctx.close();
    }
}
