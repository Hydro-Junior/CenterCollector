package com.xjy.test;

import com.xjy.decoder.InternalProtocolDecoder;
import com.xjy.entity.XtControlArea;
import com.xjy.parms.XTParams;
import com.xjy.util.ConvertUtil;
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
        int[] f = XTProtocolSendHelper.getFn(1);
        System.out.println(ConvertUtil.fixedLengthHex(f));
    }
}
