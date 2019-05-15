package com.xjy.util;

import com.xjy.adapter.CollectorAdapter;
import com.xjy.adapter.MeterAdapter;
import com.xjy.entity.*;
import com.xjy.parms.XTParams;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBMeter;

import java.util.ArrayList;
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
     * @param center 集中器
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
        int[] fn = getFn(f); //长度为4

        //6. 构造数据体中表个数与表序号，转为BCD码
        // 每个数据2字节，如50块表，其格式就是 50 00; 如序号为125，则格式为 25 01;
        List<Meter> meters = constructAndGetMetersInfo(center);
        int num = meters.size(); //表的总数
        int[] data = new int[num * 2 + 2];
        //todo
    }

    /**
     * 为了确保内存中的表资料是最新的，建议获得
     */
    public static List<Meter> constructAndGetMetersInfo(Center center) {
        ConcurrentHashMap<Center,List<Meter>> map = GlobalMap.getMeterInfo();
        //查找数据库中该集中器对应的采集器;
        List<DBCollector> dbcollectors = DBUtil.getCollectorsByCenter(center);
        List<Collector> collectors = new ArrayList<>();
        List<Meter> totalMeters = new ArrayList<>();
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
                //theMeter.setCollectorIndex(i);//设置对应采集器序号 130中用不上
                theMeter.setCollector(theCollector); //设置所属采集器
                meters.add(theMeter);
            }
            theCollector.setMeters(meters);//更新每个采集器的表资料
            totalMeters.addAll(meters);
        }
        center.setCollectors(collectors);//更新集中器的采集器资料
        //构建集中器的表信息
        map.put(center,totalMeters);
        return map.get(center);
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
}
