package com.xjy.entity;

import com.xjy.parms.XTParams;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:54 2019/5/14
 * @Description:对单个字节的序列域信息进行分解
 * D7保留，D6:FIR（置1表示报文的第一帧） D5:FIN（置1表示报文的最后一帧） D4:CON（置1表示需要对报文进行确认）
 * D3-D0:帧序号
 */
public class XtSeqArea {
    int fir = XTParams.SEQ_IS_FIRST_FRAME;
    int fin = XTParams.SEQ_IS_FINAL_FRAME;
    int con = XTParams.SEQ_NEED_CHECK;
    int id ;
    public XtSeqArea(){}
    public XtSeqArea(int seq){
        fir = (seq >> 6) & 1;
        fin = (seq >> 5) & 1;
        con = (seq >> 4) & 1;
        id = seq & 0x0f;
    }
    public int getSeq(){
        return (fir << 6) | (fin << 5) | (con << 4) | id;
    }
}
