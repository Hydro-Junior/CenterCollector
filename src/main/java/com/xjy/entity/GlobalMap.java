package com.xjy.entity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:32 2018/9/28
 * @Description: 一个全局的单例，维护一个ConcurrentHashMap,监控集中器的实时状态
 */
public class GlobalMap {
    private GlobalMap(){}
    private static class InnerMap{
        private static ConcurrentHashMap<String,Center> map = new ConcurrentHashMap<>();
    }
    private static class InnerInfo{
        private static ConcurrentHashMap<String,Center> persistenceData = new ConcurrentHashMap<>();
    }
    //获得实时集中器状态表，key为集中器地址
    public static ConcurrentHashMap getMap(){//getMap方法第一次调用时，InnerMap实例才会被创建
        return InnerMap.map;
    }
    //获得持久层的基本数据资料，key为水司编码
    public static ConcurrentHashMap getBasicInfo(){return InnerInfo.persistenceData;}
}
