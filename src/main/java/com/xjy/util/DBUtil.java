package com.xjy.util;

import com.xjy.entity.Center;
import com.xjy.entity.Collector;
import com.xjy.entity.Command;
import com.xjy.entity.Meter;
import com.xjy.parms.CommandState;
import com.xjy.parms.Constants;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBCommand;
import com.xjy.pojo.DBMeter;
import com.xjy.test.mybatis.util.MyBatisUtil;
import mappers.CenterMapper;
import mappers.CommandMapper;
import mappers.DeviceTmpMapper;
import org.apache.ibatis.session.SqlSession;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:02 2018/10/25
 * @Description: 提供一系列对数据表的操作
 */
public class DBUtil {
    //获取某个集中器的待执行命令队列（这是很频繁的操作，需要复用同一个会话）
    public static List<DBCommand> getCommandByCenterAddress(String address,SqlSession session){
        //SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        List<DBCommand> DBCommands = mapper.getCommands(address , CommandState.UN_ENQUEUED,"%"+Constants.connectServer+":"+Constants.protocolPort+"%");
        //session.close();
        return DBCommands;
    }
    //根据集中器获取采集器集合
    public static List<DBCollector> getCollectorsByCenter(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        int centerId = mapper.getIdByAddress(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        center.setDbId(centerId);//下载档案时可以重置集中器在数据库中的id，可用于重新导入资料后的数据更新
        List<DBCollector> collectors = mapper.getCollectors(centerId);
        session.close();
        return collectors;
    }
    public static void preprocessOfRead(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        int centerId = mapper.getIdByAddress(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        center.setDbId(centerId);
        if(center.getEnprNo() == null)
            center.setEnprNo(mapper.getEnprNo(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort)));
        session.close();
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
        mapper.updateHeartBeatTime(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));
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
        session.close();
    }
    public static void updateCommandState(int state ,Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        mapper.updateCommandState(center.getCurCommand().getId(),state);
        session.commit();
        session.close();
    }
    public static void updateCommandState(int state ,int commandId){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        mapper.updateCommandState(commandId,state);
        session.commit();
        session.close();
    }

    public static void refreshMeterData(Meter meter, Center center) {
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        String enprNo = center.getEnprNo();
        int centerId = center.getDbId();
        //如果当天已有数据，则更新，否则插入
        String tableName = "t_deviceTmp"+ LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        if(mapper.searchDeviceData(tableName,centerId,LocalDateTime.now().getDayOfMonth(),meter.getId()) != null){
            mapper.updateDeviceData(tableName,meter.getValue(), Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId());
        }else{
            mapper.insertNewData(tableName,meter.getValue(),Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId(),meter.getState(),enprNo);
        }
        session.commit();
        session.close();
    }

    public static void updateCenterReadTime(Center center) {
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        mapper.updateCenterReadTime(center.getDbId());
        session.commit();
        session.close();
    }

    public static void batchlyRefreshData(List<Meter> tempMeterData, Center center) {
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        String enprNo = center.getEnprNo();
        int centerId = center.getDbId();
        String tableName = "t_deviceTmp"+ LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        //如果当天已有数据，则更新，否则插入
        for(Meter meter : tempMeterData){
            if(mapper.searchDeviceData(tableName,centerId,LocalDateTime.now().getDayOfMonth(),meter.getId()) != null){
                mapper.updateDeviceData(tableName,meter.getValue(), Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId());
            }else{
                mapper.insertNewData(tableName,meter.getValue(),Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId(),meter.getState(),enprNo);
            }
        }
        session.commit();
        session.close();
    }
}
