package com.logisbelley.mobileapp

import android.content.Context
import android.content.SharedPreferences

/**
 * 내부DB 저장
 */
class MySharedPreferences(context: Context) {

    val PREFS_FILENAME = "prefs"
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var gpsParam: String?
        get() = prefs.getString("gpsParam", "")
        set(value) = prefs.edit().putString("gpsParam", value).apply()

    var gpsCycle: String?
        get() = prefs.getString("gpsCycle", "60")
        set(value) = prefs.edit().putString("gpsCycle", value).apply()

    var bluetoothAddress: String?
        get() = prefs.getString("bluetoothAddress", "")
        set(value) = prefs.edit().putString("bluetoothAddress", value).apply()

    var dataFile: String?
        get() = prefs.getString("dataFile", "")
        set(value) = prefs.edit().putString("dataFile", value).apply()

    var imei: String?
        get() = prefs.getString("imei", "")
        set(value) = prefs.edit().putString("imei", value).apply()

    var param: String?
        get() = prefs.getString("param", "")
        set(value) = prefs.edit().putString("param", value).apply()

    var rsnCode: String?
        get() = prefs.getString("rsnCode", "")
        set(value) = prefs.edit().putString("rsnCode", value).apply()


    var dvName: String?
        get() = prefs.getString("dvName", "")
        set(value) = prefs.edit().putString("dvName", value).apply()

    var carNum: String?
        get() = prefs.getString("carNum", "")
        set(value) = prefs.edit().putString("carNum", value).apply()

    var drvNo: String?
        get() = prefs.getString("drvNo", "")
        set(value) = prefs.edit().putString("drvNo", value).apply()

    var transRoutId: String?
        get() = prefs.getString("transRoutId", "")
        set(value) = prefs.edit().putString("transRoutId", value).apply()

}