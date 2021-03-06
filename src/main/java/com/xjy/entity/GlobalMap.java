package com.xjy.entity;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:32 2018/9/28
 * @Description: 全局的单例，维护一个ConcurrentHashMap,监控集中器的实时状态
 */
public class GlobalMap {
    private GlobalMap(){}
    private static class InnerMap{
        private static ConcurrentHashMap<String,Center> map = new ConcurrentHashMap<>();
    }
    private static class InnerInfo{
        private static ConcurrentHashMap<Center,List<CenterPage>> persistentDataOfInternalProtocol = new ConcurrentHashMap<>();
    }
    private static class XTInfo{
        private static ConcurrentHashMap<Center,List<MeterOf130>> persistentDataOfXtProtocol = new ConcurrentHashMap<>();
    }
    //获得实时集中器状态表，key为集中器地址
    public static ConcurrentHashMap getMap(){//getMap方法第一次调用时，InnerMap实例才会被创建
        return InnerMap.map;
    }
    //获得持久层的基本数据资料，key为集中器
    public static ConcurrentHashMap getBasicInfo(){return InnerInfo.persistentDataOfInternalProtocol;}
    //获得130集中器的所有表
    public static ConcurrentHashMap get130MeterInfo(){return XTInfo.persistentDataOfXtProtocol;}
}
