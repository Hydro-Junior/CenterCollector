package com.xjy.test;

import com.xjy.decoder.InternalProtocolDecoder;
import com.xjy.entity.Center;
import com.xjy.entity.Command;
import com.xjy.entity.XtControlArea;
import com.xjy.entity.XtMsgBody;
import com.xjy.parms.XTParams;
import com.xjy.util.ConvertUtil;
import com.xjy.util.InternalProtocolSendHelper;
import com.xjy.util.XTProtocolSendHelper;
import org.junit.Test;

/**
 * @Author: Mr.Xu
 * @Date: Created in 15:16 2019/5/15
 * @Description:
 */
public class TesterFor130 {
    @Test
    public void testGetC(){
        int a = XtControlArea.generateControlArea(XTParams.DIR_SERVER_TO_CENTER,XTParams.PRM_MASTER,XTParams.CTRL_FOR_DATA);
        System.out.println(Integer.toHexString(a));
    }
    @Test
    public void testGetAddressArea(){
        int[] A = XTProtocolSendHelper.getAddressArea("201951401");
        for(int i = 0; i < A.length; i++){
            System.out.print(ConvertUtil.fixedLengthHex(A[i]) + " ");
        }
    }
    @Test
    public void testGetFn(){
        int[] f = XTProtocolSendHelper.getFn(57);
        System.out.println(ConvertUtil.fixedLengthHex(f));
    }
    @Test
    public void testToBytes(){
        byte[] bytes = new XtMsgBody(0x70,new int[]{0x07,0x13,0xf4,0xd6,0x00},0x02,0x70,new
        int[]{0x00,0x00,0x01,0x00},null).toBytes();
        System.out.println(ConvertUtil.fixedLengthHex(ConvertUtil.bytesToInts(bytes)));
    }
    @Test
    public void testReadFileSyntax(){
        Command c = new Command();
        c.setParameter(0);
        XTProtocolSendHelper.getFileInfo(new Center("201951401",null),c);
    }
    @Test
    public void testReadMetersCode(){
        Command c = new Command();
        c.setParameter(0);
        XTProtocolSendHelper.readMeters(new Center("201951401",null),c);
    }
}
