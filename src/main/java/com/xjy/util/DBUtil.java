package com.xjy.util;

import com.xjy.entity.*;
import com.xjy.parms.CommandState;
import com.xjy.parms.Constants;
import com.xjy.pojo.*;
import com.xjy.test.mybatis.util.MyBatisUtil;
import mappers.CenterMapper;
import mappers.CommandMapper;
import mappers.DeviceTmpMapper;
import org.apache.ibatis.session.SqlSession;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.xjy.entity.GlobalMap.getBasicInfo;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:02 2018/10/25
 * @Description: 提供一系列对数据表的操作
 */
public class DBUtil {
    //获取某个集中器的待执行命令队列（这是很频繁的操作）
    public static List<DBCommand> getCommandByCenterAddress(String address){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        List<DBCommand> DBCommands = mapper.getCommands(address , CommandState.UN_ENQUEUED,"%"+Constants.connectServer+":"+Constants.protocolPort+"%");
        session.close();
        return DBCommands;
    }
    //获取某个集中器特定状态的待执行命令队列
    public static List<DBCommand> getCommandByCenterAddressAndState(String address,int state){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        List<DBCommand> DBCommands = mapper.getCommands(address , state ,"%"+Constants.connectServer+":"+Constants.protocolPort+"%");
        session.close();
        return DBCommands;
    }
    //插入一条新的采集命令
    public static void insertNewCollectCommand(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        ConcurrentHashMap<Center,List<CenterPage>> pageInfo = GlobalMap.getBasicInfo();
        int meterCounts = pageInfo.containsKey(center)? pageInfo.get(center).size() * 32 : 16;
        mapper.insertNewCollectCommand(Timestamp.valueOf(LocalDateTime.now()),meterCounts/16 * 2000,center.getId(),meterCounts,
                Constants.connectServer+":"+Constants.protocolPort,center.getEnprNo());
        session.commit();
        session.close();
    }
    public static void insertNewReadCommand(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        ConcurrentHashMap<Center,List<CenterPage>> pageInfo = GlobalMap.getBasicInfo();
        int meterCounts = pageInfo.containsKey(center)? pageInfo.get(center).size() * 32 : 16;
        mapper.insertNewCollectCommand(Timestamp.valueOf(LocalDateTime.now()),meterCounts/16 * 2000,center.getId(),meterCounts,
                Constants.connectServer+":"+Constants.protocolPort,center.getEnprNo());
        session.commit();
        session.close();
    }
    //根据集中器获取采集器集合
    public static List<DBCollector> getCollectorsByCenter(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        Integer centerId = mapper.getIdByAddress(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        if(centerId == null) {
            LogUtil.DataMessageLog(DBUtil.class,"找不到集中器id！请检查集中器IP和端口配置！");
            return new ArrayList<>();
        }
        center.setDbId(centerId);//下载档案时可以重置集中器在数据库中的id，可用于重新导入资料后的数据更新
        List<DBCollector> collectors = mapper.getCollectors(centerId);
        session.close();
        return collectors;
    }
    public static Scheme getScheme(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        if(center.getDbId() == null) {
            try{
             center.setDbId(mapper.getIdByAddress(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort)));
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        if(center.getDbId()  == null) {
            LogUtil.DataMessageLog(DBUtil.class,"找不到集中器id！请检查集中器IP和端口配置！");
            return null;
        }
        Scheme scheme = null;
        try {
            scheme = mapper.getScheme(mapper.getSchemeId(center.getDbId()));
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("查询计划命令时出错！");
        }
        return scheme;
    }
    //读表前的预处理，加载相关参数
    public static void preprocessOfRead(Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        Integer centerId = mapper.getIdByAddress(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        if(centerId  == null) {
            LogUtil.DataMessageLog(DBUtil.class,"找不到集中器id！请检查集中器IP和端口配置！");
            return;
        }
        center.setDbId(centerId);
        if(center.getEnprNo() == null){
            String enprNo = mapper.getEnprNo(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));;
            center.setEnprNo(enprNo);
        }

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
    public static void updateCommandEndTime(Center center, Timestamp time){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        mapper.updateCommandEndTime(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort),time);
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
    //更新deviceTmp表中的阀门状态(由于原来程序中数据库的commstate和meterstate字段混乱使用，导致一个字段被浪费，temp表中不再有
    // 表示开关阀状态的字段，此方法暂时保留)
    public static void updateValveStateOfTmp(int valveState,String meterAddress, Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        int centerId = center.getDbId();
        String tableName = "t_deviceTmp"+ LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        mapper.updateValveState(tableName,valveState,centerId,LocalDateTime.now().getDayOfMonth(),meterAddress);
        session.commit();
        session.close();
    }
    public static void updateValveState(int valveState,String meterAddress, Center center){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        int centerId = center.getDbId();
        DBMeter meter = mapper.searchDevice(centerId,meterAddress);
        if(meter == null) LogUtil.DataMessageLog(DBUtil.class,"找不到对应水表！\r\n centerId="+centerId+"   meterAddress="+meterAddress);
        if(meter.getStrobeStatue() != 0)mapper.updateStrobeState(valveState,meter.getId());
        session.commit();
        session.close();
    }
    public static void updateValveState(int valveState,int meterId){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        mapper.updateStrobeState(valveState,meterId);
        session.commit();
        session.close();
    }
    //更新表数据
    public static void refreshMeterData(Meter meter, Center center) {
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        String enprNo = center.getEnprNo();
        int centerId = center.getDbId();
        //如果当天已有数据，则更新，否则插入
        String tableName = "t_deviceTmp"+ LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        if(mapper.searchDeviceData(tableName,centerId,LocalDateTime.now().getDayOfMonth(),meter.getId()) != null){
            mapper.updateDeviceData(tableName,meter.getValue(), Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId(),meter.getValveState(),meter.getState(),enprNo);
        }else{
            mapper.insertNewData(tableName,meter.getValue(),Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId(),meter.getValveState(),meter.getState(),enprNo);
        }
        //阀门信息
        DBMeter dbmeter = mapper.searchDevice(centerId,meter.getId());
        if(meter == null) LogUtil.DataMessageLog(DBUtil.class,"找不到对应水表！\r\n centerId="+centerId+"   meterAddress="+meter.getId());
        if(dbmeter.getStrobeStatue() != 0 && meter.getValveState()>=1) mapper.updateStrobeState(meter.getValveState(),dbmeter.getId());
        session.commit();
        session.close();
    }
    public static DeviceTmp getTmpRecord(int centerId,String meterAddress){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        String tableName = "t_deviceTmp"+ LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        return mapper.searchDeviceData(tableName,centerId,LocalDateTime.now().getDayOfMonth(),meterAddress);
    }
    public static void createTempDeviceTable(){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        String tableName = "t_deviceTmp"+ LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        if(mapper.existTable(tableName) > 0 ) {
            //System.out.println("表已存在！");
            session.close();
        } else {
            //System.out.println("创建新表！");
            mapper.createNewTable(tableName);
            session.commit();
            session.close();
        }
    }
    public static DBMeter getDBMeter(int centerId,String meterAddress){
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        return mapper.searchDevice(centerId,meterAddress);
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
        Integer centerId = center.getDbId();
        if(centerId == null) return;
        String tableName = "t_deviceTmp"+ LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        //如果当天已有数据，则更新，否则插入
        for(Meter meter : tempMeterData){
            if(mapper.searchDeviceData(tableName,centerId,LocalDateTime.now().getDayOfMonth(),meter.getId()) != null){
                mapper.updateDeviceData(tableName,meter.getValue(), Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId(),meter.getValveState(),meter.getState(),enprNo);
            }else{
                System.out.println(
                        "tableName:" + tableName + "meter:" + meter +"centerId: " + "enprNo: " + enprNo
                );
                mapper.insertNewData(tableName,meter.getValue(),Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId(),meter.getValveState(),meter.getState(),enprNo);
            }
            //TODO 可在此处添加与前一天的数据比对，将读数异常的表信息插入t_warning表，也可交给web端做数据同步时操作
            //阀门信息
            DBMeter dbmeter = mapper.searchDevice(centerId,meter.getId());
            if(dbmeter == null){
                LogUtil.DataMessageLog(DBUtil.class,"找不到对应水表！\r\n centerId="+centerId+"   meterAddress="+meter.getId());
                continue;
            }
            try{
                if(dbmeter.getStrobeStatue() != 0 && meter.getValveState()>=1) mapper.updateStrobeState(meter.getValveState(),dbmeter.getId());
            }catch (NullPointerException e){
                //测试时出现空指针异常，很可能是数据库中不存在这个表！
                LogUtil.DataMessageLog(DBUtil.class,"测试时出现空指针异常，很可能是数据库中不存在这个表！\r\n 集中器号："+center.getId()+"    表地址号："+meter.getId());
            }
        }
        session.commit();
        session.close();
    }

    public static void getEnprNoByAddress(Center center) {
        SqlSession session = MyBatisUtil.getSqlSessionFactory().openSession();
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        String enprNo = mapper.getEnprNo(center.getId(),Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        center.setEnprNo(enprNo);
        session.close();
    }
}
