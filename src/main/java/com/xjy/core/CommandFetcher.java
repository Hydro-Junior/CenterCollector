package com.xjy.core;

import com.xjy.adapter.CommandAdapter;
import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.GlobalMap;
import com.xjy.parms.CommandState;
import com.xjy.pojo.DBCommand;
import com.xjy.test.mybatis.util.MyBatisUtil;
import com.xjy.util.DBUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:00 2018/10/25
 * @Description: 命令获取者类，用于不断从数据库中查找每个集中器的待执行命令，添加到其待执行队列中
 */
public class CommandFetcher implements Runnable{
    @Override
    public void run() {
        System.out.println("命令获取线程启动！");
        while(true){ //不断查找数据库中待执行的命令
            ConcurrentHashMap<String, Center> map = GlobalMap.getMap();
            for(Map.Entry<String,Center> entry : map.entrySet()){
                Center c = entry.getValue();
                List<DBCommand> dbCommands = DBUtil.getCommandByCenterAddress(c.getId());
                for(DBCommand dbCommand : dbCommands){
                    Command command = CommandAdapter.getCommand(dbCommand);
                    command.setState(CommandState.WAITING_IN_QUEUE);
                    DBUtil.updateCommandState(CommandState.WAITING_IN_QUEUE, dbCommand.getId());
                    c.getCommandQueue().offer(command);
                }
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
