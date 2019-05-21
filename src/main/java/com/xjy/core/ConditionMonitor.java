package com.xjy.core;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.util.DBUtil;
import com.xjy.util.LogUtil;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Mr.Xu
 * @Date: Created in 8:45 2018/10/3
 * @Description:
 */
public class ConditionMonitor implements Runnable {
    @Override
    public void run() {
        System.out.println("监控程序启动！开始初始化：");
        DBUtil.initCenters();
        while (true){
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
            System.out.println("集中器(共 "+map.size()+" 台)：");
            Iterator<Map.Entry<String,Center>> it = map.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String,Center> entry = it.next();
                Center center = entry.getValue();
                if(center.getEnprNo() == null || center.getEnprNo().equals("")){//加载水司编码
                    DBUtil.getEnprNoByAddress(center);
                }
                ChannelHandlerContext channelCtx = entry.getValue().getCtx();
                try{
                    Command c = entry.getValue().getCurCommand();
                    if(center.getHeartBeatTime() != null){ //长时间没有心跳且有命令在执行，挂起命令
                        Duration duration = Duration.between(center.getHeartBeatTime(), LocalDateTime.now());
                        if(duration.toMinutes() > 8){
                            if(c.getState()==CommandState.EXECUTING){
                                LogUtil.channelLog(center.getId(),"监控时发现执行命令时断开，将"+center.getCurCommand().getType()+"命令挂起\r\n");
                                c.setSuspend(true);
                            }
                        }
                    }
                    if(channelCtx.channel().isActive()){
                        LogUtil.DataMessageLog(ConditionMonitor.class, entry.getKey()+  "   当前命令："+ c + "  水司编码："+entry.getValue().getEnprNo());
                    }else{
                        //更新数据库，将集中器设置为不在线
                        LogUtil.DataMessageLog(ConditionMonitor.class,"集中器"+entry.getValue().getId()+"掉线！");
                        DBUtil.updateCenterState(0,entry.getValue());
                        //it.remove();
                    }
                }catch (NullPointerException e){
                    //防止万一得不到context或channel报异常
                    //更新数据库，将集中器设置为不在线
                    LogUtil.DataMessageLog(ConditionMonitor.class,"空指针异常（没有合法的信道）,集中器"+entry.getValue().getId()+"掉线！");
                    DBUtil.updateCenterState(0,entry.getValue());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
