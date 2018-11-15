package com.xjy.pojo;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:38 2018/10/31
 * @Description:对应数据库device表的实体类(原来的命名确实很不规范，但改动成本较大)
 */
public class DBMeter {
    private Integer id;
    private String name;
    private Integer indexNo;
    private String iAddr;
    private Double showValue;
    private Integer StrobeStatue;
    private Integer statue;
    private Integer BigDeviceFlag;
    private String enprNo;

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

    public Integer getIndexNo() {
        return indexNo;
    }

    public void setIndexNo(Integer indexNo) {
        this.indexNo = indexNo;
    }

    public String getiAddr() {
        return iAddr;
    }

    public void setiAddr(String iAddr) {
        this.iAddr = iAddr;
    }

    public Double getShowValue() {
        return showValue;
    }

    public void setShowValue(Double showValue) {
        this.showValue = showValue;
    }

    public Integer getStrobeStatue() {
        return StrobeStatue;
    }

    public void setStrobeStatue(Integer strobeStatue) {
        StrobeStatue = strobeStatue;
    }

    public Integer getStatue() {
        return statue;
    }

    public void setStatue(Integer statue) {
        this.statue = statue;
    }

    public Integer getBigDeviceFlag() {
        return BigDeviceFlag;
    }

    public void setBigDeviceFlag(Integer bigDeviceFlag) {
        BigDeviceFlag = bigDeviceFlag;
    }

    public String getEnprNo() {
        return enprNo;
    }

    public void setEnprNo(String enprNo) {
        this.enprNo = enprNo;
    }

    @Override
    public String toString() {
        return "DBMeter{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", indexNo=" + indexNo +
                ", iAddr='" + iAddr + '\'' +
                ", showValue=" + showValue +
                ", StrobeStatue=" + StrobeStatue +
                ", statue=" + statue +
                ", BigDeviceFlag=" + BigDeviceFlag +
                ", enprNo='" + enprNo + '\'' +
                '}';
    }
}
