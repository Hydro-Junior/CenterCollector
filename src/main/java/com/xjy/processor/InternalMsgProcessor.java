package com.xjy.processor;

import com.xjy.entity.*;
import com.xjy.parms.CommandState;
import com.xjy.util.ConvertUtil;
import com.xjy.util.DBUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
        if (datas.length <= 3) {
            //报文传输失误
            DBUtil.updateCommandState(CommandState.FAILED,center);
            return;
        }
        int pageNum = datas[3];
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
            //读采集器结束，读下一页
            System.out.println("读采集器结束，接下来读取表数据");

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
                    int collectIdx = meterData[0];
                    Meter meter = new Meter();
                    String meterAddress = ConvertUtil.fixedLengthHex(meterData[6])
                            + ConvertUtil.fixedLengthHex(meterData[5])
                            + ConvertUtil.fixedLengthHex(meterData[4])
                            + ConvertUtil.fixedLengthHex(meterData[3])
                            + ConvertUtil.fixedLengthHex(meterData[2])
                            + ConvertUtil.fixedLengthHex(meterData[1]);
                    double meterValue = Double.parseDouble(ConvertUtil.fixedLengthHex(meterData[10])
                            + ConvertUtil.fixedLengthHex(meterData[9])
                            + ConvertUtil.fixedLengthHex(meterData[8]) + "."
                            + ConvertUtil.fixedLengthHex(meterData[7])
                    );
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
                    try {
                        center.getCollectors().get(collectIdx).getMeters().add(meter);
                    }catch (Exception e){}
                    //如果是单个表读取，判断表地址是否与参数中的表地址相同，如果相同，直接更新数据库后不做其他处理
                    System.out.println(meter);
                }
            }
            if (keepReading && pageNum < 256) { //发送读取下一页的命令
                readNextPage(center,pageNum);
            } else {//重置集中器的命令执行状态
                System.out.println("命令执行结束");
                //todo 更新数据库中的命令状态
                System.out.println(center);
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
        ConcurrentHashMap<Center,List<CenterPage>> infoMap = GlobalMap.getBasicInfo();
        if(infoMap.get(currentCenter).size() != pageNum){
            writePage(currentCenter,pageNum+1);
        }else{
            currentCenter.getCurCommand().setState(CommandState.SUCCESSED);
            DBUtil.updateCommandState(CommandState.SUCCESSED,currentCenter);
        }
    }
}
