package com.xjy.processor;

import com.xjy.entity.Center;
import com.xjy.entity.Collector;
import com.xjy.entity.InternalMsgBody;
import com.xjy.entity.Meter;
import com.xjy.parms.InternalOrders;
import com.xjy.util.ConvertUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 8:38 2018/10/18
 * @Description: 内部协议相关处理方法集合
 */
public class InternalMsgProcessor {
    //内部协议的读页过程
    public static void readProcessor(Center center, InternalMsgBody msgBody) {
        int[] datas = msgBody.getEffectiveBytes();
        if (datas.length <= 3) return;
        int pageNum = datas[3];
        if(pageNum == 0){ //第0页，读取集中器基本信息

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
            }
        }
    }

    private static void readNextPage(Center center, int currentPageNum){
        int[] effectiveData = new int[3 + 1];//指令字3个字节+页面号1个字节
        for (int i = 0; i < effectiveData.length; i++) {
            if (i < 3) effectiveData[i] = InternalOrders.READ.getBytes()[i];
            else effectiveData[i] = currentPageNum + 1;
        }
        InternalMsgBody internalMsgBody = new InternalMsgBody(center.getId(), effectiveData);
        ByteBuf buf = Unpooled.copiedBuffer(internalMsgBody.toBytes());
        center.getCtx().writeAndFlush(buf);
        System.out.println("发送读取下一页的命令");
        byte[] theBytes = internalMsgBody.toBytes();
        for (int i = 0; i < theBytes.length; i++) {
            System.out.print(ConvertUtil.fixedLengthHex(theBytes[i]) + " ");
        }
    }
}
