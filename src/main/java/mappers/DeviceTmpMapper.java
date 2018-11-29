package mappers;

import com.xjy.pojo.DeviceTmp;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;

/**
 * @Author: Mr.Xu
 * @Date: Created in 16:28 2018/11/16
 * @Description:与表数据相关的mapper
 */
public interface DeviceTmpMapper {
    DeviceTmp searchDeviceData(@Param("tableName") String tableName, @Param("centerId") int centerId,
                               @Param("day") int day, @Param("meterAddress") String meterAddress);
    void updateDeviceData(@Param("tableName") String tableName, @Param("data")double data, @Param("now")Timestamp now, @Param("centerId") int centerId,
                          @Param("day") int day, @Param("meterAddress") String meterAddress,@Param("valveState")int valveState,@Param("state")int state);
    //修改temp表的阀门状态
    void updateValveState(@Param("tableName") String tableName,@Param("valveState")int valveState, @Param("centerId") int centerId,
                          @Param("day") int day, @Param("meterAddress") String meterAddress);
    void insertNewData(
            @Param("tableName") String tableName, @Param("data")double data, @Param("now")Timestamp now, @Param("centerId") int centerId,
            @Param("day") int day, @Param("meterAddress") String meterAddress,@Param("valveState")int valveState,@Param("state")int state,@Param("enprNo")String enprNo
    );
}
