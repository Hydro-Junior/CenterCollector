package com.xjy.entity;

import java.util.Arrays;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:27 2018/10/25
 * @Description:
 */
public class DetailedInfoOfCenter {
    private String timingCollect ;
    private String deviceClock;
    private String[] historyRecord = new String[12];//12条记录，第一位从A到T

    public String getTimingCollect() {
        return timingCollect;
    }

    public void setTimingCollect(String timingCollect) {
        this.timingCollect = timingCollect;
    }

    public String getDeviceClock() {
        return deviceClock;
    }

    public void setDeviceClock(String deviceClock) {
        this.deviceClock = deviceClock;
    }

    public String[] getHistoryRecord() {
        return historyRecord;
    }

    public void setHistoryRecord(String[] historyRecord) {
        this.historyRecord = historyRecord;
    }

    @Override
    public String toString() {
        return "DetailedInfoOfCenter{" +
                "timingCollect='" + timingCollect + '\'' +
                ", deviceClock='" + deviceClock + '\'' +
                ", historyRecord=" + Arrays.toString(historyRecord) +
                '}';
    }
}
