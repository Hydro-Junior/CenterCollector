package com.xjy.test.mybatis.pojo;

import java.sql.Timestamp;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:58 2018/10/19
 * @Description:
 */
public class TempCommand {
    Integer id;
    String command;
    Timestamp generateTime;
    String contentValue1;
    String contentValue2;
    String contentValue3;
    String contentValue4;
    Integer state;

    public TempCommand(){super();}
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Timestamp getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(Timestamp generateTime) {
        this.generateTime = generateTime;
    }

    public String getContentValue1() {
        return contentValue1;
    }

    public void setContentValue1(String contentValue1) {
        this.contentValue1 = contentValue1;
    }

    public String getContentValue2() {
        return contentValue2;
    }

    public void setContentValue2(String contentValue2) {
        this.contentValue2 = contentValue2;
    }

    public String getContentValue3() {
        return contentValue3;
    }

    public void setContentValue3(String contentValue3) {
        this.contentValue3 = contentValue3;
    }

    public String getContentValue4() {
        return contentValue4;
    }

    public void setContentValue4(String contentValue4) {
        this.contentValue4 = contentValue4;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "TempCommand{" +
                "id=" + id +
                ", command='" + command + '\'' +
                ", generateTime=" + generateTime +
                ", contentValue1='" + contentValue1 + '\'' +
                ", contentValue2='" + contentValue2 + '\'' +
                ", contentValue3='" + contentValue3 + '\'' +
                ", contentValue4='" + contentValue4 + '\'' +
                ", state=" + state +
                '}';
    }
}
