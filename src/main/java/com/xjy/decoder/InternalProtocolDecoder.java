package com.xjy.decoder;

import com.xjy.entity.*;
import com.xjy.parms.CommandState;
import com.xjy.parms.CommandType;
import com.xjy.parms.Constants;
import com.xjy.parms.InternalMsgType;
import com.xjy.processor.ExceptionProcessor;
import com.xjy.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:17 2018/9/27
 * @Description: 按内部协议对字节流进行初步解码
 */
public class InternalProtocolDecoder extends ByteToMessageDecoder {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LogUtil.DataMessageLog(InternalProtocolDecoder.class,"内部协议解码时异常");
        cause.printStackTrace();
        ExceptionProcessor.processAfterException(ctx);//将对应集中器的命令状态置为失败
        ctx.close();
    }
    //内部协议只存在报文头，报文尾0x45仅为数据包尾，心跳包没有报文尾
    //理论上来讲这种协议设计并不是很合理，较难处理分包的情况，
    //为了效率优先，只能舍弃原来的分隔符解码器。
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int rd = in.readerIndex(); //获取最初的readerIndex
        if(in.readableBytes() < 15) return; //小于最小报文长度，不做处理
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);//这里读取in中的字节，readerIndex发生变化
        List<Integer> res = DecodeUtil.findHead(bytes, Constants.INTERNAL_DELIMETER);
        if(res.size() == 0){//未找到报文头
            //得到的消息中没有报文头，打印字节
            System.out.println("得到的消息中没有报文头，打印字节");
            for(int i = 0 ; i < bytes.length; i++){
                System.out.print(ConvertUtil.fixedLengthHex(bytes[i])+ " ");
            }
            in.readerIndex(rd);//重置读取位置
            return;
        }
        //报文分割处理
        for(int i = 0 ; i < res.size() ;i++){
            int start = res.get(i) + 4;
            int end = ((i+1)==res.size() ? bytes.length : res.get(i+1));//下一个报文头的位置
            /**
             * 【半包处理】
             * findHead方法是找两个报文头，对于粘包能顺利解决，对于分包，只能结合一下两个信息，另外添加判断处理
             * (可能要用到mark和reset)，降低异常发生的概率
             1. 心跳包长度是15
             2. 其他任何数据包结尾都是0x45（甚至有可能报文中间有0x45，如关节点失败EEE），且数据包都有指令码，且长度在25以上
             3. 这里没有处理太过极端的情况（出现几率几乎为零），比如半包刚好截至指令位，且指令为0x45
             */
            //心跳包满足end - start == 11
            //值得一提的是，对于可能的数据包（大于11），只判断最后两个字节（无线集中器最后是0x45 0x0d），不合要求即算半包
            //就实际测试来看，没有必要轮询往前找0x45，即便出现0x45在更前面的位置这种几乎不可能的情况（那0x45后面是什么？
            // 等于没按照协议的规矩来，像无线集中器最后就是0x0d），也会打印出来
            if(end - start < 11 || (end -start > 11 && bytes[end-1]!= 0x45 && bytes[end-2] != 0x45)){
                //自组网在数据包后面还跟个0x0d，因此检测最后两位即可，如果都不是0x45，认定为半包。
                LogUtil.DataMessageLog(InternalProtocolDecoder.class,"检测到半包，打印字节");
                for(int j = start ; j < end; j++){
                    System.out.print(ConvertUtil.fixedLengthHex(bytes[j])+ " ");
                }
                in.readerIndex(rd + res.get(i));//重置读取开始位指针
                in.discardReadBytes();
                return;
            }
            if(end - start > 11 && bytes[end-1] == 0x0d && bytes[end-2]==0x45) end--;//无线集中器的特殊情况，数据包以0x0d结尾
            byte[] strippedBytes = new byte[end - start];
            System.arraycopy(bytes,start,strippedBytes,0,end - start);
            /**
             * 由于超过127的byte经java的强转后，高位都是1，为避免一些问题，统一 &0xff 取后8位，转为int处理
             */
            int[] data = ConvertUtil.bytesToInts(strippedBytes);
            //LogUtil.DataMessageLog(InternalProtocolDecoder.class,"收到数据长度："+data.length);
            //通过消息实体构造方法确定其消息类型和设备地址
            InternalMsgBody msgBody = new InternalMsgBody(data);
            if(msgBody.getMsgType() != InternalMsgType.INVALID_PACKAGE) out.add(msgBody);
        }
    }

}
