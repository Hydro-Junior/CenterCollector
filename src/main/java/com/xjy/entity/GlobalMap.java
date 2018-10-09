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
        private static ConcurrentHashMap<String,Center> map = new ConcurrentHashMap<String, Center>();
    }
    public static ConcurrentHashMap getMap(){//getMap方法第一次调用时，InnerMap实例才会被创建
        return InnerMap.map;
    }
}
