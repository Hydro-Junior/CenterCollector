package com.xjy.subjob;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.pojo.Scheme;
import com.xjy.util.DBUtil;
import com.xjy.util.LogUtil;
import io.netty.channel.ChannelHandlerContext;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 9:18 2018/12/7
 * @Description: 定时采集任务实现
 */
public class TimingCollect implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ConcurrentHashMap<String,Center> map = GlobalMap.getMap();
        DBUtil.createTempDeviceTable();//如果没有临时数据表，会自动创建
        Iterator<Map.Entry<String,Center>> it = map.entrySet().iterator();
        try {
            LogUtil.planTaskLog("---------------------Plan Collecting Polling "+map.size()+" centers--------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(it.hasNext()){
            Map.Entry<String,Center> entry = it.next();
            Center center = entry.getValue();
            try {
                LogUtil.planTaskLog("[TARGET CENTER: "+ entry.getKey()+"   center Address: "+center.getId()+" ]");
            } catch (IOException e) {
                e.printStackTrace();
            }
            ChannelHandlerContext channelCtx = center.getCtx();
            try{
                if(channelCtx.channel().isActive()){
                    LogUtil.planTaskLog("Active");
                    Command c = center.getCurCommand();
                    if(c == null || c.getState()== CommandState.FAILED || c.getState()== CommandState.SUCCESSED){
                        //查询数据库，判断这个整点是否可以执行
                        Scheme scheme = DBUtil.getScheme(center);
                        if(scheme == null){
                            LogUtil.planTaskLog("empty schemeId of center ["+center.getId()+"]");
                        }
                        int beginHour = 0;//开始执行时刻
                        int dayReadNum = 1;//读次数
                        int interval = 24;//间隔小时数
                        if(scheme != null){
                             beginHour = scheme.getBeginTime();//开始执行时刻
                             dayReadNum = scheme.getDayReadNum();//读次数
                             interval = scheme.getHourInterval();//间隔小时数
                        }
                        if(24/dayReadNum > interval) interval = 24/dayReadNum;
                        if(interval == 1) beginHour = 0;
                        LogUtil.planTaskLog(scheme.toString());
                        for(int i = beginHour; i < 24; i += interval){
                            if(i == LocalDateTime.now().getHour()){
                                String info = "distribute the Task for  "+center.getId()+"  of  "+center.getEnprNo();
                                LogUtil.planTaskLog(info);
                                DBUtil.insertNewCollectCommand(center);
                                //else InternalProtocolSendHelper.collect(center);//默认当做内部协议处理
                                break;
                            }
                        }
                    }
                }else{
                    LogUtil.planTaskLog("InActive");
                }
            }catch (NullPointerException e){
                //防止万一得不到context或channel报异常
                //更新数据库，将集中器设置为不在线
                LogUtil.DataMessageLog(TimingCollect.class,"空指针异常（没有合法的信道）,集中器"+center.getId()+"掉线！");
                DBUtil.updateCenterState(0,entry.getValue());
                //it.remove();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
