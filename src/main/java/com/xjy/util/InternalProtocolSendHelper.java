package com.xjy.util;

import com.xjy.adapter.CollectorAdapter;
import com.xjy.adapter.MeterAdapter;
import com.xjy.entity.*;
import com.xjy.parms.InternalOrders;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBMeter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
    //内部协议，读取下一页
    public static void readNextPage(Center center, int currentPageNum){
        if(currentPageNum == -1){//开始读取之前，获取center在数据库中的id和水司编号，应对修改数据库中表具资料后的情况
            DBUtil.preprocessOfRead(center);
        }
        int[] effectiveData = new int[3 + 1];//指令字3个字节+页面号1个字节
        for (int i = 0; i < effectiveData.length; i++) {
            if (i < 3) effectiveData[i] = InternalOrders.READ.getBytes()[i];
            else effectiveData[i] = currentPageNum + 1;
        }
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), effectiveData);
        ByteBuf buf = Unpooled.copiedBuffer(internalMsgBody.toBytes());
        center.getCtx().writeAndFlush(buf);
        printMsgLog(internalMsgBody);
        System.out.println("发送读取下一页的命令");
    }
    //内部协议，读取首页
    public static void readFirstPage(Center center){
        readNextPage(center,-1);
    }
    //采集
    public static void collect(Center center){

    }

    /**
     * 写入资料,采集器所在页，在这个方法中，构建好集中器所有页的数据资料,同时更新集中器状态map中的资料
     * @param center 集中器
     */
    public static void writeFirstPage(Center center){
        LogUtil.DataMessageLog("开始执行写页指令");

        ConcurrentHashMap<Center,List<CenterPage>> persistentDataOfInternalProtocol = GlobalMap.getBasicInfo();
        //查找数据库中该集中器对应的采集器;
        List<DBCollector> dbcollectors = DBUtil.getCollectorsByCenter(center);
        List<Collector> collectors = new ArrayList<>();
        //遍历采集器集合，查询获得总表集合，构建集中器资料
        for(int i = 0 ; i < dbcollectors.size(); i++){
            DBCollector dbCollector = dbcollectors.get(i);
            Collector theCollector = CollectorAdapter.getCollector(dbCollector);
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

        LogUtil.DataMessageLog("页资料:\n"+pages.toString());

        persistentDataOfInternalProtocol.put(center,pages);
        //发送第一页（采集器页）
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), addInstruction(pages.get(0).getData(),InternalOrders.DOWNLOAD));

        //调试日志
        printMsgLog(internalMsgBody);

        ByteBuf buf = Unpooled.copiedBuffer(internalMsgBody.toBytes());
        center.getCtx().writeAndFlush(buf);
    }
    public static void writePage(Center center,int page){
        //从persistentDataOfInternalProtocol中获取center相应页的资料，构建msg发送即可
        ConcurrentHashMap<Center,List<CenterPage>> persistentDataOfInternalProtocol = GlobalMap.getBasicInfo();
        List<CenterPage> pages = persistentDataOfInternalProtocol.get(center);
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), addInstruction(pages.get(page-1).getData(),InternalOrders.DOWNLOAD));

        //调试日志,打印待发送报文
        printMsgLog(internalMsgBody);

        ByteBuf buf = Unpooled.copiedBuffer(internalMsgBody.toBytes());
        center.getCtx().writeAndFlush(buf);
    }
    private static int[] addInstruction(int[] originData,String instruction){
        int[] res = new int[originData.length + 3];
        for(int i = 0 ; i < 3; i++){
            res[i] = instruction.charAt(i);
        }
        System.arraycopy(originData,0,res,3,originData.length);
        return res;
    }
    private static void printMsgLog(InternalMsgBody internalMsgBody){
        LogUtil.DataMessageLog("待发送报文：\n");
        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < internalMsgBody.toBytes().length; i++){
            sb.append(ConvertUtil.fixedLengthHex(internalMsgBody.toBytes()[i])+" ");
            if(i !=0 && i % 30 == 0) sb.append("\r\n");
        }
        LogUtil.DataMessageLog(sb.toString());
    }
}
