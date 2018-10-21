package mappers;

import com.xjy.pojo.DBCommand;
import com.xjy.test.mybatis.pojo.TempCommand;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 11:11 2018/10/19
 * @Description:
 */
public interface CommandMapper {
    List<DBCommand> getCommands(@Param("centerAddress") String centerAddress, @Param("state") int state);
    void updateCommandState(@Param("id") int id ,@Param("state") int state);
}
