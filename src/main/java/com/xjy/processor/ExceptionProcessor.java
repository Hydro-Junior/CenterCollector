package com.xjy.processor;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.util.DBUtil;
import io.netty.channel.ChannelHandlerContext;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:21 2018/11/30
 * @Description:出现错误时的处理方法
 */
public class ExceptionProcessor {
    //将对应集中器的命令状态置为失败
    public static void processAfterException(ChannelHandlerContext ctx){
        ConcurrentHashMap<String,Center> centerMap = GlobalMap.getMap();
        Iterator<Map.Entry<String,Center>> it = centerMap.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Center> entry = it.next();
            if(entry.getValue().getCtx() == ctx){
                Command c = entry.getValue().getCurCommand();
                if(c != null){
                    c.setState(CommandState.FAILED);
                    DBUtil.updateCommandState(CommandState.FAILED , c.getId());
                }
            }
        }
    }
}
