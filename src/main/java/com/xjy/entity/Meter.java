package com.xjy.entity;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:39 2018/10/17
 * @Description:水表实体类
 */
public class Meter {
    private String id;
    private Integer indexNo;
    private Double value = 0.0;
    private Integer state = 0; //通信状态 0-正常 1-数据读取失败 2 - 采集器失败
    private Integer valveState = 0; //阀门状态 0-无阀 1-开阀 2-关阀
    private Collector collector; //所属采集器
    private Integer collectorIndex; // 针对内部协议：采集器索引，下载档案时用到（动态数据）
    boolean valveStateChanged;

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getIndexNo() {
        return indexNo;
    }

    public void setIndexNo(Integer indexNo) {
        this.indexNo = indexNo;
    }

    public Collector getCollector() {
        return collector;
    }

    public void setCollector(Collector collector) {
        this.collector = collector;
    }

    public Integer getValveState() {
        return valveState;
    }

    public void setValveState(Integer valveState) {
        this.valveState = valveState;
    }

    public boolean isValveStateChanged() {
        return valveStateChanged;
    }

    public void setValveStateChanged(boolean valveStateChanged) {
        this.valveStateChanged = valveStateChanged;
    }

    public Integer getCollectorIndex() {
        return collectorIndex;
    }

    public void setCollectorIndex(Integer collectorIndex) {
        this.collectorIndex = collectorIndex;
    }

    @Override
    public String toString() {
        return "Meter{" +
                "id='" + id + '\'' +
                ", value=" + value +
                ", state=" + state +
                ", valveState=" + valveState +
                ", collector=" + (collector == null? "NULL":collector.getId()) +
                '}';
    }
}
