package com.xjy.adapter;

import com.xjy.entity.Meter;
import com.xjy.pojo.DBMeter;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:17 2018/11/1
 * @Description:
 */
public class MeterAdapter {
    public static Meter getMeter(DBMeter dbMeter){
        Meter meter = new Meter();
        meter.setId(dbMeter.getiAddr());
        meter.setIndexNo(dbMeter.getIndexNo());
        meter.setValue(dbMeter.getShowValue());
        meter.setState(dbMeter.getStatue());
        return  meter;
    }
}
