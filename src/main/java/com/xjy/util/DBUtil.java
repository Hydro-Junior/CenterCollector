package com.xjy.util;

import com.xjy.entity.Center;
import com.xjy.entity.Collector;
import com.xjy.entity.Command;
import com.xjy.parms.CommandState;
import com.xjy.parms.Constants;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBCommand;
import com.xjy.pojo.DBMeter;
import com.xjy.test.mybatis.util.MyBatisUtil;
import mappers.CenterMapper;
import mappers.CommandMapper;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:02 2018/10/25
 * @Description: 提供一系列对数据表的操作
 */
public class DBUtil {
    //获取某个集中器的待执行命令队列
    public static List<DBCommand> getCommandByCenterAddress(String address){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        List<DBCommand> DBCommands = mapper.getCommands(address , CommandState.UN_ENQUEUED,"%"+Constants.connectServer+":"+Constants.protocolPort+"%");
        session.close();
        return DBCommands;
    }
    //根据集中器获取采集器集合
    public static List<DBCollector> getCollectorsByCenter(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        int centerId = mapper.getIdByAddress(center.getId());
        List<DBCollector> collectors = mapper.getCollectors(centerId);
        session.close();
        return collectors;
    }
    //根据采集器获取表集合
    public static List<DBMeter> getMetersByCollector(DBCollector collector){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        List<DBMeter> meters = mapper.getMetersByCollector(collector.getId());
        session.close();
        return meters;
    }
    //更新集中器上线状态
    public static void updateCenterState(int state, Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        mapper.updateCenterOnline(state,center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        session.commit();
        session.close();
    }
    //更新集中器心跳时间
    public static void updateheartBeatTime(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        mapper.updateHeartBeatTime(center.getId());
        session.commit();
        session.close();
    }

    public static void initCenters() {
        String ip = Constants.connectServer;
        int port = 0;
        try {
            port = Integer.parseInt(Constants.protocolPort);
        }catch (NumberFormatException e){
            System.out.println("端口初始化失败，请检查配置文件。");
        }
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        mapper.initCenterState(ip,port);
        session.commit();
    }
    public static void updateCommandState(int state ,Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        mapper.updateCommandState(center.getCurCommand().getId(),state);
        session.commit();
    }
    public static void updateCommandState(int state ,int commandId){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        mapper.updateCommandState(commandId,state);
        session.commit();
    }
}
