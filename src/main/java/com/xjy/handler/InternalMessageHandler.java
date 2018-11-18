package com.xjy.handler;

import com.xjy.entity.*;
import com.xjy.parms.CommandState;
import com.xjy.parms.Constants;
import com.xjy.parms.InternalMsgType;
import com.xjy.parms.InternalOrders;
import com.xjy.processor.InternalMsgProcessor;
import com.xjy.util.ConvertUtil;
import com.xjy.util.DBUtil;
import com.xjy.util.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import sun.rmi.runtime.Log;

import java.time.Duration;
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
            //LogUtil.DataMessageLog(msgBody.toString());
            String address = msgBody.getDeviceId();
            ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
            if(!map.containsKey(address)){
                Center center = new Center(address,ctx);
                map.put(address,center);
                LogUtil.DataMessageLog(InternalMessageHandler.class,"收到集中器"+address+"的首条消息！\r\n"+msgBody.toString());
                //更新数据库中集中器状态
                DBUtil.updateCenterState(1,center);
            }else{
                map.get(address).setCtx(ctx);
            }
            Center currentCenter = map.get(address);

            if(currentCenter.getHeartBeatTime() == null ){
                currentCenter.setHeartBeatTime(LocalDateTime.now());
                DBUtil.updateheartBeatTime(currentCenter);
            }else{
                Duration duration = Duration.between(currentCenter.getHeartBeatTime(),LocalDateTime.now());
                if(duration.toMinutes() > 4){
                    currentCenter.setHeartBeatTime(LocalDateTime.now());
                    DBUtil.updateheartBeatTime(currentCenter);
                }
            }
            if(msgBody.getMsgType() != InternalMsgType.HEARTBEAT_PACKAGE){//如果不是心跳包，进一步处理
                String instruction = msgBody.getInstruction().trim();
                LogUtil.DataMessageLog(InternalMessageHandler.class,"收到指令类型:" + instruction);
                if(instruction.equals(InternalOrders.READ)){ //读取指令,按页解析读数
                    InternalMsgProcessor.readProcessor(currentCenter,msgBody);
                }else if(instruction.equals(InternalOrders.COLLECT)){//采集指令，说明采集器已经开始采集
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"集中器已经开始采集！");
                }else if(instruction.equals(InternalOrders.DOWNLOAD)){
                    //下载档案命令的处理器，先读取页数，判断要不要写下一页。不需要的话命令成功结束
                    InternalMsgProcessor.writeProcessor(currentCenter,msgBody);
                }else if(instruction.equals(InternalOrders.SUCCESE)){
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"(采集)命令执行成功！");
                    currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
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
