package com.xjy.adapter;

import com.xjy.entity.Command;
import com.xjy.parms.CommandType;
import com.xjy.pojo.DBCommand;

import java.time.LocalDateTime;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:09 2018/10/25
 * @Description: 适配器类，实现数据库DBCommand与程序逻辑Command的互转
 */
public class CommandAdapter {
    public static Command getCommand(DBCommand dbCommand){
        Command c = new Command();
        c.setId(dbCommand.getId());
        c.setState(dbCommand.getState());
        //判断命令类型
        String s = dbCommand.getCommand();
        for(CommandType type : CommandType.values()){
            if(type.getValue().equals(s)){
                c.setType(type);
            }
        }
        //设置参数
        c.setArgs(new String[]{dbCommand.getContentValue1(),dbCommand.getContentValue2(),dbCommand.getContentValue3(),dbCommand.getContentValue4()});
        c.setGenerateTime(dbCommand.getGenerateTime().toLocalDateTime());
        return c;
    }
}
