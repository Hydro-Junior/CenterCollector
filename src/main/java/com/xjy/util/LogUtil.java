package com.xjy.util;


import org.apache.log4j.Logger;

import java.io.File;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:27 2018/11/15
 * @Description: 日志工具类
 */
public class LogUtil {
    public static void DataMessageLog(Class clazz,String info){
        File f = new File("log");
        if(!f.exists()){
            f.mkdir();
        }
        Logger dataLogger = Logger.getLogger(clazz);
        dataLogger.debug(info);
    }
}
