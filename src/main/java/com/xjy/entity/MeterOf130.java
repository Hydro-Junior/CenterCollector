package com.xjy.entity;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:31 2019/5/16
 * @Description:
 * 130表资料的具体信息,涉及到表具扩展时，比如电表之类的可以根据这些性质生成表档案信息
 * 但是常用的表档案信息就是如下8个字节
 * 00 00 00 10 00 00 00 00
 * 其中10表示表类型代码，其他都没用到
 */
public class MeterOf130 extends Meter{

    // 测量点性质 协议数据格式06
    public byte measurePro = 0x33;

    // 接线方式 协议数据格式07
    public String connectMode = "";

    // 费率(电表相关) 协议数据格式08
    public byte rates = 0;

    // 表类型代码 协议数据格式09
    public byte typeOfMeter = 0x10;

    // 线路编号 协议数据格式10(双字节BCD码)
    public String lineNumber = "0001";

    // 表箱编号 双字节整数
    public int meterBoxNum = 0;

    public byte getMeasurePro() {
        return measurePro;
    }

    public void setMeasurePro(byte measurePro) {
        this.measurePro = measurePro;
    }

    public String getConnectMode() {
        return connectMode;
    }

    public void setConnectMode(String connectMode) {
        this.connectMode = connectMode;
    }

    public byte getRates() {
        return rates;
    }

    public void setRates(byte rates) {
        this.rates = rates;
    }

    public byte getTypeOfMeter() {
        return typeOfMeter;
    }

    public void setTypeOfMeter(byte typeOfMeter) {
        this.typeOfMeter = typeOfMeter;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getMeterBoxNum() {
        return meterBoxNum;
    }

    public void setMeterBoxNum(int meterBoxNum) {
        this.meterBoxNum = meterBoxNum;
    }

    /**
     * 获取表属性信息
     * @return
     */
    public int[] getGeneralPropertyBytes(){
        int[] propertyBytes = new int[8];
        propertyBytes[3] = typeOfMeter;
        return propertyBytes;
    }

}
