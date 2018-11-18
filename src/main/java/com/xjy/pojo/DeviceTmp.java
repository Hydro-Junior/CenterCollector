package com.xjy.pojo;

import java.sql.Timestamp;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:20 2018/11/16
 * @Description:对应数据库deviceTmp表的实体类
 */
public class DeviceTmp {
    String addr;//集中器编号
    Integer readDate;//当月的第几天
    Integer centerID;//集中器在数据库中对应的id
    Double showValue;//表读数
    Double fshowValue;
    Integer meterState;
    Integer commState;
    Integer isUse = 0; //是否使用，默认为0
    Timestamp readTime;
    String enprNo;

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public Integer getReadDate() {
        return readDate;
    }

    public void setReadDate(Integer readDate) {
        this.readDate = readDate;
    }

    public Integer getCenterID() {
        return centerID;
    }

    public void setCenterID(Integer centerID) {
        this.centerID = centerID;
    }

    public Double getShowValue() {
        return showValue;
    }

    public void setShowValue(Double showValue) {
        this.showValue = showValue;
    }

    public Double getFshowValue() {
        return fshowValue;
    }

    public void setFshowValue(Double fshowValue) {
        this.fshowValue = fshowValue;
    }

    public Integer getMeterState() {
        return meterState;
    }

    public void setMeterState(Integer meterState) {
        this.meterState = meterState;
    }

    public Integer getCommState() {
        return commState;
    }

    public void setCommState(Integer commState) {
        this.commState = commState;
    }

    public Integer getIsUse() {
        return isUse;
    }

    public void setIsUse(Integer isUse) {
        this.isUse = isUse;
    }

    public Timestamp getReadTime() {
        return readTime;
    }

    public void setReadTime(Timestamp readTime) {
        this.readTime = readTime;
    }

    public String getEnprNo() {
        return enprNo;
    }

    public void setEnprNo(String enprNo) {
        this.enprNo = enprNo;
    }

    @Override
    public String toString() {
        return "DeviceTmp{" +
                "addr='" + addr + '\'' +
                ", readDate=" + readDate +
                ", centerID=" + centerID +
                ", showValue=" + showValue +
                ", fshowValue=" + fshowValue +
                ", meterState=" + meterState +
                ", commState=" + commState +
                ", isUse=" + isUse +
                ", readTime=" + readTime +
                ", enprNo='" + enprNo + '\'' +
                '}';
    }
}
