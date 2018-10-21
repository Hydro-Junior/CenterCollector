package com.xjy.test.mybatis.mappers;

import com.xjy.test.mybatis.pojo.TempCommand;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:11 2018/10/19
 * @Description:
 */
public interface TempCommandMapper {
    TempCommand getCommand(int id);
    List<TempCommand> getCommands(@Param("centerAddress") String centerAddress , @Param("state") int state);
}
