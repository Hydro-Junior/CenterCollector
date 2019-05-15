package com.xjy.handler;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.entity.XtMsgBody;
import com.xjy.parms.CommandState;
import com.xjy.pojo.DBCommand;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
        String address = msgBody.getCenterAddress();
        ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
        if(!map.containsKey(address)){
            Center center = new Center(address,ctx);
            map.put(address,center);
            LogUtil.DataMessageLog(InternalMessageHandler.class,"首次收到集中器"+address+"的消息！\r\n"+msgBody.toString());
            //更新数据库中集中器状态
            DBUtil.updateCenterState(1,center);
            //首次上线，如果有命令正在执行，将其置为成功！(有些硬件的特殊情况：执行采集命令会断开连接，结束后上线)
            List<DBCommand> dbCommands = DBUtil.getCommandByCenterAddressAndState(address, CommandState.EXECUTING);
            for(DBCommand dbCommand : dbCommands){
                DBUtil.updateCommandState(CommandState.SUCCESSED, dbCommand.getId());
            }
        }else{
            Center center = map.get(address);
            center.setCtx(ctx);
            if(center.getCurCommand() != null && center.getCurCommand().isSuspend()){//在执行命令时掉线
                if(center.getLatestMsg() != null &&center.getCurCommand().getState()==CommandState.EXECUTING ){
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"集中器"+center.getId()+"有缓存消息待发送！");
                    //发送缓存消息(两种情况下会重试：1. 断开连接后重新收到心跳（此处） 2. 命令超时（见CommandExecutor）)
                    Command cur = center.getCurCommand();
                    int retry = cur.getRetryNum();
                    if(retry < cur.getAllowedRetryTimes()){
                        //主动重发上一条命令
                        InternalProtocolSendHelper.writeAndFlush(center,center.getLatestMsg());
                        cur.setStartExcuteTime(LocalDateTime.now()); //更新开始时间
                        cur.setRetryNum(retry + 1); //重试次数加一
                    }
                }
                center.getCurCommand().setSuspend(false);
            }
        }
        Center currentCenter = map.get(address);
        if(currentCenter.getHeartBeatTime() == null){
            currentCenter.setHeartBeatTime(LocalDateTime.now());
            DBUtil.updateheartBeatTime(currentCenter);
        }else{
            Duration duration = Duration.between(currentCenter.getHeartBeatTime(),LocalDateTime.now());
            if(duration.toMinutes() > 2){
                //由于web后台程序的干预，时间超过一定值不更新心跳，会将集中器设为不在线，因此添加下面的语句，
                //防止在线的集中器网页显示却是不在线！
                currentCenter.setHeartBeatTime(LocalDateTime.now());
                DBUtil.updateheartBeatTime(currentCenter);
                DBUtil.updateCenterState(1,currentCenter);
            }
        }
        //接下来，根据不同的命令类型，对消息进行处理

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
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
