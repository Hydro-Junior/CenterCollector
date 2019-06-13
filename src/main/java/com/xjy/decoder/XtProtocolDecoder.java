package com.xjy.decoder;

import com.xjy.entity.XtMsgBody;
import com.xjy.parms.Constants;
import com.xjy.util.CheckUtil;
import com.xjy.util.ConvertUtil;
import com.xjy.util.LogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:15 2018/9/27
 * @Description: 按新天通讯协议对字节流进行初步解码
 * 所有多字节数据域均先传送低位字节，后传高位字节
 * 帧格式如下（具体协议见文档）：
 * 起始字符（68H）			1字节
    长度L		            2字节
    长度L			        2字节
   起始字符（68H）		    1字节

   控制域C（报文传输方向和所提供的传输服务类型）
   地址域A（由行政区划码A1<BCD,2字节>、终端地址A2<BIN,2字节>、主站地址和组地址标志A3组成<BIN,1字节>）
   链路用户数据（应用层功能码，帧序列域和数据单元标识）
                        —— —— —— —— —— —— 共L字节
   校验和CS		            1字节
   结束字符（16H）			1字节
   报文总长： L+8字节
 */
public class XtProtocolDecoder extends ByteToMessageDecoder {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("解码时异常");
        cause.printStackTrace();
        ctx.close();
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 20) return; //小于最小报文长度，不做处理
        int rd = in.readerIndex();
       // System.out.println("readerIndex:" + rd);
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        /**
         * 由于超过127的byte经java的强转后，高位都是1，为避免一些问题，统一 &0xff 取后8位，转为int处理
         */
        int[] data = ConvertUtil.bytesToInts(bytes);
        if(data.length > 20 || (data.length == 20 && data[6] != 0xC9)){
            StringBuffer sb = new StringBuffer();
            sb.append("收到原始报文："+ Constants.LINE_SEPARATOR);
            for(int i = 0 ;  i < data.length; i++){
                sb.append(ConvertUtil.fixedLengthHex(data[i]) + " ");
            }
            LogUtil.DataMessageLog(XtProtocolDecoder.class,sb.toString());
        }
        int[] effectiveData = null;
        int st = -1, i = 0;
        for(; i < data.length - 19; i++){
            if(data[i] == 0x68 && data[i+5] == 0x68){
                st = i; break;
            }
        }
        if(st == -1){//没有找到报文头
            in.readerIndex(rd+ (i == 0 ? i : --i)); return;
        }else{
            int LL = data[1+st]; //先低位
            int LH = data[2+st]; //后高位
            int L = (LH << 8 | LL) >> 2; //得到长度（最后两位是规约标志位，忽略）
            if(st + 5 + L < data.length - 2 && data[L + st + 5 + 2] == 0x16){
                 in.readerIndex(rd + st + 8 + L); // 重置读取位置，比如第一个0x68的位置是0，则读了0+6+L+2个字节
                 in.discardReadBytes(); //丢弃已读报文
                 //System.out.println(in.readerIndex());
                 //作校验和
                if(CheckUtil.doSumCheck(data,st + 6,st + 6 +L, data[st + 6 + L])){
                    effectiveData = new int[L];
                    //System.out.println("有效数据部分");
                    for(int j = 0; j < L ; j ++){
                        effectiveData[j] = data[st + 6 + j] & 0xff;
                        //System.out.print(ConvertUtil.fixedLengthHex(effectiveData[j])+" ");
                    }
                    XtMsgBody xtMsgBody = new XtMsgBody(effectiveData);
                    out.add(xtMsgBody);
                }
            }else{ //可能报文尾在下一段
                in.readerIndex(rd);
                return;
            }
        }
       /* if(data[0] == 0x68){
           effectiveData = getEffectiveData(data);
           //if(effectiveData != null) out.add(effectiveData);
        }else{//以下是特殊情况，一般不会出现。如果判断出第一个字节不是报文头，进行容错处理（有其他字节混入的情况，不处理的话可能会丢包）
            // 当然如果数据混入包内，就不会是有效包了）
            int[] dataPart = null;
            System.out.println("出现报文头不对情况，当前数据长度："+data.length);
            for(int i = 0; i < data.length - 7;){
                if(data[i] == 0x68){
                    System.out.println("找到报文头！");
                    dataPart = new int[data.length-i];
                    for(int j = 0 ; j < dataPart.length; j++){dataPart[j] = data[i++];}
                }else{
                    i++;
                }
            }
            effectiveData = getEffectiveData(dataPart);
           // if(effectiveData != null) out.add(effectiveData);
        }
        //生成消息实体
        if(effectiveData != null && effectiveData.length >= 12){
            XtMsgBody xtMsgBody = new XtMsgBody(effectiveData);
            out.add(xtMsgBody);
        }else{
            LogUtil.DataMessageLog(XtProtocolDecoder.class,*//*"无法生成有效数据包! effectiveData:" +
                    ConvertUtil.fixedLengthHex(effectiveData)+Constants.LINE_SEPARATOR *//* "不明原始报文--->"+ sb.toString()
            );
        }*/
    }
    //过滤掉报文公共部分（包括做报文长度和校验和检测）
    public static int[] getEffectiveData(int[] data){
        if(data == null) return null;
        int LL = data[1]; //先低位
        int LH = data[2]; //后高位
        int L = (LH << 8 | LL) >> 2; //得到长度（最后两位是规约标志位，忽略）
        if(L + 7 == data.length){//长度符合要求（8个非数据字节去掉一个报文尾，剩7个）
            //校验和通过，得到有效消息
            if(CheckUtil.doSumCheck(data,6)){//偏置量为6，前6个字节的报文头不需要
                int[] effectiveData = new int[data.length - 7];
                for(int i = 0 ; i < effectiveData.length; i++){
                    effectiveData[i] = data[i+6];
                }
                return effectiveData;
            }
        }else{
           /* LogUtil.DataMessageLog(XtProtocolDecoder.class,"报文长度对应不上！"
            + Constants.LINE_SEPARATOR + "L + 7: "+ L + "  data.length:"+ data.length);*/
        }
        return null;
    }
}
