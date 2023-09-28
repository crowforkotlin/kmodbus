package com.crow.modbus.model


import com.google.gson.annotations.SerializedName

data class Content(

    @SerializedName("baudRate")
    val mBaudRate: String,

    @SerializedName("breakTimeOut")
    val mBreakTimeOut: String,

    @SerializedName("commMode")
    val mCommMode: String,

    @SerializedName("commType")
    val mCommType: String,

    @SerializedName("connTimeOut")
    val mConnTimeOut: String,

    @SerializedName("crcMode")
    val mCrcMode: String,

    @SerializedName("dataBits")
    val mDataBits: String,

    @SerializedName("dataTypeRule")
    val mDataTypeRule: String,

    @SerializedName("flowControll")
    val mFlowControll: String,

    @SerializedName("hostMode")
    val mHostMode: String,

    @SerializedName("listenPort")
    val mListenPort: String,

    @SerializedName("parityBits")
    val mParityBits: String,

    @SerializedName("registerDatas")
    val mRegisterDatas: List<RegisterData>,

    @SerializedName("serverIp")
    val mServerIp: String,

    @SerializedName("serverPort")
    val mServerPort: String,

    @SerializedName("stopBits")
    val mStopBits: String
)