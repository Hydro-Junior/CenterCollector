package com.xjy.handler;

import com.xjy.adapter.CommandAdapter;
import com.xjy.core.CommandExecutor;
import com.xjy.entity.*;
import com.xjy.parms.*;
import com.xjy.pojo.DBCommand;
import com.xjy.processor.ExceptionProcessor;
import com.xjy.processor.InternalMsgProcessor;
import com.xjy.util.ConvertUtil;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.omg.PortableInterceptor.INACTIVE;

import java.net.SocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static com.xjy.entity.GlobalMap.getMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:23 2018/9/27
 * @Description: 内部协议业务消息处理器
 */
@ChannelHandler.Sharable
public class InternalMessageHandler extends ChannelHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            InternalMsgBody msgBody = (InternalMsgBody)msg;
            String address = msgBody.getDeviceId();
            LogUtil.channelLog(address,msgBody.toString());
            ConcurrentHashMap<String,Center> map = getMap();
            if(!map.containsKey(address)){
                Center center = new Center(address,ctx);
                map.put(address,center);
                LogUtil.DataMessageLog(InternalMessageHandler.class,"收到集中器"+address+"的首条消息！\r\n"+msgBody.toString());
                //更新数据库中集中器状态
                DBUtil.updateCenterState(1,center);
                //首次上线，如果有命令正在执行，将其置为成功！(有些硬件的特殊情况：执行采集命令会断开连接，结束后上线)
                List<DBCommand> dbCommands = DBUtil.getCommandByCenterAddressAndState(address,CommandState.EXECUTING);
                for(DBCommand dbCommand : dbCommands){
                    DBUtil.updateCommandState(CommandState.SUCCESSED, dbCommand.getId());
                }
            }else{
                Center center = map.get(address);
                center.setCtx(ctx);
                if(center.getCurCommand() != null && center.getCurCommand().isSuspend()){//在执行命令时掉线
                   if(center.getLatestMsg() != null &&center.getCurCommand().getState()==CommandState.EXECUTING && center.getCurCommand().getType()!=CommandType.COLLECT_FOR_CENTER){//如果是采集命令，重发时间代价太大，且有些集中器执行采集命令会断开连接，结束后上线
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
                   }else if(center.getCurCommand().getType() == CommandType.COLLECT_FOR_CENTER){//是采集命令的情况,重连通常命令成功，不用重发指令
                       center.getCurCommand().setState(CommandState.SUCCESSED);
                       DBUtil.updateCommandState(CommandState.SUCCESSED, center);
                       InternalProtocolSendHelper.readNextPage(center,1);
                   }
                   center.getCurCommand().setSuspend(false);
                }
            }
            Center currentCenter = map.get(address);

            if(currentCenter.getHeartBeatTime() == null ){
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
            if(msgBody.getMsgType() != InternalMsgType.HEARTBEAT_PACKAGE){//如果不是心跳包，进一步处理
                LogUtil.DataMessageLog(InternalMessageHandler.class,"收到有效数据："+msgBody.toString());
                String instruction = msgBody.getInstruction().trim();
                LogUtil.DataMessageLog(InternalMessageHandler.class,"指令类型:" + instruction);

                if(instruction.equals(InternalOrders.READ)){ //读取指令,按页解析读数
                    if(currentCenter.getDbId() == null) DBUtil.preprocessOfRead(currentCenter);//针对定时读取尚未初始化的情况
                    InternalMsgProcessor.readProcessor(currentCenter,msgBody);
                }
                else if(instruction.equals(InternalOrders.D_READ)){//开关阀后返回的表状态(或是单表采集)
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"指令类型:" + "进入D命令处理流程！");
                    Command c = currentCenter.getCurCommand();
                    if(c != null && c.getType() == CommandType.COLLECT_FOR_METER){ //命令有可能是采集，返回成功
                        c.setState(CommandState.SUCCESSED);
                        DBUtil.updateCommandState(CommandState.SUCCESSED, currentCenter);
                        return;
                    }
                    String lastInstruction ;//得到上次所发的指令
                    if(currentCenter.getLatestMsg() != null)  lastInstruction = ((InternalMsgBody)currentCenter.getLatestMsg()).getInstruction();
                    else  lastInstruction = "";
                    if(c != null && lastInstruction.equals(InternalOrders.OPEN_CHANNEL)){
                        //得到采集器地址，并关闭该节点
                        c.setState(CommandState.FAILED);
                        DBUtil.updateCommandState(CommandState.FAILED, currentCenter);
                        InternalProtocolSendHelper.closeChannelBefore(currentCenter,msgBody);
                        return;
                    }
                    //其他情况
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"正常开关阀情况处理！命令类型："+c.getType());
                    if(c.getType() == CommandType.CLOSE_VALVE || c.getType() == CommandType.OPEN_VALVE){
                        if(currentCenter.getDbId() == null ) DBUtil.preprocessOfRead(currentCenter);//初始化集中器在数据库中的id
                        //InternalMsgProcessor.getValveInfo(currentCenter,msgBody);//获取开关阀信息
                        LogUtil.DataMessageLog(InternalMessageHandler.class,"准备进入开关阀状态更新！");
                        InternalMsgProcessor.afterUpdateValveState(currentCenter,msgBody);
                        //关闭节点阀控
                        InternalProtocolSendHelper.closeChannel(currentCenter,currentCenter.getCurCommand());
                    }
                }
                else if(instruction.equals(InternalOrders.COLLECT)){//采集指令，说明采集器已经开始采集
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"集中器已经开始采集！");
                }
                else if(instruction.equals(InternalOrders.DOWNLOAD)){
                    //下载档案命令的处理器，先读取页数，是最后一页的话命令成功结束
                    InternalMsgProcessor.writeProcessor(currentCenter,msgBody);
                }
                else if(instruction.equals(InternalOrders.CLOCK)){//设备校时返回，设置命令成功，并设置定时采集
                    currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
                    InternalProtocolSendHelper.setTimingCollect(currentCenter);
                }
                else if(instruction.equals(InternalOrders.SCHEDUEL)){
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"设置定时采集成功！");
                }
                else if(instruction.equals(InternalOrders.SUCCESE)) {//（采集）命令执行成功
                    LogUtil.DataMessageLog(InternalMessageHandler.class, "(采集)命令执行成功！");
                    //非定时采集
                    if(currentCenter.getCurCommand() != null && (currentCenter.getCurCommand().getType()== CommandType.COLLECT_FOR_CENTER ||currentCenter.getCurCommand().getType()== CommandType.COLLECT_FOR_METER)){//非定时采集
                        currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
                        DBUtil.updateCommandState(CommandState.SUCCESSED, currentCenter);
                    }
                    InternalProtocolSendHelper.readNextPage(currentCenter,1);
                }
                else if(instruction.equals((InternalOrders.OPEN_CHANNEL))){//打开节点成功
                    Command command = currentCenter.getCurCommand();
                    if(command != null){
                        if(command.getType()==CommandType.OPEN_VALVE){
                            InternalProtocolSendHelper.openValve(currentCenter);
                        }else if(command.getType()==CommandType.CLOSE_VALVE){
                            InternalProtocolSendHelper.closeValve(currentCenter);
                        }
                    }
                }
                else if(instruction.equals(InternalOrders.OPCHANNEL_FAILED)){//打开节点失败
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"打开节点失败！");
                    //周全起见，关闭阀控节点
                    //InternalProtocolSendHelper.closeChannel(currentCenter,currentCenter.getCurCommand());
                    currentCenter.getCurCommand().setState(CommandState.FAILED);
                    DBUtil.updateCommandState(CommandState.FAILED,currentCenter);
                }
                else if(instruction.equals(InternalOrders.OPEN_VALVE) || instruction.equals(InternalOrders.CLOSE_VALVE)){//开关阀返回
                    System.out.println("开关阀返回！");
                    //if(currentCenter.getDbId() == null ) DBUtil.preprocessOfRead(currentCenter);//初始化集中器在数据库中的id
                    //InternalMsgProcessor.getValveInfo(currentCenter,msgBody);//获取开关阀信息
                    //if(currentCenter.getCommandQueue().isEmpty()) InternalProtocolSendHelper.closeChannel(currentCenter,currentCenter.getCurCommand());
                    //InternalProtocolSendHelper.DRead(currentCenter); //读节点表
                }
               /* else if(instruction.equals(InternalOrders.BEFORE_CLOSE)){//开关阀后读节点表返回
                    InternalMsgProcessor.getValveInfo(currentCenter,msgBody);//获取开关阀信息
                    if(currentCenter.getCommandQueue().isEmpty())InternalProtocolSendHelper.closeChannel(currentCenter,currentCenter.getCurCommand());
                }*/
                else if(instruction.equals(InternalOrders.CLOSE_CHANNEL)){//关通道成功
                    LogUtil.DataMessageLog(InternalMessageHandler.class,"开关阀后关闭通道");
                    Command c = currentCenter.getCurCommand();
                    if(c != null && (c.getType() == CommandType.OPEN_VALVE || c.getType() == CommandType.CLOSE_VALVE)){
                        c.setState(CommandState.SUCCESSED);
                    }
                }
            }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.DataMessageLog(InternalMessageHandler.class,"业务处理时异常");
        cause.printStackTrace();
        ExceptionProcessor.processAfterException(ctx);//将对应集中器的命令状态置为失败或增加命令重试次数
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //这里会根据新建立的连接，创建全新的ctx
        LogUtil.DataMessageLog(InternalMessageHandler.class,"新连接建立！" + ctx);
        //打印本地服务器IP端口，远端ip和端口
        /*System.out.println(ctx.channel().id()+"     local:"+ctx.channel().localAddress()+" || remote:"+ctx.channel().remoteAddress());
        String remoteAddr = ctx.channel().remoteAddress().toString();
        String remoteIP = remoteAddr.substring(1,remoteAddr.indexOf(":"));
        String remotePort = remoteAddr.substring(remoteAddr.indexOf(":")+1,remoteAddr.length());
        System.out.println(remoteIP + "  " + remotePort);*/
        /**
         * 下面的程序原来其实是为了根据IP和是否掉线的标记，在重新建立连接时找到掉线的集中器，从而补发原来可能未执行的命令
         * 特别针对 厦门诺特自组网 和 肇庆水司下的自组网集中器，可能由于所用模块有一定区别，它们经常一发消息就掉线
         * 然而经过测试，发现自组网的这两台IP也是动态获取的，那就无法在建立连接时进行补发消息的处理了,改在channelRead方法中处理。
         * 不过如下这段代码对于一些IP固定的客户端还是可以表达一定信息的，因此不删除。
         */
        SocketAddress socketAddress =  ctx.channel().remoteAddress();
        ConcurrentHashMap<String,Center> map =  GlobalMap.getMap();
        //System.out.println("当前map中的集中器内容：");
        for(Map.Entry<String,Center> entry : map.entrySet()){
            Center center = entry.getValue();
            String ip = ConvertUtil.getIP(center.getCtx().channel().remoteAddress().toString());
            String newIp = ConvertUtil.getIP(socketAddress.toString());
            if(ip.equals(newIp)){
                LogUtil.DataMessageLog(InternalMessageHandler.class,"刚刚掉线的集中器："+center.getId()+"\r\n"+"原来的ctx："+center.getCtx()+"  "+center.getCtx().channel().isActive() + "\r\n新的ctx:"+
                        ctx + "    "+ctx.channel().isActive());
                //确定了这是刚刚掉线的集中器，向它发送缓存命令（取消处理，不能在这里做）
            }
        }
        super.channelActive(ctx);
    }

    //处理客户端关闭的情况
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        ConcurrentHashMap<String,Center> map =  GlobalMap.getMap();
        String centerAddr = "";
        Center center;
        for(Map.Entry<String,Center> entry : map.entrySet()){
            if(entry.getValue().getCtx() == ctx){
                centerAddr = entry.getKey();
                center = entry.getValue();
                if(center.getCurCommand()!=null && center.getCurCommand().getState()==CommandState.EXECUTING){
                    center.getCurCommand().setSuspend(true); //挂起命令
                    LogUtil.channelLog(centerAddr,"handler removed when executing the command --> command "+ center.getCurCommand().getType() + " suspend");
                }
                break;
            }
        }
        LogUtil.DataMessageLog(InternalMessageHandler.class,"one client disconnected ! center address："+centerAddr +" ctx:"+ctx+"   channel message："+ctx.channel() + "    is active ? ："+ctx.channel().isActive());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    /*    System.out.println("通道不再活跃！");
        System.out.println("hasDisConnected?"+ctx.channel().metadata().hasDisconnect() +"   isWritable?"+ ctx.channel().isWritable()
        + "     isActive?"+ctx.channel().isActive() + "     isOpen?"+ctx.channel().isOpen());*/
        super.channelInactive(ctx);
    }
}
