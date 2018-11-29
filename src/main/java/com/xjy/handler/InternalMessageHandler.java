package com.xjy.handler;

import com.xjy.entity.*;
import com.xjy.parms.*;
import com.xjy.processor.InternalMsgProcessor;
import com.xjy.util.ConvertUtil;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
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
                }
                else if(instruction.equals(InternalOrders.COLLECT)){//采集指令，说明采集器已经开始采集
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"集中器已经开始采集！");
                }
                else if(instruction.equals(InternalOrders.DOWNLOAD)){
                    //下载档案命令的处理器，先读取页数，判断要不要写下一页。不需要的话命令成功结束
                    InternalMsgProcessor.writeProcessor(currentCenter,msgBody);
                }
                else if(instruction.equals(InternalOrders.CLOCK)){//设备校时返回，设置命令成功，并设置定时采集
                    currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
                    InternalMsgProcessor.setTimingCollect(currentCenter);
                }
                else if(instruction.equals(InternalOrders.SUCCESE)){//（采集）命令执行成功
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"(采集)命令执行成功！");
                    currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
                }
                else if(instruction.equals(InternalOrders.OPEN_CHANNEL)){//打开通道成功（可能是开阀或关阀）
                    if(currentCenter.getCurCommand().getType() == CommandType.OPEN_VALVE){
                        LogUtil.DataMessageLog(InternalMessageHandler.class,"发送开阀指令！");
                        InternalProtocolSendHelper.openValve(currentCenter);
                    }else if(currentCenter.getCurCommand().getType() == CommandType.CLOSE_VALVE){
                        LogUtil.DataMessageLog(InternalMessageHandler.class,"发送关阀指令！");
                        InternalProtocolSendHelper.closeValve(currentCenter);
                    }
                }
                else if(instruction.equals(InternalOrders.OPCHANNEL_FAILED)){//打开节点失败
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"打开节点失败！");
                    //周全起见，关闭阀控节点
                    InternalProtocolSendHelper.closeChannel(currentCenter,currentCenter.getCurCommand());
                    currentCenter.getCurCommand().setState(CommandState.FAILED);
                    DBUtil.updateCommandState(CommandState.FAILED,currentCenter);
                }
                else if(instruction.equals(InternalOrders.OPEN_VALVE) || instruction.equals(InternalOrders.CLOSE_VALVE)){//开关阀返回
                    System.out.println("开关阀返回！");
                    InternalMsgProcessor.getValveInfo(currentCenter,msgBody);//获取开关阀信息
                    InternalProtocolSendHelper.closeChannel(currentCenter,currentCenter.getCurCommand());
                }
                else if(instruction.equals(InternalOrders.BEFORE_CLOSE)){//开关阀后读节点表返回
                    InternalMsgProcessor.getValveInfo(currentCenter,msgBody);//获取开关阀信息
                    InternalProtocolSendHelper.closeChannel(currentCenter,currentCenter.getCurCommand());
                }
                else if(instruction.equals(InternalOrders.CLOSE_CHANNEL)){//关通道，写入日志即可
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"开关阀后关闭通道");
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
