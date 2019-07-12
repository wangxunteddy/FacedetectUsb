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

    // 其他信息（护照等，根据10E护照阅读器的返回数据定义）
    public String ReadedCardType = "";
    public String oriimage = "";
    public String RFIDMRZ = "";
    public String LocalName = "";
    public String OCRMRZ = "";
    public String EngName = "";
    public String POBPinyin = "";
    public String IssuePlacePinyin = "";
    public String DOB = "";
    public String IDCardNo = "";
    public String PassportMRZ = "";
    public String ValidDate = "";
    public String IssueState = "";
    public String SelfDefineInfo = "";
    public String EngSurname = "";
    public String POB = "";
    public String EngFirstname = "";
    public String CardNoMRZ = "";
    public String MRZ1 = "";
    public String CardNo = "";
    public String MRZ2 = "";
    public String CardType = "";
    public String Nationality = "";
    public String ChnName = "";
    public String PassportNo = "";
    public String ValidDateTo = "";
    public String BirthPlace = "";
    public String MRZ3 = "";
    public String ExchangeCardTimes = "";
    public String FprInfo = "";

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

        // 其他
        ReadedCardType = "";
        oriimage = "";
        RFIDMRZ = "";
        LocalName = "";
        OCRMRZ = "";
        EngName = "";
        POBPinyin = "";
        IssuePlacePinyin = "";
        DOB = "";
        IDCardNo = "";
        PassportMRZ = "";
        ValidDate = "";
        IssueState = "";
        SelfDefineInfo = "";
        EngSurname = "";
        POB = "";
        EngFirstname = "";
        CardNoMRZ = "";
        MRZ1 = "";
        CardNo = "";
        MRZ2 = "";
        CardType = "";
        Nationality = "";
        PassportNo = "";
        ValidDateTo = "";
        BirthPlace = "";
        MRZ3 = "";
        ExchangeCardTimes = "";
        FprInfo = "";
    }
}
