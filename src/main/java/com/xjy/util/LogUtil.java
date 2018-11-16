package com.xjy.util;


import com.xjy.entity.InternalMsgBody;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:27 2018/11/15
 * @Description: 日志工具类
 */
public class LogUtil {
    private static final Logger dataLogger = Logger.getLogger(InternalMsgBody.class);
    public static void DataMessageLog(String info){
        File f = new File("log");
        if(!f.exists()){
            f.mkdir();
        }
        dataLogger.debug(info);
    }
}
