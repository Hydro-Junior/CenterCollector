package com.xjy.core;

import com.xjy.decoder.InternalProtocolDecoder;
import com.xjy.decoder.XtProtocolDecoder;
import com.xjy.parms.Constants;
import com.xjy.handler.InternalMessageHandler;
import com.xjy.handler.XtMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:01 2018/9/27
 * @Description: 基于Netty实现的水表采集总服务端
 */
public class CollectServer {
    private String protocol;
    private int port;
    public CollectServer(String protocol,int port){
        //到时根据配置文件的协议类型和端口进行初始化
    }
    public CollectServer(){
    }
    public void bind(int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap b = new ServerBootstrap();//NIO服务端的辅助启动类
            //通过辅助启动类进行配置
            b.group(bossGroup,workerGroup)//定义线程组
                    //选择通道类型
                    .channel(NioServerSocketChannel.class) //指定NIO的模式
                    .option(ChannelOption.SO_BACKLOG,1024) //配置tcp缓冲区，对已经建立的连接不影响
                    //绑定事件处理方法
                    .option(ChannelOption.SO_SNDBUF,32 * 1024)
                    .option(ChannelOption.SO_RCVBUF,32 * 1024)
                    .option(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChildChannelHandler()); // 拿到SocketChannel,交给具体数据的处理类
            //绑定端口，同步等待成功
            ChannelFuture f = b.bind(port).sync();
            //等待服务器监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            // 拿到SocketChannel,进行一系列的配置
            //socketChannel.config().setAllowHalfClosure(true);//允许半关闭
            if(Constants.protocol != null && Constants.protocol.equals("XT")){
                ByteBuf xtdelimiter = Unpooled.copiedBuffer(Constants.XT_DELIMETER);//定义分隔符
                //管道流式处理字节流
                socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,xtdelimiter),
                        new XtProtocolDecoder());
                socketChannel.pipeline().addLast( new XtMessageHandler());
            }else{//默认内部协议
                //内部协议以固定数据头作分隔符
                //ByteBuf internalDelimiter = Unpooled.copiedBuffer(Constants.INTERNAL_DELIMETER);//定义分隔符
                //管道流式处理字节流
    /*由于内部协议只有报文头，如果用delimiter解码会导致这样一个问题：只有在下一个消息到达时，当前消息才能被处理！这就导致了
    接收消息的滞后！*/
                //socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,internalDelimiter));
                socketChannel.pipeline().addLast( new InternalProtocolDecoder());
                socketChannel.pipeline().addLast( new InternalMessageHandler());
            }
        }

    }

    public static void main(String[] args) {
        new Thread(new ConditionMonitor()).start();
        new Thread(new CommandFetcher()).start();//轮询数据库命令的线程
        new Thread(new CommandExecutor()).start();//命令执行线程
        ScheduleTask.startTimingCollect();//开启定时采集任务
        try{
            new CollectServer().bind(Integer.parseInt(Constants.protocolPort));
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println("绑定地址错误！\r\n + IP:"+Constants.connectServer + "端口号："+Constants.protocolPort );
        }

    }
}
