package com.logisbelley.mobileapp

import com.google.gson.annotations.SerializedName

class GetLoadList {

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

        @SerializedName("OMS_EX_NO")
        val OMS_EX_NO = ""

        @SerializedName("LOAD_SCAN_YN")
        val LOAD_SCAN_YN = ""

        @SerializedName("LOAD_RSN_CD")
        val LOAD_RSN_CD = ""

        @SerializedName("UNLOAD_SCAN_YN")
        val UNLOAD_SCAN_YN = ""

        @SerializedName("UNLOAD_RSN_CD")
        val UNLOAD_RSN_CD = ""

        //차량번호
        @SerializedName("DRV_NO")
        val DRV_NO = ""

        @SerializedName("DELI_NO")
        val DELI_NO = ""

        @SerializedName("OMS_PACKING_NO")
        val OMS_PACKING_NO = ""

        @SerializedName("ODR_STS_NM")
        val ODR_STS_NM = ""

        @SerializedName("WCS_ID")
        val WCS_ID = ""

        @SerializedName("STOP_SEQ_VW")
        val STOP_SEQ_VW = ""

    }

}