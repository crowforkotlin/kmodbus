package com.crow.modbus.model


import com.google.gson.annotations.SerializedName

data class RegisterData(

    @SerializedName("registerAddress")
    val registerAddress: String,

    @SerializedName("registerFieldLen")
    val registerFieldLen: String,

    @SerializedName("registerFieldName")
    val registerFieldName: String,

    @SerializedName("registerFieldPrecision")
    val registerFieldPrecision: String,

    @SerializedName("registerFieldType")
    val registerFieldType: String,

    @SerializedName("registerFieldUnit")
    val registerFieldUnit: String
)