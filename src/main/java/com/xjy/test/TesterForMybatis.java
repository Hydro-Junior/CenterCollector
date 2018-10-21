package com.xjy.test;

import com.xjy.entity.Command;
import com.xjy.parms.CommandState;
import com.xjy.pojo.DBCommand;
import com.xjy.test.mybatis.mappers.TempCommandMapper;
import com.xjy.test.mybatis.pojo.TempCommand;
import com.xjy.test.mybatis.util.MyBatisUtil;
import mappers.CenterMapper;
import mappers.CommandMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:04 2018/10/19
 * @Description:
 */
public class TesterForMybatis {
    SqlSession session ;
    @Before
    public void setup(){
        session = MyBatisUtil.getSqlSessionFactory().openSession();
    }
    @Test
    public void testGetCommand() {
            CommandMapper mapper = session.getMapper(CommandMapper.class);
            List<DBCommand> tempCommands = mapper.getCommands("00201709004" , CommandState.SUCCESSED);
            System.out.println(tempCommands);
    }
    @Test
    public void testUpdateCommand(){
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        mapper.updateCommandState(25393,5);
        session.commit();
    }
    @Test
    public void testSearchIdOfCenter(){
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        System.out.println(mapper.getIdByAddress("00020160811"));
    }

    @After
    public void destroy(){
        session.close();
    }

}
