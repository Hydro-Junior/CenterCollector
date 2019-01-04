package com.xjy.util;

import com.xjy.adapter.CollectorAdapter;
import com.xjy.adapter.MeterAdapter;
import com.xjy.entity.*;
import com.xjy.parms.CommandType;
import com.xjy.parms.InternalOrders;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBMeter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @Author: Mr.Xu
 * @Date: Created in 17:03 2018/10/25
 * @Description: 针对内部协议，封装在不同情境下向集中器发送指令的语句
 */
public class InternalProtocolSendHelper {

    public static void collect(Center center,String collectorId,String meterId){
        int[] effectiveData = new int[3 + 6 + 6];//指令字3个字节+采集器地址6个字节+表地址6个字节
        effectiveData[0] = effectiveData[1] = effectiveData[2] = InternalOrders.COLLECT.getBytes()[0];
        for(int i = 0 ; i < 12; i++){//默认对整个集中器采集，用FF填充地址
            effectiveData[i+3] = 0xFF;
        }
        if(collectorId != null){//对采集器进行采集
            int[] collectorBytes = ConvertUtil.addressToBytes(collectorId);
            for(int i = 0; i < 6; i++){
                effectiveData[i+3] = collectorBytes[i];
            }
        }
        if(collectorId != null && meterId != null){//对单只表进行采集
            int[] meterAddress = ConvertUtil.addressToBytes(meterId);
            for (int i = 0; i< 6; i++){
                effectiveData[i+3+6] = meterAddress[i];
            }
        }
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), effectiveData);
        writeAndFlush(center,internalMsgBody);
    }
    //内部协议，读取下一页
    public static void readNextPage(Center center, int currentPageNum){
        int[] effectiveData = new int[3 + 1];//指令字3个字节+页面号1个字节
        for (int i = 0; i < effectiveData.length; i++) {
            if (i < 3) effectiveData[i] = InternalOrders.READ.getBytes()[i];
            else effectiveData[i] = currentPageNum + 1;
        }
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), effectiveData);
        writeAndFlush(center,internalMsgBody);
    }
    //内部协议，读取首页
    public static void readFirstPage(Center center){
        readNextPage(center,-1);
    }
    //采集
    public static void collect(Center center){
        collect(center,null,null);
    }

    /**
     * 写入资料,采集器所在页，在这个方法中，构建好集中器所有页的数据资料,同时更新集中器状态map中的资料
     * @param center 集中器
     */
    public static void writeFirstPage(Center center){
        LogUtil.DataMessageLog(InternalProtocolSendHelper.class,"开始执行写页指令");
        //按页构建资料(同时写入全局变量)
        List<CenterPage> pages = constructPages(center);

        LogUtil.DataMessageLog(InternalProtocolSendHelper.class,"页资料:\n"+pages.toString());

        //发送第一页（采集器页）
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), addInstruction(pages.get(0).getData(),InternalOrders.DOWNLOAD));

        //调试日志
        writeAndFlush(center,internalMsgBody);
    }
    //设置系统时钟，指令字 TTT + 16字节数据
    public static void setClock(Center center){
        //构建16字节的时间数据，格式CTyyyymmddhhmmss
        int[] data = new int[19];
        data[0] = 'T';data[1] = 'T';data[2] = 'T';
        data[3] = 'C'; data[4] = 'T';//CT
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime time = LocalDateTime.now();
        String localTime = df.format(time);
        for(int i = 0 ; i < localTime.length(); i++){
            data[i+5] = localTime.charAt(i);
        }
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(),data);
        ByteBuf buf = Unpooled.copiedBuffer(internalMsgBody.toBytes());
        center.getCtx().writeAndFlush(buf);
    }
    //从数据库中查询集中器下的采集通道信息与表信息，构建页资料，同时放入信息全局变量
    public static  List<CenterPage> constructPages(Center center){
        ConcurrentHashMap<Center,List<CenterPage>> persistentDataOfInternalProtocol = GlobalMap.getBasicInfo();
        //查找数据库中该集中器对应的采集器;
        List<DBCollector> dbcollectors = DBUtil.getCollectorsByCenter(center);
        List<Collector> collectors = new ArrayList<>();
        //遍历采集器集合，查询获得总表集合，构建集中器资料
        for(int i = 0 ; i < dbcollectors.size(); i++){
            DBCollector dbCollector = dbcollectors.get(i);
            Collector theCollector = CollectorAdapter.getCollector(dbCollector);
            theCollector.setCenter(center);
            collectors.add(theCollector);
            List<DBMeter> dbMeters = DBUtil.getMetersByCollector(dbcollectors.get(i));
            List<Meter> meters = new ArrayList<>();
            for(DBMeter dbMeter : dbMeters){
                Meter theMeter = MeterAdapter.getMeter(dbMeter);
                theMeter.setCollectorIndex(i);//设置对应采集器序号
                theMeter.setCollector(theCollector); //设置所属采集器
                meters.add(theMeter);
            }
            theCollector.setMeters(meters);//更新每个采集器的表资料
        }
        center.setCollectors(collectors);//更新集中器的采集器资料
        //按页构建资料
        List<CenterPage> pages = CenterPage.generateCenterPages(center);
        persistentDataOfInternalProtocol.put(center,pages);
        return pages;
    }
    //设置定时采集的处理器(设置为当天x点和第二天x点，时间参考数据库，测试阶段先写死)
    public static void setTimingCollect(Center currentCenter) {//数据格式PT1(Y/N)dd2(Y/N)ddhhmmss
        int[] data = new int[19];//3字节指令字+数据
        data[0] = 'P';data[1] = 'P';data[2]='P';
        data[3] = 'P'; data[4] = 'T';//后面2个字节是dd(均为Y，每天采集,为N则都不采)
        data[5] = 1+'0';data[6] = 'N'; //7|8 dd
        data[9] = 2+'0';data[10] = 'N';//11|12 dd

        LocalDateTime time = LocalDateTime.now();
        //设置两个dd(默认是每天采集)
        int day1 = time.getDayOfMonth();//今天
        int day2 = time.plusDays(1).getDayOfMonth();//明天
        int hi1 = day1/10, hi2 = day2/10, lo1 = day1%10, lo2 = day2%10;
        data[7] = hi1+'0'; data[8] = lo1+'0';
        data[11] = hi2+'0'; data[12] = lo2+'0';
        //设置hhmmss
        int h = 23, m = 30, s = 0;
        int h1 = h / 10, h2 = h % 10, m1 = m /10, m2 = m % 10, s1 = s/10, s2 = s % 10;
        data[13] = h1+'0'; data[14] = h2+'0';
        data[15] = m1+'0';data[16] = m2+'0';
        data[17] = s1+'0'; data[18] = s2+'0';
        InternalMsgBody internalMsgBody = new InternalMsgBody(currentCenter.getId() ,data);
        writeAndFlush(currentCenter,internalMsgBody);
    }
    public static void writePage(Center center,int page){
        //从persistentDataOfInternalProtocol中获取center相应页的资料，构建msg发送即可
        ConcurrentHashMap<Center,List<CenterPage>> persistentDataOfInternalProtocol = GlobalMap.getBasicInfo();
        List<CenterPage> pages = persistentDataOfInternalProtocol.get(center);
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), addInstruction(pages.get(page-1).getData(),InternalOrders.DOWNLOAD));

        writeAndFlush(center,internalMsgBody);
    }
    private static int[] addInstruction(int[] originData,String instruction){
        int[] res = new int[originData.length + 3];
        for(int i = 0 ; i < 3; i++){
            res[i] = instruction.charAt(i);
        }
        System.arraycopy(originData,0,res,3,originData.length);
        return res;
    }

    /**
     * 打印消息发送日志
     * @param internalMsgBody
     */
    private static void printMsgLog(InternalMsgBody internalMsgBody){
        LogUtil.DataMessageLog(InternalProtocolSendHelper.class,"待发送报文：\n");
        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < internalMsgBody.toBytes().length; i++){
            sb.append(ConvertUtil.fixedLengthHex(internalMsgBody.toBytes()[i])+" ");
            if(i !=0 && i % 30 == 0) sb.append("\r\n");
        }
        String channelLogContent = "[MESSAGE SEND]{\r\n" + sb.toString()+"}";
        try {
            LogUtil.channelLog(internalMsgBody.getDeviceId(),channelLogContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtil.DataMessageLog(InternalProtocolSendHelper.class, sb.toString());
    }

    //打开通道
    public static void openChannel(Center center, Command currentCommand) {

        String collector = currentCommand.getArgs()[1];
        System.out.println("通道号："+collector);
        int[] effectiveData = addInstruction(ConvertUtil.addressToBytes(collector),InternalOrders.OPEN_CHANNEL);
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(),effectiveData);

        writeAndFlush(center,internalMsgBody);
    }
    //关闭通道
    public static void closeChannel(Center center, Command currentCommand) {
        String collector = currentCommand.getArgs()[1];
        int[] effectiveData = addInstruction(ConvertUtil.addressToBytes(collector),InternalOrders.CLOSE_CHANNEL);
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(),effectiveData);
        writeAndFlush(center,internalMsgBody);
    }
    //开阀
    public static void openValve(Center currentCenter) {
        String meter = currentCenter.getCurCommand().getArgs()[2];
        int[] effectiveData = addInstruction(ConvertUtil.addressToBytes(meter),InternalOrders.OPEN_VALVE);
        InternalMsgBody internalMsgBody = new InternalMsgBody(currentCenter.getId(),effectiveData);
        writeAndFlush(currentCenter,internalMsgBody);
    }
    //关阀
    public static void closeValve(Center currentCenter) {
        String meter = currentCenter.getCurCommand().getArgs()[2];
        int[] effectiveData = addInstruction(ConvertUtil.addressToBytes(meter),InternalOrders.CLOSE_VALVE);
        InternalMsgBody internalMsgBody = new InternalMsgBody(currentCenter.getId(),effectiveData);
        writeAndFlush(currentCenter,internalMsgBody);
    }

    /**
     * 封装消息发送
     * 另外，存储集中器最新发送的消息->由于少数客户端在接收发送的消息后会出现掉线，需要缓存刚刚发送的消息给集中器
     * @param center
     * @param msgBody
     */
    public static void writeAndFlush(Center center,InternalMsgBody msgBody){
        ByteBuf buf = Unpooled.copiedBuffer(msgBody.toBytes());
        ChannelFuture f = center.getCtx().writeAndFlush(buf);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                System.out.println("写指令完成！");
                assert f==future;
            }
        });
        printMsgLog(msgBody);
        center.setLatestMsg(msgBody);
    }
}
