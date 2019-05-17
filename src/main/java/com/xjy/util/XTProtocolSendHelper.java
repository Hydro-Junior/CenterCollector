package com.xjy.util;

import com.xjy.adapter.CollectorAdapter;
import com.xjy.adapter.MeterAdapter;
import com.xjy.entity.*;
import com.xjy.parms.XTParams;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBMeter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:03 2019/5/14
 * @Description:
 */
public class XTProtocolSendHelper {


    /**
     * 读取档案，根据130（网络版，文库可搜到，数据单元标识为00 00 01 00，4个字节）
     * @param center 集中器对象
     * @param currentCommand 当前命令
     */
    public static void getFileInfo(Center center, Command currentCommand) {
        /**
         * 要发送命令，先确定控制域、地址域、以及功能码
         */
        // 1. 控制域由协议规则得出，也可直接参考新天通讯协议所述的控制域
        //    新天通讯协议中，控制域为4b，验证过程如下
        int dir = XTParams.DIR_SERVER_TO_CENTER ; //是从服务器到集中器的报文
        int prm = XTParams.PRM_MASTER; // 是启动站
        int func = XTParams.CTRL_FOR_DATA; // 功能是为了获得数据
        int C = XtControlArea.generateControlArea(dir,prm,func); //这里C即0x4b

        //2. 从集中器地址确定地址域 （即XtMsgBody中的getCenterAddress的反向操作）
        String address = center.getId();
        int[] A = getAddressArea(address); //长度为5

        //3. 功能码
        int AFN = XTParams.AFN_GET_PARAM;

        //4. 帧序列域
        int SEQ = 0x60; //一般是单帧，不需要确认 0110 0000
        //int seq = XTParams.SEQ_IS_FIRST_FRAME << 6 | XTParams.SEQ_IS_FINAL_FRAME << 5 | (~XTParams.SEQ_NEED_CHECK);

        //5. 数据单元
        int f = 1; //读取档案，Fn = 1；
        int[] Fn = getFn(f); //长度为4

        //6. 构造数据体中表个数与表序号，转为BCD码
        // 每个数据2字节，如50块表，其格式就是 50 00; 如序号为125，则格式为 25 01;
        int offset = (Integer) currentCommand.getParameter() == null? 0 : (Integer) currentCommand.getParameter(); //命令参数为表序号的偏置量
        List<MeterOf130> meters = constructAndGetMetersInfo(center);
        int num = meters.size()- offset >= 10 ? 10 : meters.size() - offset; //表的总数
        currentCommand.setParameter(offset+ num);
        int[] data = new int[num * 2 + 2];
        data[1] = 0x00;
        data[0] = (((num / 10) << 4) & 0xf0) |((num % 10) & 0x0f);
        for(int i = 1; i <= num; i++){
            Meter meter = meters.get(i - 1 + offset);
            Integer index = meter.getIndexNo();
            System.out.println(index);
            if(index == null) {
                LogUtil.DataMessageLog(XTProtocolSendHelper.class,"表序号错误！+ 集中器编号："+
                        center.getId() + "表地址："+ meter.getId());
                continue;
            }
            arrangeBcdCodeIn2Bytes(index,2 * i , data);
        }
        //7. 整理数据并发送
        XtMsgBody msgBody  = new XtMsgBody(C, A, AFN, SEQ, Fn, data);
        System.out.println(msgBody);
        System.out.println(ConvertUtil.fixedLengthHex(ConvertUtil.bytesToInts(msgBody.toBytes())));
        writeAndFlush(center,msgBody);
    }
    /**
     * 读取单个表
     * @param center
     * @param currentCommand
     */
    public static void readSingleMeter(Center center, Command currentCommand) {
       String meterAddress =  currentCommand.getArgs()[2];

    }

    /**
     * 读表
     * @param center
     * @param currentCommand
     */
    public static void readMeters(Center center, Command currentCommand) {
        int C = XtControlArea.generateControlArea(XTParams.DIR_SERVER_TO_CENTER,XTParams.PRM_MASTER,XTParams.CTRL_FOR_DATA);
        int[] A = getAddressArea(center.getId());
        int AFN = XTParams.AFN_GET_REALTIME_DATA;
        int SEQ = 0x60;
        int[] Fn = getFn(57); //根据协议获得 F57：当前正向有功总电能、累计水量、累计气量
        int offset = (Integer) currentCommand.getParameter()==null? 0 : (Integer) currentCommand.getParameter();
        List<MeterOf130> meters = constructAndGetMetersInfo(center);
        int num = meters.size()- offset >= 5 ? 5 : meters.size() - offset; //表的总数
        currentCommand.setParameter(offset + num);
        //数据单元字节数 抄表方式1字节(0x00) + 表数量2字节 + 表序号 2 * n
        int[] data = new int[3 + 2 * num];
        data[0] = 0x00;
        arrangeBcdCodeIn2Bytes(num,1, data);
        for(int i = 1; i <= num ; i++){
            arrangeBcdCodeIn2Bytes(meters.get(offset+i-1).getIndexNo(),1+2 * i,data);
        }
        //7. 整理数据并发送
        XtMsgBody msgBody  = new XtMsgBody(C, A, AFN, SEQ, Fn, data);
        System.out.println(msgBody);
        System.out.println(ConvertUtil.fixedLengthHex(ConvertUtil.bytesToInts(msgBody.toBytes())));
        writeAndFlush(center,msgBody);
    }
    //把某个数值的bcd码分配到两个字节，低位在前
    public static void arrangeBcdCodeIn2Bytes(int num, int offset, int[] data){
        int high = num % 100;
        int low = num / 100;
        data[offset] = ConvertUtil.getBcdOf2digit(high);
        data[offset + 1] = ConvertUtil.getBcdOf2digit(low);
    }

