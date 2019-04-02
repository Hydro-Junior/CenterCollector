package com.xjy.core;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.subjob.TimingCollect;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

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
                TimeUnit.MINUTES.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
            System.out.println("在线集中器：");
            Iterator<Map.Entry<String,Center>> it = map.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String,Center> entry = it.next();
                Center center = entry.getValue();
                if(center.getEnprNo() == null || center.getEnprNo().equals("")){//加载水司编码
                    DBUtil.getEnprNoByAddress(center);
                }
                ChannelHandlerContext channelCtx = entry.getValue().getCtx();
                try{
                    if(channelCtx.channel().isActive()){
                        Command c = entry.getValue().getCurCommand();
                        System.out.println(entry.getKey()+  "   当前命令："+ c + "  水司编码："+entry.getValue().getEnprNo());
                        //定时采集设置命令可以成功执行，然后似乎硬件实际并没有执行，为了确保定时采集成功，
                        //用quartz定时任务替代：由程序亲自发送采集命令
                        /*if(c == null || c.getState()== CommandState.FAILED || c.getState()==CommandState.SUCCESSED)
                                InternalProtocolSendHelper.setTimingCollect(entry.getValue());*/
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
                }
            }
            try {
                TimeUnit.SECONDS.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
