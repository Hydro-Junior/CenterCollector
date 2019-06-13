package com.xjy.test;

import com.xjy.entity.InternalMsgBody;
import com.xjy.sender.XTProtocolSendHelper;
import com.xjy.util.ConvertUtil;
import org.junit.Test;

/**
 * @Author: Mr.Xu
 * @Date: Created in 20:45 2018/10/17
 * @Description:
 */
public class TesterForByteTrans {
    @Test
    public void testConvertByte(){
        System.out.println(ConvertUtil.fixedLengthHex(-36));
    }
    @Test
    public void testSendData(){
        int[] effectiveBytes = new int[]{(byte)0x52,(byte)0x52,(byte)0x52,(byte)0x03};
        InternalMsgBody internalMsgBody = new InternalMsgBody("15108379391",effectiveBytes);
        byte[] bytes = internalMsgBody.toBytes();
        int[] res = new int[bytes.length];
        for(int i = 0 ; i < bytes.length; i++){
            res[i] = bytes[i];
            System.out.print(ConvertUtil.fixedLengthHex(res[i]) + " ");
        }
    }
    @Test
    public void testBINNumIN2bytes(){
        int[] data = new int[2];
        XTProtocolSendHelper.arrangeBINCodeIn2Bytes(640,0,data);
        System.out.print(ConvertUtil.fixedLengthHex(data[0]) + " " + ConvertUtil.fixedLengthHex(data[1]));
    }

    @Test
    public void testbytesToInt(){
        int[] data = new int[2];
        data[0] = 0xff; data[1] = 0x02;
        System.out.println(ConvertUtil.binBytesToInt(data,0,data.length-1));
    }
}
