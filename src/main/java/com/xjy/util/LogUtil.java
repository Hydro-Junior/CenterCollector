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
        String dateString =  LocalDateTime.now().getYear() + "年"+LocalDateTime.now().getMonthValue()+"月";
        //String path = "log/" + addr + ".txt";
        String parentPath = "log/" + dateString + "/";
        File f = new File(parentPath);
        if(!f.exists()) f.mkdirs();
        //log4j 调试部分
        Logger infoLog;
        if(loggerMap.containsKey(clazz.getName())){
            infoLog = loggerMap.get(clazz.getName());
        }else{
            infoLog = Logger.getLogger(clazz);
        }
        infoLog.debug(info);
        //dataMessage
        String path = "log/" + dateString + "/" + "dataLog - " + LocalDateTime.now().getMonthValue()+"月"+LocalDateTime.now().getDayOfMonth()+"日.txt";
        File tf = new File(path);
        try {
            if(!tf.exists()){
                tf.createNewFile();
            }
            RandomAccessFile randomFile = new RandomAccessFile(path, "rw");
            long fileLength = randomFile.length();
            randomFile.seek(fileLength);
            randomFile.writeBytes(LocalDateTime.now().toString() +"\r\n" + info + "\r\n\r\n");
            randomFile.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static void channelLog(String addr, String msg) throws IOException {
        String dateString =  LocalDateTime.now().getYear() + "年"+LocalDateTime.now().getMonthValue()+"月";
        //String path = "log/" + addr + ".txt";
        String parentPath = "log/" + dateString + "/";
        File f = new File(parentPath);
        if(!f.exists()) f.mkdirs();
        String path = "log/" + dateString + "/" + addr + "-" + LocalDateTime.now().getMonthValue()+"月"+LocalDateTime.now().getDayOfMonth()+"日.txt";
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
    public static void planTaskLog(String info)throws IOException {
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
        //info写入时出现了乱码现象
        randomFile.writeBytes(LocalDateTime.now().toString() +"\r\n" + /*new String(info.getBytes("UTF-8"),"UTF-8")*/info + "\r\n\r\n");
        randomFile.close();
    }
}
