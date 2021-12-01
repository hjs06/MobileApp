package com.logisbelley.mobileapp

import com.google.gson.annotations.SerializedName

class GetTakeList {
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

        @SerializedName("DELI_NO")
        val DELI_NO = ""

        @SerializedName("OMS_WAYBILL_NO")
        val OMS_WAYBILL_NO = ""

        @SerializedName("OMS_PACKING_NO")
        val OMS_PACKING_NO = ""

        @SerializedName("OMS_EX_NO")
        val OMS_EX_NO = ""

        @SerializedName("STOP_NM")
        val STOP_NM = ""

        @SerializedName("LOAD_SCAN_YN")
        val LOAD_SCAN_YN = ""

        @SerializedName("ODR_STS_NM")
        val ODR_STS_NM = ""

        @SerializedName("LOAD_RSN_CD")
        val LOAD_RSN_CD = ""

        @SerializedName("ROUTE_ID_ORG")
        val ROUTE_ID_ORG = ""

        @SerializedName("STOP_SEQ_ORG")
        val STOP_SEQ_ORG = ""

        @SerializedName("DRV_NO")
        val DRV_NO = ""

        @SerializedName("STOP_SEQ_VW")
        val STOP_SEQ_VW = ""



    }
}