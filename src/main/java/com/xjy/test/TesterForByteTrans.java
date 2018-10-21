package com.xjy.test;

import com.xjy.entity.InternalMsgBody;
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
}
