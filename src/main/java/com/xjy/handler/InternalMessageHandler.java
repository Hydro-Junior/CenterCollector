package com.xjy.handler;

import com.xjy.entity.Center;
import com.xjy.entity.GlobalMap;
import com.xjy.entity.InternalMsgBody;
import com.xjy.entity.XtMsgBody;
import com.xjy.parms.InternalMsgType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:23 2018/9/27
 * @Description: 内部协议业务消息处理器
 */
public class InternalMessageHandler extends ChannelHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InternalMsgBody msgBody = (InternalMsgBody)msg;
        String addr = msgBody.getDeviceId();
        ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
        if(!map.containsKey(addr)){
            map.put(addr,new Center(addr,ctx));
        }else{
            map.get(addr).setCtx(ctx);
        }
        System.out.println("得到消息实体：" + msgBody);
        System.out.println("当前在线集中器：");
        for(Map.Entry<String,Center> entry : map.entrySet()){
            ChannelHandlerContext channelCtx = entry.getValue().getCtx();
            if(channelCtx.channel().isActive()){
                System.out.println(entry.getKey()+ ":" + entry.getValue());
            }
        }
        if(msgBody.getMsgType() != InternalMsgType.HEARTBEAT_PACKAGE){//如果不是心跳包，进一步处理
            System.out.println("收到数据包！");
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
