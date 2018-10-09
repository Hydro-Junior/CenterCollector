package com.xjy.handler;

import com.xjy.entity.Center;
import com.xjy.entity.GlobalMap;
import com.xjy.entity.XtMsgBody;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:03 2018/9/27
 * @Description: 新天通讯协议（130）业务消息处理器
 */
public class XtMessageHandler extends ChannelHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        XtMsgBody msgBody = (XtMsgBody)msg;
        String addr = msgBody.getCenterAddr();
        ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
        if(!map.containsKey(addr)){
            map.put(addr,new Center(addr,ctx));
        }else{
            map.get(addr).setCtx(ctx);
        }
        System.out.println("得到消息实体：" + msgBody);
        System.out.println("当前在线集中器：");
        for(Map.Entry<String,Center> entry : map.entrySet()){
            if(entry.getValue().getCtx().channel().isActive())
                System.out.println(entry.getKey()+ ":" + entry.getValue());
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
