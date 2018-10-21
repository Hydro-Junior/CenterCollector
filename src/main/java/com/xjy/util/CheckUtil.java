package com.xjy.util;

import com.xjy.parms.Constants;

import java.util.zip.CRC32;

/**
 * @Author: Mr.Xu
 * @Date: Created in 20:23 2018/9/27
 * @Description:
 */
public class CheckUtil {
    /**
     *
     * @param data 包括有效数据的字节数组（用int表示），最后一个单元是校验和
     * @param offset 偏置量，跳过无效数据
     * @return
     */
    public static boolean doSumCheck(int[] data,int offset){
        int checkSum = 0;
        for(int i = offset ; i < data.length - 1; i++){
            checkSum += data[i];
            checkSum &= 0xff;
        }
        if(checkSum == data[data.length-1]){
            return true;
        }else{
            return false;
        }
    }

    /**
     *
     * @param data 待校验的有效数据
     * @param crcCode crc校验码
     * 生成多项式遵循标准CRC-CCITT G＝[1 0001 0000 0010 0001]，用多项式形式表示为G(x)＝x16＋x12＋x5＋1，
     * 由它产生的检验码R的二进制位数是16位（2字节）这里采用查表法
     * @return 校验和结果
     */
    public static boolean crcCheck(int[] data,int crcCode){
        int crc = getCrc(0,data,data.length);
        System.out.println("crc:"+ConvertUtil.fixedLengthHex(crc) + " crcCode:"+ConvertUtil.fixedLengthHex(crcCode));
        return crc == crcCode;
    }
    public static int getCrc(int initCRC, int[] buffer, int len) {
        int CrcA = buffer[0];
        int CrcB = buffer[1];
        int CrcC = buffer[2];
        for (int i = 2; i < len; i++)
        {
            CrcC = Constants.CRC_LOW[CrcA] ^ CrcC;
            CrcA = Constants.CRC_HIGH[CrcA] ^ CrcB;
            CrcB = CrcC;
            if (i == len - 1)
            {
                break;
            }
            CrcC = buffer[i + 1];
        }
        return (CrcB | (CrcA << 8));
    }
}
