package com.xjy.core;

import com.xjy.entity.Center;
import com.xjy.entity.CenterPage;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.parms.CommandType;
import com.xjy.parms.Constants;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;
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
                    LocalDateTime start = cur.getStartExcuteTime();
                    Duration duration = Duration.between(start,LocalDateTime.now());
                    if(duration.toMinutes() > cur.getMinitesLimit()){
                        cur.setState(CommandState.FAILED);
                        DBUtil.updateCommandState(CommandState.FAILED,center);
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
                       currentCommand.setStartExcuteTime(LocalDateTime.now());
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
        setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
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
                center.getCurCommand().setMinitesLimit(timeLimit);//设置写页超时时间限制，与页数关联
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
                center.getCurCommand().setMinitesLimit(timeLimit1);//设置采集超时时间限制，与页数关联
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

