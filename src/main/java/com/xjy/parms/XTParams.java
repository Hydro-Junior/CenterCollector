package com.xjy.parms;

/**
 * @Author: Mr.Xu
 * @Date: Created in 10:15 2019/5/14
 * @Description:130协议相关的参数
 */
public class XTParams {
    //控制域参数dir和prm
    public static final int DIR_SERVER_TO_CENTER = 0; //本机到集中器,下行报文
    public static final int DIR_CENTER_TO_SERVER = 1; //集中器到本机，上行报文
    public static final int PRM_MASTER = 1; //启动标志
    public static final int PRM_SLAVE = 0; //从动标志

    //控制域功能码(控制域的后四位) PRM = 1时，其实具体的控制域的值C可直接参考按文档的样例
    public static final int CTRL_RESET = 1;
    public static final int CTRL_HAERTBEAT = 9;
    public static final int CTRL_FOR_ACK = 10;
    public static final int CTRL_FOR_DATA = 11;

    //应用层功能码afn
    public static final int AFN_CONFIRM_OR_DENY = 0x00;
    public static final int AFN_RESET = 0x01;
    public static final int AFN_LINK_DETECTION = 0X02;
    public static final int AFN_SET_PARAM = 0X84; //配合F1用于下载档案
    public static final int AFN_CONTROL = 0X85; //用于开关阀（F1校时 F2关阀跳闸 F3开阀合闸）
    public static final int AFN_GET_PARAM = 0X8A; //F1读取档案
    public static final int AFN_GET_REALTIME_DATA = 0X8C; //请求一类数据，实时数据（F1抄读所有表，F2抄读单表）

    //序列域seq
    public static final int SEQ_IS_FIRST_FRAME = 1;
    public static final int SEQ_IS_NOT_FIRST_FRAME = 0;
    public static final int SEQ_IS_FINAL_FRAME = 1;
    public static final int SEQ_IS_NOT_FINAL_FRAME = 0;
    public static final int SEQ_NEED_CHECK = 1;
    public static final int SEQ_NEED_NOT_CHECK = 0;

}
