package com.xjy.subjob;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.util.DBUtil;
import com.xjy.sender.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:28 2019/1/6
 * @Description: 补充读取，暂不使用
 */
public class AdditionalRead implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
        DBUtil.createTempDeviceTable();//如果没有临时数据表，会自动创建
        Iterator<Map.Entry<String,Center>> it = map.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Center> entry = it.next();
            Center center = entry.getValue();
            ChannelHandlerContext channelCtx = center.getCtx();
            try{
                if(channelCtx.channel().isActive()){
                    Command c = center.getCurCommand();
                    if(c == null || c.getState()== CommandState.FAILED || c.getState()== CommandState.SUCCESSED){
                        if(center.getReadTime()==null || center.getReadTime().getDayOfYear()!= LocalDateTime.now().getDayOfYear()){
                            //如果今天还未读取
                            InternalProtocolSendHelper.readNextPage(center,1);
                        }
                    }
                }
            }catch (NullPointerException e){
                //防止万一得不到context或channel报异常
                //更新数据库，将集中器设置为不在线
                LogUtil.DataMessageLog(TimingCollect.class,"空指针异常（没有合法的信道）,集中器"+center.getId()+"掉线！");
                DBUtil.updateCenterState(0,entry.getValue());
                //it.remove();
            }
        }
    }
}
