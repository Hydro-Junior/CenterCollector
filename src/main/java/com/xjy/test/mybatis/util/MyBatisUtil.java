package com.xjy.test.mybatis.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.*;
import java.util.Properties;

/**
 * @Author: Mr.Xu
 * @Date: Created in 14:21 2018/10/19
 * @Description:
 */
public class MyBatisUtil {
    private final static SqlSessionFactory sqlSessionFactory;
    static {
        String resource = "mybatis-config.xml";
        InputStream inputStream = null;
        Properties properties = new Properties();
        try {
            try {
                properties.load(new BufferedInputStream(new FileInputStream("conf/config")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
       //sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream,properties);
    }
    public static SqlSessionFactory getSqlSessionFactory(){
        return sqlSessionFactory;
    }
}
