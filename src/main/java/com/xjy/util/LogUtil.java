package com.xjy.util;


import com.xjy.entity.InternalMsgBody;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:27 2018/11/15
 * @Description: 日志工具类
 */
public class LogUtil {
    private static Map<String , Logger> loggerMap = new HashMap<>();
    public static void DataMessageLog(Class clazz, String info){
        File f = new File("log");
        if(!f.exists()){
            f.mkdir();
        }
        Logger infoLog;
        if(loggerMap.containsKey(clazz.getName())){
            infoLog = loggerMap.get(clazz.getName());
        }else{
            infoLog = Logger.getLogger(clazz);
        }
        infoLog.debug(info);
    }
}
