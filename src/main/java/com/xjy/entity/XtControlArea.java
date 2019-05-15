package com.xjy.entity;

import com.xjy.parms.XTParams;

/**
 * @Author: Mr.Xu
 * @Date: Created in 9:45 2019/5/14
 * @Description:对单个字节的控制域信息进行分解
 * D7表示传输方向位，D6表示启动标志位，D5、D4保留，D3-D0表示功能码
 */
public class XtControlArea {
    int dir = XTParams.DIR_SERVER_TO_CENTER; // dir = 0表示本机到集中器，dir=1表示集中器到本机
    int prm = XTParams.PRM_MASTER; // prm = 1表示报文来自启动站（即请求），prm = 0表示报文来自从动站(即响应)
    int func = XTParams.CTRL_FOR_DATA;

    public XtControlArea(){}
    public XtControlArea(int c) {
        dir = (c >> 7) & 1;
        prm = (c >> 6) & 1;
        func = c & 0x0f;
    }
    public int getC (){
        return (dir << 7) | (prm << 6) | func;
    }
    public static int generateControlArea(int dir, int prm, int func){
        return (dir << 7) | (prm << 6) | func;
    }
}
