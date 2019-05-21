package com.xjy.core;

import com.xjy.entity.*;
import com.xjy.parms.CommandState;
import com.xjy.parms.CommandType;
import com.xjy.parms.Constants;
import com.xjy.util.DBUtil;
import com.xjy.sender.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;
import com.xjy.sender.XTProtocolSendHelper;
import io.netty.channel.ChannelHandlerContext;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:20 2018/10/25
 * @Description: 命令执行线程，针对每个集中器，不断地从命令队列中取出待执行命令进而执行
 */
public class CommandExecutor implements Runnable{
    @Override
    public void run() {
        while(true){
            ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
            for(Map.Entry<String,Center> entry : map.entrySet()){
                Center center = entry.getValue();
                //判断当前命令是否超时（默认）
                Command cur = null;
                if(( cur = center.getCurCommand()) != null && cur.getState()== CommandState.EXECUTING){
                    LocalDateTime start = cur.getStartExecuteTime();
                    Duration duration = Duration.between(start,LocalDateTime.now());
                    if(duration.toMillis() / 1000 > cur.getSecondsLimit()){
                        int retry = cur.getRetryNum();
                        if(retry < cur.getAllowedRetryTimes() && cur.getType() != CommandType.COLLECT_FOR_CENTER){
                            //主动重发上一条命令
                            System.out.println("重发指令！命令类型："+cur.getType());
                            if(Constants.protocol != null && Constants.protocol.equals("XT")){
                                System.out.println("lastMessage:"+ center.getLatestMsg());
                                XTProtocolSendHelper.writeAndFlush(center,(XtMsgBody) (center.getLatestMsg()));
                            }else{
                                InternalProtocolSendHelper.writeAndFlush(center,center.getLatestMsg());
                            }
                            cur.setStartExecuteTime(LocalDateTime.now()); //一定要重置命令开始时间
                            cur.setRetryNum(retry + 1);
                        }else{
                            LogUtil.DataMessageLog(CommandExecutor.class,"overTime causes command failed! The center address : "+center.getId()+" durationMillis: "+ duration.toMillis() + "current command allowed time gap:"+ cur.getSecondsLimit()
                            +"has retried times : "+ retry);
                            cur.setState(CommandState.FAILED);
                            DBUtil.updateCommandState(CommandState.FAILED,center);
                        }
                    }
                }
                ChannelHandlerContext channelCtx = center.getCtx();
                // 通道活跃是发送指令的前提,其次是集中器当前的命令队列不为空
                if(channelCtx.channel().isActive() && !center.getCommandQueue().isEmpty()){
                    //当前集中器正在执行的命令的状态
                    //集中器当前命令curCommand为null，或状态是成功或失败的情况，提取新的命令执行
                    if(center.getCurCommand() == null || center.getCurCommand().getState() == CommandState.SUCCESSED
                            ||center.getCurCommand().getState() == CommandState.FAILED){
                       center.setCurCommand(center.getCommandQueue().poll());
                       Command currentCommand = center.getCurCommand();
                       currentCommand.setState(CommandState.EXECUTING);
                       DBUtil.updateCommandState(CommandState.EXECUTING,center);
                       currentCommand.setStartExecuteTime(LocalDateTime.now());
                       //先判断当前协议，随后根据不同的命令类型发送不同的指令
                        if(Constants.protocol != null && Constants.protocol.equals("XT")){
                            executeForXTProtocol(center ,currentCommand);
                        }else{
                            executeForInternalProtocol(center ,currentCommand);
                        }
                    }
                }
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void executeForXTProtocol(Center center, Command currentCommand) {
        switch (currentCommand.getType()) {
            //读取集中器信息，执行130中的读取档案命令
            case READ_CENTER_INFO:
                timeLimitSetFor130(center);
                currentCommand.setParameter(0); //这里的参数是表序号的偏置量
                XTProtocolSendHelper.getFileInfo(center,currentCommand);
                break;
            /**
             *  抄表
             *  对于130协议，抄单个表和抄所有表数据单元格式是一致的：
             *  数据单元表示（4字节 00 00 01 07）
             *  抄表方式 （1字节，自动中继为 00）
             *  表数量（2字节） 表序号（2字节） 表序号（2字节）...
             */
            case READ_SINGLE_METER:
                XTProtocolSendHelper.readSingleMeter(center,currentCommand);
                break;
            case READ_ALL_METERS:
                timeLimitSetFor130(center);
                currentCommand.setParameter(0); //表序号的偏置量，表示已经读到了哪个表
                XTProtocolSendHelper.readMeters(center,currentCommand);
                break;
            case COLLECT_FOR_CENTER: //对于130协议，没有单独的采集命令，读取是实时的，所以与读取所有表处理方式相同
                currentCommand.setType(CommandType.READ_ALL_METERS);
                timeLimitSetFor130(center);
                currentCommand.setParameter(0); //表序号的偏置量，表示已经读到了哪个表
                XTProtocolSendHelper.readMeters(center,currentCommand);
                break;
            //下载档案
            case WRITE_INFO:
                timeLimitSetFor130(center);
                currentCommand.setParameter(0); //起始档案序号
                XTProtocolSendHelper.writeFileInfo(center,currentCommand);
                break;
            default:
                setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
                break;
        }
        //setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
    }
    private static void timeLimitSetFor130(Center center){
        List<MeterOf130> meters = XTProtocolSendHelper.constructAndGetMetersInfo(center);
        int timeLimit = Math.max(meters.size()/10 * 2,5);
        center.getCurCommand().setSecondsLimit(timeLimit * 60);//设置超时时间限制，与表个数关联
        Timestamp t3 = Timestamp.valueOf(LocalDateTime.now().plusMinutes(timeLimit));
        DBUtil.updateCommandEndTime(center,t3);
    }

    private static void executeForInternalProtocol(Center center, Command currentCommand) {
        switch (currentCommand.getType()){
            case OPEN_VALVE://开阀：先打开节点
                String collector = center.getCurCommand().getArgs()[1];//得到采集器编号
                LogUtil.DataMessageLog(CommandExecutor.class,"要开阀的采集器通道编号："+collector);
                if(collector.endsWith("00")){//无线，没有通道
                    InternalProtocolSendHelper.openValve(center);
                }else{//有线，先打开通道
                    InternalProtocolSendHelper.openChannel(center,currentCommand);
                }
                break;
            case CLOSE_VALVE://关阀：先打开节点
                String collector2 = center.getCurCommand().getArgs()[1];//得到采集器编号
                LogUtil.DataMessageLog(CommandExecutor.class,"要关阀的采集器通道编号："+collector2);
                if(collector2.endsWith("00")){//无线，没有通道
                    InternalProtocolSendHelper.closeValve(center);
                }else{//有线，先打开通道
                    InternalProtocolSendHelper.openChannel(center,currentCommand);
                }
                break;
            case WRITE_INFO:
                List<CenterPage> pages = InternalProtocolSendHelper.constructPages(center);
                System.out.println("总页数" + pages.size());//总页数
                int timeLimit = Math.max(pages.size() * 1,5);
                center.getCurCommand().setSecondsLimit(timeLimit * 60);//设置写页超时时间限制，与页数关联
                Timestamp t = Timestamp.valueOf(LocalDateTime.now().plusMinutes(timeLimit));
                DBUtil.updateCommandEndTime(center,t);
                center.getCurCommand().setParameter(1);
                InternalProtocolSendHelper.writePage(center,1);
                break;
            case COLLECT_FOR_METER:
                String theCollector = null;
                String theMeter = null;
                if(currentCommand.getArgs()!= null && currentCommand.getArgs().length > 2){
                    theCollector = currentCommand.getArgs()[1];
                    theMeter = currentCommand.getArgs()[2];
                }
                InternalProtocolSendHelper.collect(center,theCollector,theMeter);
                break;
            case COLLECT_FOR_CENTER:
                //统计时间
                int pageNum = getTotalPages(center);
                int timeLimit1 = Math.max(pageNum * 2,5);
                center.getCurCommand().setSecondsLimit(timeLimit1 * 60);//设置采集超时时间限制，与页数关联
                Timestamp t1 = Timestamp.valueOf(LocalDateTime.now().plusMinutes(timeLimit1));
                DBUtil.updateCommandEndTime(center,t1);
                InternalProtocolSendHelper.collect(center);
                break;
            case READ_CENTER_INFO:
                InternalProtocolSendHelper.readFirstPage(center);//获取集中器的时钟、计划采集时间等
                break;
            case READ_SINGLE_METER:
                //开始读取之前，获取center在数据库中的id和水司编号，应对修改数据库中表具资料后的情况
                DBUtil.preprocessOfRead(center);
                InternalProtocolSendHelper.readNextPage(center,1);
                break;
            case READ_ALL_METERS:
                //开始读取之前，获取center在数据库中的id和水司编号，应对修改数据库中表具资料后的情况
                DBUtil.preprocessOfRead(center);
                int pageNum2 = getTotalPages(center);
                int timeLimit2 = Math.max(pageNum2 * 1,5);
                Timestamp t2 = Timestamp.valueOf(LocalDateTime.now().plusMinutes(timeLimit2));
                DBUtil.updateCommandEndTime(center,t2); //设置允许整个命令结束时间，防止web程序中途将其设置为不在线
                currentCommand.setAllowedRetryTimes(Constants.RETRY_TIMES_FOR_READ_PAGES); //允许最多重试次数，设置每次读页最长允许等待时间见readNestPage方法
                InternalProtocolSendHelper.readNextPage(center,1);
                break;
            case CHECK_CLOCK: //设备校时，顺便设置定时采集
                InternalProtocolSendHelper.setClock(center);
                break;
            default:
                setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
                break;
        }

    }
    private static void setCurCommandState(int state, Center center, Command currentCommand){
        DBUtil.updateCommandState(state,center);
        currentCommand.setState(state);
    }
    private static int getTotalPages(Center center){
        ConcurrentHashMap<Center,List<CenterPage>> infos = GlobalMap.getBasicInfo();
        int totalPages;
        if(!infos.containsKey(center)){
            totalPages = InternalProtocolSendHelper.constructPages(center).size();
        }else{
            totalPages = infos.get(center).size();
        }
        return totalPages;
    }
}

