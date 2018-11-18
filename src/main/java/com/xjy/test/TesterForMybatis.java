package com.xjy.test;

import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.InternalMsgBody;
import com.xjy.entity.Meter;
import com.xjy.parms.CommandState;
import com.xjy.parms.Constants;
import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBCommand;
import com.xjy.pojo.DBMeter;
import com.xjy.pojo.DeviceTmp;
import com.xjy.test.mybatis.mappers.TempCommandMapper;
import com.xjy.test.mybatis.pojo.TempCommand;
import com.xjy.test.mybatis.util.MyBatisUtil;
import com.xjy.util.DBUtil;
import mappers.CenterMapper;
import mappers.CommandMapper;
import mappers.DeviceTmpMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:04 2018/10/19
 * @Description:
 */
public class TesterForMybatis {
    SqlSession session ;
    @Before
    public void setup(){
        session = MyBatisUtil.getSqlSessionFactory().openSession();
    }
    @Test
    public void testGetCommand() {
            CommandMapper mapper = session.getMapper(CommandMapper.class);
            List<DBCommand> tempCommands = mapper.getCommands("00201709004" , CommandState.SUCCESSED,"%"+Constants.connectServer+":"+Constants.protocolPort+"%");
            System.out.println(tempCommands);
    }
    @Test
    public void testUpdateCommand(){
        CommandMapper mapper = session.getMapper(CommandMapper.class);
        mapper.updateCommandState(25393,5);
        session.commit();
    }
    @Test
    public void testSearchIdOfCenter(){
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        System.out.println(mapper.getIdByAddress("00020160811",Constants.connectServer,Integer.parseInt(Constants.protocolPort)));
    }
    @Test
    public void testInitCenterState(){
        DBUtil.initCenters();
    }
    @Test
    public void testUpdateCenterOnline(){
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        mapper.updateCenterOnline(1,"00201611251",Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        session.commit();
    }
    @Test
    public void testUpdateHeartBeatTime(){
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        mapper.updateHeartBeatTime("650157587",Constants.connectServer,Integer.parseInt(Constants.protocolPort));
        session.commit();
    }
    @Test
    public void testDeviceData(){
        DeviceTmpMapper mapper = session.getMapper(DeviceTmpMapper.class);
        Meter meter = new Meter();
        meter.setId("000070301698");
        meter.setValue(124.0);
        String enprNo = "cbxtd";
        int centerId = 792;
        String tableName = "t_deviceTmp"+LocalDateTime.now().getYear()+String.format("%02d",LocalDateTime.now().getMonthValue());
        if(mapper.searchDeviceData(tableName,centerId,LocalDateTime.now().getDayOfMonth(),meter.getId()) != null){
            mapper.updateDeviceData(tableName,meter.getValue(),Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId());
        }else{
            mapper.insertNewData(tableName,meter.getValue(),Timestamp.valueOf(LocalDateTime.now()),centerId,LocalDateTime.now().getDayOfMonth(),meter.getId(),meter.getState(),enprNo);
        }
        //session.commit();
    }
    @Test
    public void testGetInformation(){
        CenterMapper mapper = session.getMapper(CenterMapper.class);
        int centerId = mapper.getIdByAddress("00201611251",Constants.connectServer, Integer.parseInt(Constants.protocolPort));
        List<DBCollector> dbCollectors = mapper.getCollectors(centerId);
        for(DBCollector dbCollector : dbCollectors){
            System.out.print("【" + dbCollector+"】");
            List<DBMeter> dbMeters = mapper.getMetersByCollector(dbCollector.getId());
            System.out.println(" 采集器下的表个数："+dbMeters.size());
            System.out.println(dbMeters);
        }
    }
    @After
    public void destroy(){
        session.close();
    }

}
