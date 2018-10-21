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
            System.out.println("在线集中器：");
            for(Map.Entry<String,Center> entry : map.entrySet()){
                ChannelHandlerContext channelCtx = entry.getValue().getCtx();
                try{
                    if(channelCtx.channel().isActive()){
                        System.out.println(entry.getKey()+ ":" + entry.getValue());
                    }else{
                        //todo 更新数据库，将集中器设置为不在线
                    }
                }catch (NullPointerException e){
                    //防止万一得不到context或channel报异常
                    //todo 更新数据库，将集中器设置为不在线
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
