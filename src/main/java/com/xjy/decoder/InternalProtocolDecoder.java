package com.xjy.decoder;

import com.xjy.entity.InternalMsgBody;
import com.xjy.entity.XtMsgBody;
import com.xjy.parms.InternalMsgType;
import com.xjy.util.CheckUtil;
import com.xjy.util.ConvertUtil;
import com.xjy.util.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:17 2018/9/27
 * @Description: 按内部协议对字节流进行初步解码
 */
public class InternalProtocolDecoder extends ByteToMessageDecoder {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("内部协议解码时异常");
        cause.printStackTrace();
        ctx.close();
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 11) return; //小于最小报文长度，不做处理
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        /**
         * 由于超过127的byte经java的强转后，高位都是1，为避免一些问题，统一 &0xff 取后8位，转为int处理
         */
        int[] data = ConvertUtil.bytesToInts(bytes);
        LogUtil.DataMessageLog(InternalProtocolDecoder.class,"收到数据长度："+data.length);
        //通过消息实体构造方法确定其消息类型和设备地址
        InternalMsgBody msgBody = new InternalMsgBody(data);
        if(msgBody.getMsgType() != InternalMsgType.INVALID_PACKAGE ) out.add(msgBody);

    }

}
