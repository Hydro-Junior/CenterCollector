package com.xjy.test;

import com.xjy.adapter.CollectorAdapter;
import com.xjy.adapter.MeterAdapter;
import com.xjy.entity.*;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBMeter;
import com.xjy.util.DBUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:04 2018/10/25
 * @Description:
 */
public class SyntaxTest {
    @Test
    public void testStringConstructor(){
        String[] args = new String[]{null,"yes"};
        for(String s : args){
            System.out.println(s);
        }
    }
    @Test
    public void testDoubleToInt(){
        double b = 1.3;
        System.out.println((int)b);
    }
    @Test
    public void testAdressTrans(){
        int[] addr = CenterPage.getAdderssArray("020161201761");
        for(int i = 0 ; i < addr.length ;i++){
            System.out.print(addr[i] + " ");
        }
        System.out.println();
        for(int i = 0; i < addr.length; i++){
            System.out.print(Integer.toHexString(addr[i])+" ");
        }
    }
    @Test
    public void testConstructPages(){
        //查找数据库中该集中器对应的采集器;
        Center center = new Center("00201611251 ",null);
        List<DBCollector> dbcollectors = DBUtil.getCollectorsByCenter(center);
        List<Collector> collectors = new ArrayList<>();
        //遍历采集器集合，查询获得总表集合，构建集中器资料
        for(int i = 0 ; i < dbcollectors.size(); i++){
            DBCollector dbCollector = dbcollectors.get(i);
            Collector theCollector = CollectorAdapter.getCollector(dbCollector);
            collectors.add(theCollector);
            List<DBMeter> dbMeters = DBUtil.getMetersByCollector(dbcollectors.get(i));

            System.out.println(theCollector);

            List<Meter> meters = new ArrayList<>();
            for(DBMeter dbMeter : dbMeters) {
                Meter theMeter = MeterAdapter.getMeter(dbMeter);
                theMeter.setCollectorIndex(i);//设置对应采集器序号
                theMeter.setCollector(theCollector); //设置所属采集器

                System.out.print(theMeter+" ");

                meters.add(theMeter);
            }
            theCollector.setMeters(meters);//更新每个采集器的表资料
        }
        center.setCollectors(collectors);//更新集中器的采集器资料
        //按页构建资料
        List<CenterPage> pages = CenterPage.generateCenterPages(center);
        System.out.println(pages);
    }
}
