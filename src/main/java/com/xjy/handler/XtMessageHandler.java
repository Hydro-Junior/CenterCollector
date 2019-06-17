package com.xjy.handler;

import com.xjy.entity.*;
import com.xjy.parms.CommandState;
import com.xjy.parms.CommandType;
import com.xjy.parms.Constants;
import com.xjy.parms.XTParams;
import com.xjy.pojo.DBCommand;
import com.xjy.processor.ExceptionProcessor;
import com.xjy.processor.XtMsgProcessor;
import com.xjy.util.ConvertUtil;
import com.xjy.util.DBUtil;
import com.xjy.sender.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;
import com.xjy.sender.XTProtocolSendHelper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:03 2018/9/27
 * @Description: 130协议业务消息处理器
 */
public class XtMessageHandler extends ChannelHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        XtMsgBody msgBody = (XtMsgBody)msg;
        String address = msgBody.getCenterAddress();
        ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
        //LogUtil.DataMessageLog(InternalMessageHandler.class,"来自集中器"+address+"的消息！\r\n"+msgBody.toString());
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
                        XTProtocolSendHelper.writeAndFlush(center,(XtMsgBody) center.getLatestMsg());
                        cur.setStartExecuteTime(LocalDateTime.now()); //更新开始时间
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
        int AFN = msgBody.getAFN();
        Command command = currentCenter.getCurCommand();
        if(AFN == XTParams.AFN_LINK_DETECTION){
            XTProtocolSendHelper.replyHeartBeat(currentCenter);
        } else if(AFN == XTParams.AFN_GET_REALTIME_DATA){ //请求1类数据报文
            if(command.getType() == CommandType.READ_ALL_METERS){
                List<MeterOf130> meters = XTProtocolSendHelper.constructAndGetMetersInfo(currentCenter);
                XtMsgProcessor.readProcessor(currentCenter,msgBody);
                if(meters.size() <= (Integer) command.getParameter()) {
                    command.setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED, currentCenter);
                    DBUtil.updateCenterReadTime(currentCenter);
                }else{
                    XTProtocolSendHelper.readMeters(currentCenter,command);
                }
            }else if(command.getType() == CommandType.READ_SINGLE_METER){
                XtMsgProcessor.readProcessor(currentCenter,msgBody);
                command.setState(CommandState.SUCCESSED);
                DBUtil.updateCommandState(CommandState.SUCCESSED, currentCenter);
            }
        }else if(AFN == XTParams.AFN_CONFIRM_OR_DENY){//设置参数报文（下载档案到集中器）
            if(command.getType() == CommandType.WRITE_INFO ){//下载档案回复处理
                if(msgBody.getC() == 0x80){
                    LogUtil.DataMessageLog(XtMessageHandler.class,"下载档案回复");
                    //TimeUnit.SECONDS.sleep(1);
                    List<MeterOf130> meters = XTProtocolSendHelper.constructAndGetMetersInfo(currentCenter);
                    if((Integer)command.getParameter() >= meters.size()) {
                        command.setState(CommandState.SUCCESSED);
                        DBUtil.updateCommandState(CommandState.SUCCESSED, currentCenter);
                    }else{
                        XTProtocolSendHelper.writeFileInfo(currentCenter,command);
                    }
                }else if(msgBody.getC() == 0x89){
                    command.setState(CommandState.FAILED);
                    DBUtil.updateCommandState(CommandState.FAILED, currentCenter);
                }
            }
        }else if(AFN == XTParams.AFN_GET_PARAM){//获取参数报文（如读取集中器档案）
            if(command.getType() == CommandType.READ_CENTER_INFO){
                List<MeterOf130> meters = XTProtocolSendHelper.constructAndGetMetersInfo(currentCenter);
                //读取集中器信息的方式暂时就是写到日志，不做具体解析
                arrangeFileInfo(currentCenter,command,msgBody); // 整理读取的集中器信息，将其打印到日志
                if((Integer)command.getParameter() >= meters.size()) {
                    command.setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED, currentCenter);
                }else{
                    XTProtocolSendHelper.getFileInfo(currentCenter,command);
                }
            }
        }
    }

    private void arrangeFileInfo(Center currentCenter, Command command, XtMsgBody msgBody) {
        LogUtil.DataMessageLog(XtMessageHandler.class,"【file information of the Center "+currentCenter.getId()+"--> up to meter of "+
                command.getParameter() +" 】");
        int data[] = msgBody.getData();
        int num = ConvertUtil.binBytesToInt(data,0,1);
        LogUtil.DataMessageLog(XtMessageHandler.class,"此帧共包含 "+ num +" 块表的信息:" );
        for(int i = 2; (i + 22) <= data.length; i += 22){
            int index = ConvertUtil.binBytesToInt(data, i, i+1);
            String address = ConvertUtil.bcdBytesToString(data, i+2, i+7);
            String collector = ConvertUtil.bcdBytesToString(data,i+16,i+21);
            LogUtil.DataMessageLog(XtMessageHandler.class,"表序号："+index + " 表地址："+address + "   采集通道号："+ collector);
        }
        LogUtil.DataMessageLog(XtMessageHandler.class,Constants.LINE_SEPARATOR);
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ConcurrentHashMap<String,Center> map =  GlobalMap.getMap();
        String centerAddr = "";
        Center center;
        for(Map.Entry<String,Center> entry : map.entrySet()){
            if(entry.getValue().getCtx() == ctx){
                centerAddr = entry.getKey();
                center = entry.getValue();
                //如果命令状态正在执行
                if(center.getCurCommand()!=null && center.getCurCommand().getState()==CommandState.EXECUTING){
                    center.getCurCommand().setSuspend(true); //挂起命令
                    LogUtil.channelLog(centerAddr,"disconnected when executing the command --> command "+ center.getCurCommand().getType() + " suspend");
                }
                break;
            }
        }
        LogUtil.DataMessageLog(XtMessageHandler.class,"one client disconnected ! center address："+centerAddr +" ctx:"+ctx+"   channel message："+ctx.channel() + "    is active ? ："+ctx.channel().isActive());
        super.disconnect(ctx, promise);
        if(ctx != null) ctx.close();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.DataMessageLog(XtMessageHandler.class,"业务处理时异常");
        LogUtil.DataMessageLog(XtMessageHandler.class,cause.getMessage());
        ExceptionProcessor.processAfterException(ctx);//将对应集中器的命令状态置为失败或增加命令重试次数
        ctx.close();
    }

}
