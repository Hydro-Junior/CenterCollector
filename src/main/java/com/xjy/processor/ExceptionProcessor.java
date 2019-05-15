package com.xjy.processor;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.parms.Constants;
import com.xjy.util.DBUtil;
import com.xjy.util.LogUtil;
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
    //将对应集中器的命令状态置为失败或增加重试次数
    public static void processAfterException(ChannelHandlerContext ctx){
        ConcurrentHashMap<String,Center> centerMap = GlobalMap.getMap();
        Iterator<Map.Entry<String,Center>> it = centerMap.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Center> entry = it.next();
            if(entry.getValue().getCtx() == ctx){
                Command c = entry.getValue().getCurCommand();
                if(c != null){
                    int retry = c.getRetryNum();
                    if(retry >= c.getAllowedRetryTimes()){ //大于等于允许重试次数，命令失败
                        LogUtil.DataMessageLog(ExceptionProcessor.class, "exception captured and retryTime = " + retry);
                        c.setState(CommandState.FAILED);
                        DBUtil.updateCommandState(CommandState.FAILED , c.getId());
                    }
                }
            }
        }
    }
}
