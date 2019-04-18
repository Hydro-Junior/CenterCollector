package com.xjy.processor;

import com.xjy.entity.*;
import com.xjy.parms.CommandState;
import com.xjy.parms.CommandType;
import com.xjy.parms.InternalOrders;
import com.xjy.pojo.DBMeter;
import com.xjy.pojo.DeviceTmp;
import com.xjy.util.ConvertUtil;
import com.xjy.util.DBUtil;
import com.xjy.util.InternalProtocolSendHelper;
import com.xjy.util.LogUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.xjy.entity.GlobalMap.getBasicInfo;
import static com.xjy.util.InternalProtocolSendHelper.readNextPage;
import static com.xjy.util.InternalProtocolSendHelper.writePage;

/**
 * @Author: Mr.Xu
 * @Date: Created in 8:38 2018/10/18
 * @Description: 内部协议相关处理方法集合
 */
public class InternalMsgProcessor {
    //内部协议的读页过程
    public static void readProcessor(Center center, InternalMsgBody msgBody) {
        int[] datas = msgBody.getEffectiveBytes();
        Command curCommand = center.getCurCommand();
        List<Meter> tempMeterData = new ArrayList<>();
        if (datas.length <= 3) {
            //报文传输失误
            curCommand.setState(CommandState.FAILED);
            DBUtil.updateCommandState(CommandState.FAILED,center);
            return;
        }
        int pageNum = datas[3];
        System.out.println("读到了第"+pageNum+"页数据");
        if(pageNum == 0){ //第0页，读取集中器基本信息
            DetailedInfoOfCenter detailedInfo = new DetailedInfoOfCenter();
            char st[] = new char[16]; //定时抄表时间
            char ct[] = new char[16]; //设备时钟
            char ht[][] = new char[12][16]; //历史记录
            int curIndex = 4 + 32;
            //解析定时抄表时间
            for(int j = 0; j < st.length; j++){
                st[j] = (char) datas[curIndex++];
            }
            //设备时钟
            for(int j = 0; j < ct.length; j++){
                ct[j] = (char) datas[curIndex++];
            }
            for(int j = 0; j < ht.length; j++){
                for(int k = 0; k < ht[0].length; k++){
                    ht[j][k] = (char) datas[curIndex++];
                }
            }
            detailedInfo.setTimingCollect(String.valueOf(st));
            detailedInfo.setDeviceClock(String.valueOf(ct));
            String[] records = new String[12];
            for(int i = 0 ; i < records.length; i++){
                records[i] = String.valueOf(ht[i]);
            }
            detailedInfo.setHistoryRecord(records);
            center.setInformation(detailedInfo);
            System.out.println("读完集中器基本信息：");
            System.out.println(center.getInformation());

            center.getCurCommand().setState(CommandState.SUCCESSED);
            DBUtil.updateCommandState(CommandState.SUCCESSED,center);
            //读取采集器页
            readNextPage(center,pageNum);
        }else if(pageNum == 1){ //第1页，读取采集器资料
            List<Collector> collectors = center.getCollectors();
            collectors.clear();
            boolean keepReading ;
            for(int i = 4; i < datas.length; ){
                keepReading = false;
                if(i + 6 > datas.length) break;
                int[] collectorData = new int[6];
                for(int j = 0; j < collectorData.length; j++){
                    collectorData[j] = datas[i++];
                    if(collectorData[j] != 0){
                        keepReading = true;
                    }
                }
                if(!keepReading && i > 9){ //这里i > 9 是因为不排除采集器地址全为0的情况，保留至少一个采集器
                   break; //比特全部为0，表明已经加载完采集器信息，跳出循环直接发送读取下一页指令
                }else{//解析
                    String collectorAddress = ConvertUtil.fixedLengthHex(collectorData[5])
                            + ConvertUtil.fixedLengthHex(collectorData[4])
                            + ConvertUtil.fixedLengthHex(collectorData[3])
                            + ConvertUtil.fixedLengthHex(collectorData[2])
                            + ConvertUtil.fixedLengthHex(collectorData[1])
                            + ConvertUtil.fixedLengthHex(collectorData[0]);
                    Collector collector = new Collector();
                    collector.setId(collectorAddress);
                    collector.setCenter(center);
                    collectors.add(collector);
                }
            }
            //读采集器结束(是读取集中器信息的一部分，但无关紧要，置命令成功后再读取)
            LogUtil.DataMessageLog(InternalMsgProcessor.class,"读取采集器信息结束");
            //非定时读取，需要读取下一页
            if(center.getCurCommand() != null && center.getCurCommand().getType()==CommandType.READ_ALL_METERS)
                readNextPage(center,pageNum);
        } else{
            boolean keepReading = false;
            for (int i = 4; i < datas.length; ) {//省去前3个指令字和页码，接下来12个为一组
                keepReading = false;
                if (i + 12 > datas.length) break;
                int[] meterData = new int[12];
                for (int j = 0; j < meterData.length; j++) {
                    meterData[j] = datas[i++];
                    if (meterData[j] != 0) {
                        keepReading = true;
                    }
                }
                if (!keepReading) {//12位信息全为0，表明已经全部读完，跳出循环
                    break;
                } else { //解析
                    //int collectIdx = meterData[0];//采集器的序号，暂时用不到，注释保留
                    Meter meter = new Meter();
                    double meterValue = 0.0;
                    String meterAddress = ConvertUtil.fixedLengthHex(meterData[6])
                            + ConvertUtil.fixedLengthHex(meterData[5])
                            + ConvertUtil.fixedLengthHex(meterData[4])
                            + ConvertUtil.fixedLengthHex(meterData[3])
                            + ConvertUtil.fixedLengthHex(meterData[2])
                            + ConvertUtil.fixedLengthHex(meterData[1]);
                    try{
                        meterValue = Double.parseDouble(ConvertUtil.fixedLengthHex(meterData[10])
                                + ConvertUtil.fixedLengthHex(meterData[9])
                                + ConvertUtil.fixedLengthHex(meterData[8]) + "."
                                + ConvertUtil.fixedLengthHex(meterData[7]));
                    }catch (Exception e){
                            LogUtil.DataMessageLog(InternalMsgProcessor.class,"表读数解析异常！      表地址："+meterAddress);
                            e.printStackTrace();
                    }

                    meter.setId(meterAddress);
                    meter.setValue(meterValue);
                    if (meterData[11] == 0x4f) {
                        meter.setState(0);
                        meter.setValveState(1);//开阀
                    } else if (meterData[11] == 0x43) {
                        meter.setState(0);
                        meter.setValveState(2); //关阀
                    } else if (meterData[11] == 0x4A) {//采集器异常
                        meter.setState(2);
                    } else {//读表失败
                        meter.setState(1);
                    }
                    tempMeterData.add(meter);
                    //表的信息还是放在持久层吧，这里不添加到center中
                    /*try {
                        center.getCollectors().get(collectIdx).getMeters().add(meter);
                    }catch (Exception e){//可能采集器信息尚未初始化
                    }*/
                    //如果是单个表读取，判断表地址是否与参数中的表地址相同，如果相同，直接更新数据库后不做其他处理
                    if(curCommand != null && curCommand.getType()== CommandType.READ_SINGLE_METER && curCommand.getArgs()[2].equals(meterAddress)){
                        DBUtil.refreshMeterData(meter,center);//将表读数插入数据库，今日已有抄过则更新该条记录
                        center.getCurCommand().setState(CommandState.SUCCESSED);
                        DBUtil.updateCommandState(CommandState.SUCCESSED,center);
                        return;
                    }
                }
            }
            DBUtil.batchlyRefreshData(tempMeterData,center);
            int totalPage ;
            ConcurrentHashMap<Center,List<CenterPage>> info =  GlobalMap.getBasicInfo();
            if(!info.containsKey(center)){
                InternalProtocolSendHelper.constructPages(center);
            }
            info = GlobalMap.getBasicInfo();
            totalPage = info.get(center).size();
            System.out.println("集中器"+center.getId()+"总页数:"+totalPage);
            if(!keepReading || pageNum == totalPage){ //如果已经读完
                if(center.getCurCommand() != null  && center.getCurCommand().getType()==CommandType.READ_ALL_METERS){
                    center.getCurCommand().setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED,center);
                }
                DBUtil.updateCenterReadTime(center);//更新集中器最后一次读取时间为系统当前时间
            }else{
                readNextPage(center,pageNum);
            }
        }
    }

    /**
     * 下载档案命令的处理器，先读取页数，判断要不要写下一页
     * @param currentCenter
     * @param msgBody
     */
    public static void writeProcessor(Center currentCenter, InternalMsgBody msgBody) {
        int[] datas = msgBody.getEffectiveBytes();
        if (datas.length <= 3) {
            //报文传输错误
            currentCenter.getCurCommand().setState(CommandState.FAILED);
            DBUtil.updateCommandState(CommandState.FAILED,currentCenter);
            return;
        }
        int pageNum = datas[3];
        System.out.println("数据返回的当前页："+pageNum+"\r\n"+"参数记录的当前页："+currentCenter.getCurCommand().getParameter());
        pageNum = (int)currentCenter.getCurCommand().getParameter();
        ConcurrentHashMap<Center,List<CenterPage>> infoMap = getBasicInfo();
        int total = infoMap.get(currentCenter).size();
        System.out.println("待写总页数："+total);
        if(infoMap.get(currentCenter).size() == pageNum){
            currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
            DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
        }else{
            currentCenter.getCurCommand().setParameter(pageNum+1);
            writePage(currentCenter,pageNum+1);
            System.out.println("开始写下一页");
        }
    }

    //获取开关阀信息并写入数据库
    public static void getValveInfo(Center currentCenter, InternalMsgBody msgBody) {
        String meterAddress = currentCenter.getCurCommand().getArgs()[2];
        if(msgBody.getInstruction().equals(InternalOrders.OPEN_VALVE)){
            currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
            DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
            DBUtil.updateValveState(1,meterAddress,currentCenter);
        }else if(msgBody.getInstruction().equals(InternalOrders.CLOSE_VALVE)){
            currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
            DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
            DBUtil.updateValveState(2,meterAddress,currentCenter);
        }
    }
    //开关阀后读表返回的开关阀成功与失败
    public static void afterUpdateValveState (Center currentCenter, InternalMsgBody msgBody) {
        try {
            //LogUtil.channelLog(currentCenter.getId() ,"进入开关阀状态更新方法！");
            Command c = currentCenter.getCurCommand();
            if(c == null || !(c.getType() == CommandType.CLOSE_VALVE || c.getType()==CommandType.OPEN_VALVE)){
                //LogUtil.channelLog(currentCenter.getId(),"提前返回！");
                return;
            }
            String meterAddress = currentCenter.getCurCommand().getArgs()[2];
            //if(currentCenter.getDbId() == null ) DBUtil.preprocessOfRead(currentCenter);
            //DBMeter meter = DBUtil.getDBMeter(currentCenter.getDbId(),meterAddress);
            int[] data = msgBody.getEffectiveBytes();
            StringBuilder sb = new StringBuilder();
            for(int i = 0 ; i < data.length; i++){
                sb.append(ConvertUtil.fixedLengthHex(data[i])+" ");
            }
            LogUtil.channelLog(currentCenter.getId() ,"the read bytes：\r\n"+ sb.toString());
            //实际上返回报文中还有表地址、表读数信息，暂不做处理
            if(data[19] != 'C' && data[19] != 'O'){//开关阀失败
                LogUtil.DataMessageLog(InternalMsgProcessor.class, "valve operation failed!");
                c.setState(CommandState.FAILED);
                DBUtil.updateCommandState(CommandState.FAILED,currentCenter);
            }else{
                System.out.println("data state code :"+ Integer.toHexString(data[19]) +",change the state of the valve");
                if(c.getType()== CommandType.OPEN_VALVE && data[19] == 'O'){
                    //currentCenter.getCurCommand().setState(CommandState.SUCCESSED); 暂时先不更新命令的逻辑状态
                    DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
                    DBUtil.updateValveState(1,meterAddress,currentCenter);
                }else if(c.getType() == CommandType.CLOSE_VALVE && data[19] == 'C'){
                    //currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
                    DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
                    DBUtil.updateValveState(2,meterAddress,currentCenter);
                }else{
                    c.setState(CommandState.FAILED);
                    DBUtil.updateCommandState(CommandState.FAILED,currentCenter);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
