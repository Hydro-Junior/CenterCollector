package com.xjy.core;

import com.xjy.entity.Center;
import com.xjy.entity.GlobalMap;
import com.xjy.util.DBUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

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
            System.out.println("在线集中器：");
            for(Map.Entry<String,Center> entry : map.entrySet()){
                ChannelHandlerContext channelCtx = entry.getValue().getCtx();
                try{
                    if(channelCtx.channel().isActive()){
                        System.out.println(entry.getKey()+  "   当前命令："+entry.getValue().getCurCommand());
                    }else{
                        //更新数据库，将集中器设置为不在线
                        DBUtil.updateCenterState(0,entry.getValue());
                    }
                }catch (NullPointerException e){
                    //防止万一得不到context或channel报异常
                    //更新数据库，将集中器设置为不在线
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
