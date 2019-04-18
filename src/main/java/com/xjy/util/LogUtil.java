package com.xjy.util;


import com.xjy.entity.InternalMsgBody;
import org.apache.log4j.Logger;

import java.io.*;
import java.time.LocalDateTime;
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
    public static void channelLog(String addr, String msg) throws IOException {
        File f = new File("log");
        if(!f.exists()){
            f.mkdir();
        }
        String path = "log/" + addr + ".txt";
        File tf = new File(path);
        if(!tf.exists()){
            tf.createNewFile();
        }
        RandomAccessFile randomFile = new RandomAccessFile(path, "rw");
        long fileLength = randomFile.length();
        randomFile.seek(fileLength);
        randomFile.writeBytes(LocalDateTime.now().toString() +"\r\n" + msg + "\r\n\r\n");
        randomFile.close();
    }
    public static void PlanTaskLog (String info)throws IOException {
        File f = new File("log");
        if(!f.exists()){
            f.mkdir();
        }
        String path = "log/planTask" + LocalDateTime.now().getYear() + "年"+LocalDateTime.now().getMonthValue()+"月" + ".txt";
        File tf = new File(path);
        if(!tf.exists()){
            tf.createNewFile();
        }
        RandomAccessFile randomFile = new RandomAccessFile(path, "rw");
        long fileLength = randomFile.length();
        randomFile.seek(fileLength);
        randomFile.writeBytes(LocalDateTime.now().toString() +"\r\n" + info + "\r\n\r\n");
        randomFile.close();
    }
}
