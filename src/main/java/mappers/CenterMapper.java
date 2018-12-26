package mappers;

import com.xjy.pojo.DBCollector;
import com.xjy.pojo.DBCommand;
import com.xjy.pojo.DBMeter;
import com.xjy.pojo.Scheme;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:22 2018/10/20
 * @Description:
 */
public interface CenterMapper {
    Integer getIdByAddress (@Param("address") String address,@Param("ip") String ip, @Param("port") int port);
    void initCenterState(@Param("ip") String ip, @Param("port") int port);
    void updateCenterOnline(@Param("state")int state,@Param("address") String address,@Param("ip")String ip,@Param("port")int port);
    void updateCenterReadTime(int id);
    void updateHeartBeatTime(@Param("address") String address,@Param("ip") String ip, @Param("port") int port);
    int getSchemeId(@Param("centerId")int centerId);
    Scheme getScheme(int schemeId);
    String getEnprNo(@Param("address") String address,@Param("ip")String ip,@Param("port")int port);
    List<DBCollector> getCollectors(@Param("centerId") int centerId);
    List<DBMeter> getMetersByCollector(@Param("collectorId") int collectorId);
}
