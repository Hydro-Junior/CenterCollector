package com.xjy.adapter;

import com.xjy.entity.Collector;
import com.xjy.pojo.DBCollector;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:14 2018/11/1
 * @Description:
 */
public class CollectorAdapter {
    public static Collector getCollector(DBCollector dbCollector){
        Collector collector = new Collector();
        collector.setId(dbCollector.getAddress());
        return collector;
    }
}
