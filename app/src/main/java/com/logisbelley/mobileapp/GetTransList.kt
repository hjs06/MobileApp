package com.logisbelley.mobileapp

import com.google.gson.annotations.SerializedName

class GetTransList {
    @SerializedName("status")
    val status = ""

    val data: ArrayList<Data> = ArrayList<Data>()

    class Data {
        @SerializedName("CENTER_CD")
        val CENTER_CD = ""

        @SerializedName("DELI_DY")
        val DELI_DY = ""

        @SerializedName("DELI_SEQ")
        val DELI_SEQ = ""

        @SerializedName("ROUTE_ID")
        val ROUTE_ID = ""

        @SerializedName("STOP_SEQ")
        val STOP_SEQ = ""

        @SerializedName("OMS_WAYBILL_NO")
        val OMS_WAYBILL_NO = ""

        @SerializedName("OMS_PACKING_NO")
        val OMS_PACKING_NO = ""

        @SerializedName("OMS_EX_NO")
        val OMS_EX_NO = ""

        @SerializedName("STOP_NM")
        val STOP_NM = ""

        @SerializedName("STOP_CD")
        val STOP_CD = ""

        @SerializedName("UNLOAD_SCAN_YN")
        val UNLOAD_SCAN_YN = ""

        @SerializedName("UNLOAD_RSN_CD")
        val UNLOAD_RSN_CD = ""

        @SerializedName("STOP_SEQ_VW")
        val STOP_SEQ_VW = ""

    }
}