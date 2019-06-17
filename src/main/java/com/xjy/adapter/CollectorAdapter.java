package com.xjy.adapter;

import com.xjy.entity.Collector;
import com.xjy.pojo.DBCollector;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:14 2018/11/1
 * @Description:适配器类，实现数据库DBCollector与程序逻辑Collector的互转
 */
public class CollectorAdapter {
    public static Collector getCollector(DBCollector dbCollector){
        Collector collector = new Collector();
        collector.setId(dbCollector.getAddress());
        return collector;
    }
}
