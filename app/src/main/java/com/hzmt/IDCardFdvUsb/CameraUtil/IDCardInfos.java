package com.hzmt.IDCardFdvUsb.CameraUtil;

import java.io.Serializable;

public class IDCardInfos implements Cloneable, Serializable {
    // 身份证信息
    public String name = "";
    public String issuing_authority = "";
    public String idcard_photo = "";        // base64 string
    public String birthdate = "";
    public String sex = "";
    public String idcard_issuedate = "";
    public String idcard_expiredate = "";
    public String idcard_id = "";
    public String ethnicgroup = "";
    public String address = "";

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();// 浅拷贝
    }

    public void clean()
    {
        name = "";
        issuing_authority = "";
        idcard_photo = "";
        birthdate = "";
        sex = "";
        idcard_issuedate = "";
        idcard_expiredate = "";
        idcard_id = "";
        ethnicgroup = "";
        address = "";
    }
}
