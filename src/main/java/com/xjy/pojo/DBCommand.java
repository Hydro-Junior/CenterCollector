package com.xjy.pojo;

import java.sql.Timestamp;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:31 2018/10/20
 * @Description:
 */
public class DBCommand {
    private Integer id;
    private String command;
    private Timestamp generateTime;
    private String contentValue1;
    private String contentValue2;
    private String contentValue3;
    private String contentValue4;
    private Integer state;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
