package com.xjy.pojo;

import java.sql.Timestamp;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:31 2018/10/20
 * @Description: 与数据库对应的命令实体类
 */
public class DBCommand {
    private Integer id; //主键
    private String command; //命令码
    private Timestamp generateTime; //生成时间
    private String contentValue1; //参数
    private String contentValue2;
    private String contentValue3;
    private String contentValue4;
    private Integer state;//命令状态

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
        return "DBCommand{" +
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
