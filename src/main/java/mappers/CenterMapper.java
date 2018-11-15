package mappers;

import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBCommand;
import com.xjy.pojo.DBMeter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:22 2018/10/20
 * @Description:
 */
public interface CenterMapper {
    int getIdByAddress(String address);
    void initCenterState(@Param("ip") String ip, @Param("port") int port);
    void updateCenterOnline(@Param("state")int state,@Param("address") String address,@Param("ip")String ip,@Param("port")int port);
    void updateHeartBeatTime(@Param("address") String address);
    List<DBCollector> getCollectors(@Param("centerId") int centerId);
    List<DBMeter> getMetersByCollector(@Param("collectorId") int collectorId);
}
