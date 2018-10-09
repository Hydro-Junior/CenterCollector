package com.xjy.core;

import com.xjy.entity.Center;
import com.xjy.entity.GlobalMap;
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
        System.out.println("监控程序启动！");
        while (true){
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
            System.out.println("当前在线集中器：");
            for(Map.Entry<String,Center> entry : map.entrySet()){
                ChannelHandlerContext channelCtx = entry.getValue().getCtx();
                if(channelCtx.channel().isActive()){
                    System.out.println(entry.getKey()+ ":" + entry.getValue());
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
