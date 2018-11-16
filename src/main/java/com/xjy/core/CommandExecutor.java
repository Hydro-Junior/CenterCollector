package com.xjy.core;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.parms.CommandType;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
import io.netty.channel.ChannelHandlerContext;

import java.time.Duration;
import java.time.LocalDateTime;
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
                       //根据不同的命令类型发送不同的指令
                       execute(center ,currentCommand);
                    }
                }
            }
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void execute(Center center, Command currentCommand) {
        switch (currentCommand.getType()){
            case OPEN_VALVE:
                //todo
                setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
                break;
            case CLOSE_VALVE:
                //todo
                setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
                break;
            case WRITE_INFO:
                InternalProtocolSendHelper.writeFirstPage(center);
                break;
            case COLLECT_FOR_CENTER:
                //todo
                setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
                break;
            case READ_SINGLE_METER:
                InternalProtocolSendHelper.readFirstPage(center);
                break;
            case READ_ALL_METERS:
                InternalProtocolSendHelper.readFirstPage(center);
                break;
            case OPEN_VALVE_BATCH:
                //todo
                setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
                break;
            case CLOSE_VALVE_BATCH:
                //todo
                setCurCommandState(CommandState.SUCCESSED,center,currentCommand);
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
}
