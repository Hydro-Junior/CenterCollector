package com.xjy.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:52 2018/10/17
 * @Description: 采集器实体类
 */
public class Collector {
    private String id;
    private List<Meter> meters = new ArrayList<>();
    private Center center; //所属集中器

    public Collector(){}

    public Collector(String id, Center center) {
        this.id = id;
        this.center = center;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Meter> getMeters() {
        return meters;
    }

    public void setMeters(List<Meter> meters) {
        this.meters = meters;
    }

    public Center getCenter() {
        return center;
    }

    public void setCenter(Center center) {
        this.center = center;
    }

    @Override
    public String toString() {
        return "Collector{" +
                "id='" + id + '\'' +
                ", meters=" + meters +
                ", center=" + (center == null? "NULL":center.getId()) +
                '}';
    }
}
