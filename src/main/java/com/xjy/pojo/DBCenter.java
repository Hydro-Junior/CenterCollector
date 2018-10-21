package com.xjy.pojo;

import java.sql.Timestamp;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:11 2018/10/20
 * @Description: 与数据库中Center对应，由于希望程序中的Center比较精简，并有命令队列等逻辑字段，而原版数据库设计有些不合理（非本人设计），
 * 过多的冗余字段和不当命名，为了不破坏原Center的精简性，由DBCenter接收数据库传来的数据，自己设计Adapter实现互转，顺便省去了通过Mybatis的
 * ResultMap配置别名。类似的还有Command，Meter（device）等。
 */
public class DBCenter {
    private Integer id;
    private String name;
    private Integer protocolType;
    private String gprsNum;
    private Timestamp readTime;
    private Integer runStatue;
    private String enprNo;
    private Integer portId;
    private Timestamp heartBeatTime;

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

    public Integer getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(Integer protocolType) {
        this.protocolType = protocolType;
    }

    public String getGprsNum() {
        return gprsNum;
    }

    public void setGprsNum(String gprsNum) {
        this.gprsNum = gprsNum;
    }

    public Timestamp getReadTime() {
        return readTime;
    }

    public void setReadTime(Timestamp readTime) {
        this.readTime = readTime;
    }

    public Integer getRunStatue() {
        return runStatue;
    }

    public void setRunStatue(Integer runStatue) {
        this.runStatue = runStatue;
    }

    public String getEnprNo() {
        return enprNo;
    }

    public void setEnprNo(String enprNo) {
        this.enprNo = enprNo;
    }

    public Integer getPortId() {
        return portId;
    }

    public void setPortId(Integer portId) {
        this.portId = portId;
    }

    public Timestamp getHeartBeatTime() {
        return heartBeatTime;
    }

    public void setHeartBeatTime(Timestamp heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }
}
