package com.xjy.processor;

import com.xjy.entity.*;
import com.xjy.util.ConvertUtil;
import com.xjy.util.DBUtil;
import com.xjy.sender.XTProtocolSendHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:55 2019/5/17
 * @Description:
 */
public class XtMsgProcessor {
    public static void readProcessor(Center center, XtMsgBody msgBody) {
        int[] data = msgBody.getData();
        //获得表个数
        //如下解析bcd码是可行的，内部协议就是这么做的，但其实转字符串再转回整数并不是一个高效的方法
        //int num = Integer.parseInt(ConvertUtil.fixedLengthHex(data[1]) + ConvertUtil.fixedLengthHex(data[0]));
        int num = ConvertUtil.bcdBytesToInt(data,0,1);
        List<MeterOf130> meters = XTProtocolSendHelper.constructAndGetMetersInfo(center);
        //接下来每6个字节表示一个表的数据信息
        List<Meter> tempMeterData = new ArrayList<>(); //暂时存储表数据集合
        for(int i = 2; (i + 6) <= data.length; i += 6){
            int index = ConvertUtil.bcdBytesToInt(data, i, i+1);
            int preValue = ConvertUtil.bcdBytesToInt(data, i+2, i+5);
            double value = preValue * 1.0 / 100.0;
            System.out.println("表序号："+ index + "    读数："+value);
            //实际上，用上map查指定index的表效率较高，但基于表也不多，list轮询可以接受
            for(MeterOf130 m : meters){
                if(m.getIndexNo() == index){
                    m.setValue(value);
                    m.setState(0);
                    tempMeterData.add(m);
                    break;
                }
            }
        }
        DBUtil.batchlyRefreshData(tempMeterData,center);
    }


}
