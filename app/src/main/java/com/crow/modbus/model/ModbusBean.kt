package com.crow.modbus.model


import com.google.gson.annotations.SerializedName

data class ModbusBean(

    @SerializedName("content")
    val mContent: List<Content>,

    @SerializedName("type")
    val mType: String
)