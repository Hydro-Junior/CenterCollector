package com.xjy.entity;

import com.xjy.util.ConvertUtil;
import com.xjy.util.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:50 2018/10/28
 * @Description: 内部协议以“页”的形式组织资料
 */
public class CenterPage {
    private static final int DATA_SIZE = 385;
    private int[] data = new int[DATA_SIZE];//页数后紧跟的数据 采集器页 64*6 表资料页 32*12

    /**
     * 根据传入的集中器生成所有页的资料
     *
     * @param center
     * @return
     */
    public static List<CenterPage> generateCenterPages(Center center) {
        List<CenterPage> res = new ArrayList<>();
        //写第一页，采集器页
        List<Collector> collectors = center.getCollectors();
        CenterPage page1 = new CenterPage();
        page1.data[0] = 1;
        int curIdx = 1;
        for (Collector collector : collectors) {
            String address = collector.getId();
            int[] addrArray = getAdderssArray(address);
            for (int i = 0; i < 6; i++) {
                page1.data[curIdx++] = addrArray[i];
                if (curIdx == DATA_SIZE - 1) break; //此页写满，严格来讲采集器页不需要此条语句
            }
        }
        res.add(page1);
        //写表页
        int pageNum = 2;
        CenterPage curPage = new CenterPage();
        curPage.data[0] = pageNum;
        curIdx = 1;
        for (Collector collector : collectors) {
            List<Meter> meters = collector.getMeters();
            for (Meter meter : meters) {
                int nodeIndex = meter.getCollectorIndex(); //表所属的采集器（节点）序号
                int[] addrArray = getAdderssArray(meter.getId());//6字节的地址信息
                curPage.data[curIdx++] = nodeIndex;
                for (int i = 0; i < 6; i++) {
                    curPage.data[curIdx++] = addrArray[i];
                }
                curIdx += 4; //跳过4位读数
                curPage.data[curIdx++] = 0x4f;//阀门状态默认为开
                if (curIdx == DATA_SIZE) { //1页写满
                    pageNum += 1;
                    curIdx = 0;
                    res.add(curPage);
                    curPage = new CenterPage();
                    curPage.data[curIdx++] = pageNum;
                }
            }
        }
        //写完所有表，添加最后一页(未把最后一页写满的情况）
        if (curIdx > 1) {
            res.add(curPage);
        }
        for(int i = 0 ; i < collectors.size(); i++){
            collectors.get(i).getMeters().clear(); //清空表结构，释放少量多余内存
        }
        return res;
    }

    public static int[] getAdderssArray(String address) {
        address = address.trim();
        int[] arr = new int[6];
        if(address.length() != 12){
            LogUtil.DataMessageLog(CenterPage.class,"非12位长度地址："+address);
        }
        while (address.length() < 12) {
            address = "0" + address;
        }
        for (int i = 0; i < 6; i++) {
            String twoUnits = address.substring(i * 2, i * 2 + 2);
            arr[6 - i - 1] = Integer.parseInt(twoUnits, 16);
        }
        return arr;
    }

    public int[] getData() {
        return data;
    }

    public void setData(int[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\nPage[" + data[0] + "]:");
        if (data[0] == 1) {//输出采集器页
            for (int i = 0; i < 64; i++) {
                for (int j = 0; j < 6; j++) {
                    sb.append(ConvertUtil.fixedLengthHex(data[1+i * 6 + j]) + " ");
            }
               sb.append("\r\n");
            }
        } else {//输出表资料页
            for (int i = 0; i < 32; i++) {
                for (int j = 0; j < 12; j++) {
                    sb.append(ConvertUtil.fixedLengthHex(data[1+i * 12 + j]) + " ");
                }
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }
}
