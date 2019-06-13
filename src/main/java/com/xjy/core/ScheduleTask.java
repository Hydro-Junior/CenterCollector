package com.xjy.core;

import com.xjy.subjob.AdditionalRead;
import com.xjy.subjob.TimingCollect;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @Author: Mr.Xu
 * @Date: Created in 8:57 2018/12/7
 * @Description:定时任务处理
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
            /**
             * 补读操作
             * */
            /*Scheduler additional = schedulerFactory.getScheduler();
            JobDetail addiJob = JobBuilder.newJob(AdditionalRead.class).withDescription("弥补式读取")
                    .withIdentity("additionalRead","allProtocol").build();
            CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("cronTrigger2","triggerGp2")
                    .withDescription("cronTrigger2")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 40,50 5-7 * * ?")).build();//读取“漏网之鱼”
            additional.scheduleJob(addiJob,cronTrigger);
            additional.start();*/
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
