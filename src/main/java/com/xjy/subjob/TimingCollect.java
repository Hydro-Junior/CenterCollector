package com.xjy.subjob;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.parms.Constants;
import com.xjy.pojo.Scheme;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
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
 * @Date: Created in 9:18 2018/12/7
 * @Description:
 */
public class TimingCollect implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
        Iterator<Map.Entry<String,Center>> it = map.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<String,Center> entry = it.next();
            Center center = entry.getValue();
            ChannelHandlerContext channelCtx = center.getCtx();
            try{
                if(channelCtx.channel().isActive()){
                    Command c = center.getCurCommand();
                    if(c == null || c.getState()== CommandState.FAILED || c.getState()== CommandState.SUCCESSED){
                        //查询数据库，判断这个整点是否可以执行
                        Scheme scheme = DBUtil.getScheme(center);
                        int beginHour = scheme.getBeginTime();//开始执行时刻
                        int interval = scheme.getHourInterval();//间隔小时数
                        for(int i = beginHour; i < 24; i += interval){
                            if(i == LocalDateTime.now().getHour()){
                                if(Constants.protocol != null && Constants.protocol.equalsIgnoreCase("internal"))
                                    InternalProtocolSendHelper.collect(center);
                                //todo 其他协议情况
                                else InternalProtocolSendHelper.collect(center);//默认当做内部协议处理
                                break;
                            }
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
