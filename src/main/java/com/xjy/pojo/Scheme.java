package com.xjy.pojo;

/**
 * @Author: Mr.Xu
 * @Date: Created in 9:42 2018/12/7
 * @Description:
 */
public class Scheme {
    Integer id;
    Integer beginTime;//开始采集时间
    Integer hourInterval;//间隔多少时间

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Integer beginTime) {
        this.beginTime = beginTime;
    }

    public Integer getHourInterval() {
        return hourInterval;
    }

    public void setHourInterval(Integer hourInterval) {
        this.hourInterval = hourInterval;
    }
}