    public static List<MeterOf130> constructAndGetMetersInfo(Center center) {
        ConcurrentHashMap<Center,List<MeterOf130>> map = GlobalMap.get130MeterInfo();
        //查找数据库中该集中器对应的采集器;
        if(map.containsKey(center)) return map.get(center);
        constructMetersInfo(center);
        return map.get(center);
    }
    public static void constructMetersInfo(Center center){
        ConcurrentHashMap<Center,List<MeterOf130>> map = GlobalMap.get130MeterInfo();
        List<DBCollector> dbcollectors = DBUtil.getCollectorsByCenter(center);
        List<Collector> collectors = new ArrayList<>();
        List<MeterOf130> totalMeters = new ArrayList<>();
        //遍历采集器集合，查询获得总表集合，构建集中器资料
        for(int i = 0 ; i < dbcollectors.size(); i++){
            DBCollector dbCollector = dbcollectors.get(i);
            Collector theCollector = CollectorAdapter.getCollector(dbCollector);
            theCollector.setCenter(center);
            collectors.add(theCollector);
            List<DBMeter> dbMeters = DBUtil.getMetersByCollector(dbcollectors.get(i));
            List<Meter> meters = new ArrayList<>();
            for(DBMeter dbMeter : dbMeters){
                MeterOf130 theMeter = MeterAdapter.getMeterOf130(dbMeter);
                //theMeter.setCollectorIndex(i);//设置对应采集器序号 130中用不上
                theMeter.setCollector(theCollector); //设置所属采集器
                meters.add(theMeter);
            }
            theCollector.setMeters(meters);//更新每个采集器的表资料
            for(Meter m : meters){
                totalMeters.add((MeterOf130) m );
            }
        }
        Collections.sort(totalMeters, Comparator.comparing(MeterOf130::getIndexNo));
        center.setCollectors(collectors);//更新集中器的采集器资料
        //构建集中器的表信息
        map.put(center,totalMeters);
    }

    public static int[] getFn(int f) {
        int[] fn = new int[4];
        //根据协议和实际情况，DA部分默认为0，考虑DT部分
        int DT2 = (f - 1) / 8;
        int DT1;
        int rest = f % 8;
        if(rest == 0) DT1 = 0x80;
        else DT1 = 1 << (rest - 1);
        fn[2] = DT1;
        fn[3] = DT2;
        return fn;
    }

    public static int[] getAddressArea(String address) {
        /*public String getCenterAddress() {
            return ConvertUtil.fixedLengthHex(A1[0])  + ConvertUtil.fixedLengthHex(A1[1])
                    + String.format("%05d", (A2[0] << 8 | A2[1]));
        }*/
        if(address == null || address.length() != 9){
            LogUtil.DataMessageLog(XTProtocolSendHelper.class,"invalid center address!");
            return null;
        }
        int[] A = new int[5];
        int[] A1 = new int[2];
        int[] A2 = new int[2];
        //int A3 = 0; //对于A3的值，本协议默认为00;
        A1[0] = Integer.parseInt(address.substring(0,2),16);
        A1[1] = Integer.parseInt(address.substring(2,4),16);
        A2[0] = (Integer.parseInt(address.substring(4,9)) >>> 8) & 0xff;
        A2[1] = Integer.parseInt(address.substring(4,9)) & 0xff;
        A[0] = A1[1]; A[1] = A1[0]; A[2] = A2[1]; A[3] = A2[0]; //低位优先
        return A;
    }
    public static void writeAndFlush(Center center,XtMsgBody msgBody){
        ByteBuf buf = Unpooled.copiedBuffer(msgBody.toBytes());
        ChannelFuture f = center.getCtx().writeAndFlush(buf);
        printMsgLog(msgBody);
        center.setLatestMsg(msgBody);
    }
    /**
     * 打印消息发送日志
     * @param xtMsgBody
     */
    private static void printMsgLog(XtMsgBody xtMsgBody){
        LogUtil.DataMessageLog(InternalProtocolSendHelper.class,"待发送报文：\n");
        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < xtMsgBody.toBytes().length; i++){
            sb.append(ConvertUtil.fixedLengthHex(xtMsgBody.toBytes()[i])+" ");
            if(i !=0 && i % 30 == 0) sb.append("\r\n");
        }
        String channelLogContent = "[MESSAGE SEND]{\r\n" + sb.toString()+"}";
        try {
            LogUtil.channelLog(xtMsgBody.getCenterAddress(),channelLogContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtil.DataMessageLog(XTProtocolSendHelper.class, sb.toString());
    }
    public static void setFileInfo(Center center, Command currentCommand, int i) {

    }

}
