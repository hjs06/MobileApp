package com.logisbelley.mobileapp

import com.google.gson.annotations.SerializedName

/**
 * 기본 Response 모델
 */
class BaseResponseModel {

    @SerializedName("status")
    val status = ""

    @SerializedName("data")
    val data = ""

    @SerializedName(KeyInfo.KEY_PARAM)
    val param = ""
}