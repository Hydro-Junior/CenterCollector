package com.xjy.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:11 2018/10/31
 * @Description: 对应数据库采集器表的实体类
 */
public class DBCollector {
    private Integer id;
    private String name;
    private String address;
    private String enprNo;
    private Integer isUse;
    private Integer runStatue;
    private Integer centerId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEnprNo() {
        return enprNo;
    }

    public void setEnprNo(String enprNo) {
        this.enprNo = enprNo;
    }

    public Integer getIsUse() {
        return isUse;
    }

    public void setIsUse(Integer isUse) {
        this.isUse = isUse;
    }

    public Integer getRunStatue() {
        return runStatue;
    }

    public void setRunStatue(Integer runStatue) {
        this.runStatue = runStatue;
    }

    public Integer getCenterId() {
        return centerId;
    }

    public void setCenterId(Integer centerId) {
        this.centerId = centerId;
    }


    @Override
    public String toString() {
        return "DBCollector{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", centerId=" + centerId +
                '}';
    }
}
