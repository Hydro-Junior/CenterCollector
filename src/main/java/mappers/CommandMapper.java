package mappers;

import com.xjy.pojo.DBCommand;
import com.xjy.test.mybatis.pojo.TempCommand;
import org.apache.ibatis.annotations.Param;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:11 2018/10/19
 * @Description:
 */
public interface CommandMapper {
    List<DBCommand> getCommands(@Param("centerAddress") String centerAddress, @Param("state") int state,@Param("portStr")String portStr);
    void updateCommandState(@Param("id") int id ,@Param("state") int state);
    void insertNewCollectCommand(@Param("generateTime")Timestamp generateTime,@Param("executeTime") int executeTime,@Param("centerAddress")String centerAdrress,
                                 @Param("meterCount")int meterCount,@Param("portStr")String portStr,@Param("enprNo")String enprNo);
    void insertNewReadCommand(@Param("generateTime")Timestamp generateTime,@Param("executeTime") int executeTime,@Param("centerAddress")String centerAdrress,
                                 @Param("meterCount")int meterCount,@Param("portStr")String portStr,@Param("enprNo")String enprNo);
    //#{generateTime},#{executeTime},#{centerAddress},#{meterCount},
    //        0,#{portStr},#{enprNo}
}
