package com.logisbelley.mobileapp

import android.app.Application
import android.location.Location
import android.os.Parcelable
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kakao.sdk.common.KakaoSdk
import com.logisbelley.mobileapp.BuildConfig.URL_BASE
import io.reactivex.plugins.RxJavaPlugins
import org.json.JSONArray

/**
 * 어플리케이션
 */
class LogisbelleyApplication : Application() {

    companion object {
        var recyclerViewState: Parcelable? = null
        var MAIN_URL = "$URL_BASE"

        var FLAG_GALLERY: Int = 77
        var FLAG_CAMERRA: Int = 66
        var curLocation: Location? = null
        lateinit var preferences: MySharedPreferences
        var TIME: String = ""

        var scanTypeName = arrayListOf<String>()
        var scanTypeCode = arrayListOf<String>()
        var transBarcodeList = arrayListOf<String>()
        var isBarCodeCheck = false
        // 원복을 위한 백업 데이터
        var transbackupScanLists = JSONArray()
        var isbarCodeAlert = false
        // isFlashOnMode True: 플래쉬 on 상태
        var isFlashOnMode: Boolean = false
        var stopSeq = ""

        var isScannerOn = true


        fun setFlashON(onOff: Boolean) {
            isFlashOnMode = onOff
        }

    }

    override fun onCreate() {
        super.onCreate()
        preferences = MySharedPreferences(applicationContext)
        RxJavaPlugins.setErrorHandler { e: Throwable? -> }
//        Fabric.with(this, Crashlytics())
        FirebaseCrashlytics.getInstance()
//        FirebaseApp.initializeApp(this)

        //카카오 네비 init
        KakaoSdk.init(this, "3be57bffe036797643364b429615f6a6")
    }
}