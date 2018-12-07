package com.xjy.core;

import com.xjy.subjob.TimingCollect;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @Author: Mr.Xu
 * @Date: Created in 8:57 2018/12/7
 * @Description:
 */
public class ScheduleTask {
    private static SchedulerFactory schedulerFactory = new StdSchedulerFactory();
    public static void startTimingCollect(){
        try {
            Scheduler scheduler = schedulerFactory.getScheduler();
            JobDetail jobDetail = JobBuilder.newJob(TimingCollect.class).withDescription("定时轮询采集")
                    .withIdentity("timingCollect","allProtocol").build();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("cronTrigger","triggerGp")
                    .withDescription("cronTrigger")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?")).build();
            scheduler.scheduleJob(jobDetail,trigger);
            scheduler.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
