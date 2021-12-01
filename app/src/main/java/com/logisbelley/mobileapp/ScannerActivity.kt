package com.logisbelley.mobileapp

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.zxing.integration.android.IntentIntegrator
import com.logisbelley.mobileapp.InterfaceApi.Companion.getLoadList
import com.logisbelley.mobileapp.InterfaceApi.Companion.getUnloadList
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isFlashOnMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONObject


/**
 * 상&하차 검수 바코드 요청시 실행되는 화면
 */
class ScannerActivity : Activity() {
    // isScanLoad : True = 상차, false = 하차

    private var compositeDisposable = CompositeDisposable()
    private var bigo = false

    companion object {
        var isScanLoad = true
        @kotlin.jvm.JvmField
        var isBigoCheck: Boolean = false

        // 실제 스캔된 데이터
        var scanLists = JSONArray()

        // 원복을 위한 백업 데이터
        var backupScanLists = JSONArray()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView((R.layout.activity_scanner))


        val param = intent.extras!!.get(KeyInfo.KEY_PARAM) as String
        val rsnCode = intent.extras!!.get(KeyInfo.KEY_RSN_CODE) as String

        if (intent.extras!!.get(KeyInfo.KEY_TYPE) != null) {
            isScanLoad = false
        }
//        var scanListStr = intent.extras!!.get(KeyInfo.KEY_SCAN_LIST) as String
//
//        scanList =  JSONArray(scanListStr)
        getLoadList(param)


    }

    /**
     * 바코드 화면 실행
     */
    //barcode: 방금 스캔한 아이템
    fun startBarcode(message: String, isFinish: Boolean, barcode: String, scroll: String) {

        val integrator = IntentIntegrator(this)
        integrator.captureActivity = CustomScannerActivity::class.java
        integrator.setBeepEnabled(false)
        integrator.addExtra(KeyInfo.KEY_SCAN_LIST, scanLists)
        if (isFinish) {
            integrator.addExtra(KeyInfo.KEY_FINISH_YN, "true")
        }
        integrator.addExtra(KeyInfo.KEY_MESSAGE, message)
        if (!isScanLoad) {
            integrator.addExtra(KeyInfo.KEY_TYPE, "ture")
        }
        if (barcode != "") {
            integrator.addExtra(KeyInfo.KEY_BARCODE, barcode)
        }
        if (scroll != "") {
            integrator.addExtra(KeyInfo.KEY_SCROLL_POS, scroll)
        }
        overridePendingTransition(0, 0)
        integrator.setBeepEnabled(true)

        integrator.initiateScan()

    }


    override fun onDestroy() {
        super.onDestroy()

        // 화면 종료될땐 off 초기화
        isFlashOnMode = false

        if (compositeDisposable != null) {
            if(compositeDisposable.isDisposed) {
                compositeDisposable.dispose()
            }
        }
    }


    fun getLoadList(param: String) {
        val activity: Activity = this
        compositeDisposable = CompositeDisposable()

        if (isScanLoad) {
            compositeDisposable.add(
                getLoadList(this, param)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: GetLoadList ->
                        settingData(response.data)
                    }, { error: Throwable ->
                        error.printStackTrace()
                    })
            )
        } else {
            compositeDisposable.add(
                getUnloadList(this, param)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: GetLoadList ->
                        settingData(response.data)
                    }, { error: Throwable ->
                        error.printStackTrace()
                    })
            )
        }

    }

    private fun OnVibratorSound(isSuccess: Boolean) {
        try {
            runOnUiThread {
                var sec = 100.toLong()
                if (!isSuccess) {
                    sec = 600.toLong()
                }
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            sec,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    );
                } else {
                    vibrator.vibrate(sec)

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * scanNumber : 원복을 위해 사용한다 .
     */
    private fun saveList(
        jsonArray: JSONArray,
        isActivityFinish: Boolean,
        barcode: String,
        scroll: String
    ) {

        val activity: Activity = this

        compositeDisposable = CompositeDisposable()

        if (isScanLoad) {
            compositeDisposable.add(
                InterfaceApi.saveLoad(this, jsonArray)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: BaseResponseModel ->
                        if (response.status == "200") {
                            //성공
                            when {
                                response.data == "SUCCESS" -> {
                                    //  성공
                                    if (isActivityFinish) {

                                        startBarcode(
                                            getString(R.string.load_complete),
                                            isActivityFinish,
                                            "",
                                            scroll
                                        )
                                    } else {
                                        OnVibratorSound(true)
                                        startBarcode("", isActivityFinish, barcode, scroll)
                                    }

                                }
                                response.data == "FAIL" -> {
                                    // 실패 (저장 실패)
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data == "FAIL_TMS_API" -> {
                                    //실패 (운송장번호가 DB에 없을시):
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    startBarcode(
                                        getString(R.string.error_scan_cancel),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data == "FAIL_NODATA" -> {
                                    //실패 (운송장번호가 DB에 없을시):
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_no_barcode_number),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data.contains("FAIL") -> {
                                    //실패 (운송장번호가 다른사람일경우) :
                                    val spritData = response.data.split("_")
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    val A: String = spritData[1]
                                    val B: String = spritData[2]
                                    OnVibratorSound(false)
                                    startBarcode(
                                        "배송할 상품이 아닙니다.\n본 상품은 $A 권역의\n $B 기사님에게 할당되었습니다.",
                                        isActivityFinish
                                        , ""
                                        , scroll
                                    )
                                }
                                else -> {
                                    //그밖
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save), isActivityFinish, ""
                                        , scroll
                                    )

                                }
                            }

                        } else {
                            OnVibratorSound(false)
                            //실패
                            startBarcode(
                                getString(R.string.error_barcode_save),
                                isActivityFinish,
                                "",
                                scroll
                            )

                        }
//                        if (isActivityFinish) {
//                            finish()
//                        } else {
//                            startBarcode("", isActivityFinish)
//                        }
                    }, { error: Throwable ->
                        // 실패시 문구
                        OnVibratorSound(false)
                        startBarcode(
                            getString(R.string.error_barcode_save),
                            isActivityFinish,
                            "",
                            scroll
                        )
                        error.printStackTrace()
                    })
            )
        } else {
            compositeDisposable.add(
                InterfaceApi.saveUnload(this, jsonArray)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: BaseResponseModel ->

                        if (response.status == "200") {
                            //성공
                            when {
                                response.data == "SUCCESS" -> {
                                    //  성공
                                    if (isActivityFinish) {

                                        startBarcode(
                                            getString(R.string.unload_complete),
                                            isActivityFinish,
                                            "",
                                            scroll
                                        )
                                    } else {
                                        OnVibratorSound(true)
                                        startBarcode("", isActivityFinish, barcode, scroll)
                                    }
                                }
                                response.data == "FAIL" -> {
                                    // 실패 (저장 실패)
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data == "FAIL_NODATA" -> {
                                    //실패 (운송장번호가 DB에 없을시):
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_no_barcode_number),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data.contains("FAIL") -> {
                                    //실패 (운송장번호가 다른사람일경우) :
                                    val spritData = response.data.split("_")
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    val A: String = spritData[1]
                                    val B: String = spritData[2]
                                    OnVibratorSound(false)
                                    startBarcode(
                                        "배송할 상품이 아닙니다.\n본 상품은 $A 권역의\n $B 기사님에게 할당되었습니다.",
                                        isActivityFinish
                                        , ""
                                        , scroll
                                    )
                                }
                                else -> {
                                    //그밖
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )

                                }
                            }

                        } else {
                            OnVibratorSound(false)
                            //실패
                            startBarcode(
                                getString(R.string.error_barcode_save),
                                isActivityFinish,
                                "",
                                scroll
                            )

                        }
//                        if (isActivityFinish) {
//                            finish()
//                        } else {
//                            startBarcode("", isActivityFinish)
//                        }

                    }, { error: Throwable ->
                        // 실패시 문구
                        OnVibratorSound(false)
                        startBarcode(
                            getString(R.string.error_barcode_save),
                            isActivityFinish,
                            "",
                            scroll
                        )
                        error.printStackTrace()
                    })
            )

        }
    }

    /**
     * 전송받은 데이터 셋팅
     */
    private fun settingData(list: ArrayList<GetLoadList.Data>) {
        scanLists = JSONArray()
        for (i in 0 until list.size) {
            val jsonObject = JSONObject()
            val data = list[i]

            jsonObject.put(KeyInfo.KEY_OMS_EX_NO, data.OMS_EX_NO)
            jsonObject.put(KeyInfo.KEY_CENTER_CD, data.CENTER_CD)
            jsonObject.put(KeyInfo.KEY_DELI_SEQ, data.DELI_SEQ)
            jsonObject.put(KeyInfo.KEY_DELI_DY, data.DELI_DY)
            jsonObject.put(KeyInfo.KEY_ROUTE_ID, data.ROUTE_ID)
            jsonObject.put(KeyInfo.KEY_STOP_SEQ, data.STOP_SEQ)

            jsonObject.put(KeyInfo.KEY_OMS_WAYBILL_NO, data.OMS_WAYBILL_NO)
            jsonObject.put(KeyInfo.KEY_OMS_PACKING_NO, data.OMS_PACKING_NO)
            jsonObject.put(KeyInfo.KEY_ODR_STS_NM, data.ODR_STS_NM)
            jsonObject.put(KeyInfo.KEY_WCS_ID,data.WCS_ID)

            if (data.DELI_NO != null) {
                if (data.DELI_NO != "") {
                    jsonObject.put(KeyInfo.KEY_DELI_NO, data.DELI_NO)
                }
            }

            if (isScanLoad) {
                jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, data.LOAD_SCAN_YN)
                jsonObject.put(KeyInfo.KEY_LOAD_RSN_CD, data.LOAD_RSN_CD)
            } else {
                jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, data.UNLOAD_SCAN_YN)
                jsonObject.put(KeyInfo.KEY_UNLOAD_RSN_CD, data.UNLOAD_RSN_CD)
            }

            jsonObject.put(KeyInfo.KEY_DRV_NO, data.DRV_NO)
            jsonObject.put(KeyInfo.KEY_STOP_SEQ_VW,data.STOP_SEQ_VW)
            scanLists.put(jsonObject)
        }
        startBarcode("", false, "", "0")
//        scanBarcodeSetting("MODELINOA08032")
    }


    /**
     * 바코드 스캔시 처리
     */
    private fun scanBarcodeSetting(barcode: String) {
        val backpList = scanLists
        backupScanLists = JSONArray()
        backupScanLists = scanLists

        val semdScanList = JSONArray()
        // 목록에 있는 리스트인지 체크
        var isCheck = false
        // 스캔 항목인지 체크 한 항목인지 체크
        var isScanYn = false

        var pos = 0

        scanLists = JSONArray()

        for (i in 0 until backpList.length()) {
            val jsonObject = backpList.getJSONObject(i)
            try {
                if (jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO) == barcode) {
                    if (jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN) == "1"
                        || jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN) == "O"
                    ) {
                        isScanYn = true
                    }
                    jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "1")
                    scanLists.put(jsonObject)
                    semdScanList.put(jsonObject)
                    pos = i
                    isCheck = true
                } else {
                    scanLists.put(jsonObject)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // 운송장번호가 있는경우
        if (isCheck) {
            //상차+스캔항항목
            if (isScanLoad && isScanYn) {
                OnVibratorSound(false)
                startBarcode(getString(R.string.already_scanned_barcode), false, "", "0")
            } else {
                saveList(semdScanList, false, barcode, Integer.toString(pos))
            }

        } else {
            // 운송장번호가 없는 경우
            // 상차인 경우 => 서버 전송
            // 하차인 경우 => 바로 에러 처리
            if (backpList.length() > 0) {
                // 데이터 설정
                val jsonObject = JSONObject()
                jsonObject.put(KeyInfo.KEY_OMS_WAYBILL_NO, barcode)
                jsonObject.put(KeyInfo.KEY_OMS_EX_NO, "")
                jsonObject.put(KeyInfo.KEY_STOP_SEQ, "")
                jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "1")
                jsonObject.put(KeyInfo.KEY_LOAD_RSN_CD, "0")
                jsonObject.put(
                    KeyInfo.KEY_CENTER_CD,
                    backpList.getJSONObject(0).getString(KeyInfo.KEY_CENTER_CD)
                )
                jsonObject.put(
                    KeyInfo.KEY_DELI_DY,
                    backpList.getJSONObject(0).getString(KeyInfo.KEY_DELI_DY)
                )
                jsonObject.put(
                    KeyInfo.KEY_DELI_SEQ,
                    backpList.getJSONObject(0).getString(KeyInfo.KEY_DELI_SEQ)
                )
                jsonObject.put(
                    KeyInfo.KEY_ROUTE_ID,
                    backpList.getJSONObject(0).getString(KeyInfo.KEY_ROUTE_ID)
                )
                jsonObject.put(
                    KeyInfo.KEY_DRV_NO,
                    backpList.getJSONObject(0).getString(KeyInfo.KEY_DRV_NO)
                )
                semdScanList.put(jsonObject)
            }
            // 하차+목록에 없는 경우
            if (!isScanLoad && !isCheck) {
                scanLists = backupScanLists
                OnVibratorSound(false)
                startBarcode(getString(R.string.error_barcode), false, "", "0")
            } else {
                saveList(semdScanList, false, barcode, "")
            }
        }

    }

    /**
     * 스캔 바코드 설정
     */
    fun scanBarcodeSetting(position: Int, code: String, scroll: Int) {
        val backpList = scanLists
        backupScanLists = JSONArray()
        backupScanLists = scanLists
        var semdScanList = JSONArray()
        var isCheck = false
        scanLists = JSONArray()
        for (i in 0 until backpList.length()) {
            var jsonObject = backpList.getJSONObject(i)
            try {

                if (position == i) {
                    if (jsonObject.has(KeyInfo.KEY_LOAD_RSN_CD)) {
                        jsonObject.put(KeyInfo.KEY_LOAD_RSN_CD, code)
                    } else {
                        jsonObject.put(KeyInfo.KEY_UNLOAD_RSN_CD, code)
                    }
                    scanLists.put(jsonObject)
                } else {
                    scanLists.put(jsonObject)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (bigo){
        }else{
            startBarcode("", false, "", scroll.toString())
        }


    }


    /**
     * 테스트 코드
     * 전체 스캔 완료 처리를 위한 코드
     */
    private fun scanTest() {
        val backpList = scanLists
        backupScanLists = JSONArray()
        backupScanLists = scanLists
        scanLists = JSONArray()

        for (i in 0 until backpList.length()) {
            val jsonObject = backpList.getJSONObject(i)
            try {

                jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "1")
                scanLists.put(jsonObject)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        startBarcode("", false, "", "0")

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {

            //테스트 코드
            if (data != null && data.getStringExtra(KeyInfo.KEY_SCAN_TEST) != null) {
                scanTest()
                return
            }

            // 비고 선택시
            if (data != null && data.getStringExtra(KeyInfo.KEY_ETC_SELECT) != null) {
                bigo = true
                val position = data!!.getStringExtra(KeyInfo.KEY_POSITION)
                val code = data!!.getStringExtra(KeyInfo.KEY_CODE)
                val barcode = data!!.getStringExtra(KeyInfo.KEY_OMS_WAYBILL_NO)
                scanBarcodeSetting(position.toInt(), code, 0)

                Log.e("scanLists",scanLists.toString())
                Log.e("barcode",barcode)
                Log.e("position",position)
                Log.e("code",code)


                //비고 선택시 추가 코드
                if (barcode != null){
                    saveList(scanLists, false, barcode, position,code)
                }
                return
            }

            // 완료 버튼시
            if (data != null && data.getStringExtra(KeyInfo.KEY_SCAN_COMPLETED) != null) {
                LogisbelleyApplication.recyclerViewState = null
                val sendScanList = JSONArray()
                for (i in 0 until scanLists.length()) {
                    val jsonObject = scanLists.getJSONObject(i)
                    try {
                        // 스캔안됨
                        // getList에서 가져올때는 ->x
                        // 기본은 0으로 전달해야함
                        if (jsonObject.get(KeyInfo.KEY_LOAD_SCAN_YN) == "X"
                            || jsonObject.get(KeyInfo.KEY_LOAD_SCAN_YN) == "0"
                        ) {
                            jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "0")
                        } else {
                            jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "1")
                        }

                        sendScanList.put(jsonObject)


                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                saveList(sendScanList, true, "", "")
                return
            }

            // 바코드 스캔시 넘어오는 바코드
            if (result.contents != null) {
                LogisbelleyApplication.recyclerViewState = null
                val barcodeNumber = result.contents
                scanBarcodeSetting(barcodeNumber)
            }else if (isBigoCheck){
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun saveList(
        jsonArray: JSONArray,
        isActivityFinish: Boolean,
        barcode: String,
        scroll: String,
        code: String
    ) {

        val activity: Activity = this

        compositeDisposable = CompositeDisposable()

        val sendData = JSONArray()
        sendData.put(jsonArray[scroll.toInt()])
        Log.e("sendData",sendData.toString())
        Log.e("isScanLoad",isScanLoad.toString())
        if (isScanLoad) {
            compositeDisposable.add(
                InterfaceApi.saveLoad(this, sendData)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: BaseResponseModel ->

                        if (response.status == "200") {
                            //성공
                            when {
                                response.data == "SUCCESS" -> {
                                    jsonArray.remove(scroll.toInt())
                                    startBarcode("미상차 처리가 완료되었습니다.", isActivityFinish, barcode, scroll)
                                }
                                response.data == "FAIL" -> {
                                    // 실패 (저장 실패)
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data == "FAIL_TMS_API" -> {
                                    //실패 (운송장번호가 DB에 없을시):
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    startBarcode(
                                        getString(R.string.error_scan_cancel),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data == "FAIL_NODATA" -> {
                                    //실패 (운송장번호가 DB에 없을시):
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_no_barcode_number),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data.contains("FAIL") -> {
                                    //실패 (운송장번호가 다른사람일경우) :
                                    val spritData = response.data.split("_")
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    val A: String = spritData[1]
                                    val B: String = spritData[2]
                                    OnVibratorSound(false)
                                    startBarcode(
                                        "배송할 상품이 아닙니다.\n본 상품은 $A 권역의\n $B 기사님에게 할당되었습니다.",
                                        isActivityFinish
                                        , ""
                                        , scroll
                                    )
                                }
                                else -> {
                                    //그밖
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save), isActivityFinish, ""
                                        , scroll
                                    )

                                }
                            }

                        } else {
                            OnVibratorSound(false)
                            //실패
                            startBarcode(
                                getString(R.string.error_barcode_save),
                                isActivityFinish,
                                "",
                                scroll
                            )

                        }
                    }, { error: Throwable ->
                        // 실패시 문구
                        OnVibratorSound(false)
                        startBarcode(
                            getString(R.string.error_barcode_save),
                            isActivityFinish,
                            "",
                            scroll
                        )
                        error.printStackTrace()
                    })
            )
        }else{
            compositeDisposable.add(
                InterfaceApi.saveUnload(this, jsonArray)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: BaseResponseModel ->

                        if (response.status == "200") {
                            //성공
                            when {
                                response.data == "SUCCESS" -> {
                                    //  성공
                                    if (isActivityFinish) {

                                        startBarcode(
                                            getString(R.string.unload_complete),
                                            isActivityFinish,
                                            "",
                                            scroll
                                        )
                                    } else {
                                        OnVibratorSound(true)
                                        startBarcode("", isActivityFinish, barcode, scroll)
                                    }
                                }
                                response.data == "FAIL" -> {
                                    // 실패 (저장 실패)
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data == "FAIL_NODATA" -> {
                                    //실패 (운송장번호가 DB에 없을시):
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_no_barcode_number),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data.contains("FAIL") -> {
                                    //실패 (운송장번호가 다른사람일경우) :
                                    val spritData = response.data.split("_")
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    val A: String = spritData[1]
                                    val B: String = spritData[2]
                                    OnVibratorSound(false)
                                    startBarcode(
                                        "배송할 상품이 아닙니다.\n본 상품은 $A 권역의\n $B 기사님에게 할당되었습니다.",
                                        isActivityFinish
                                        , ""
                                        , scroll
                                    )
                                }
                                else -> {
                                    //그밖
                                    scanLists = JSONArray()
                                    scanLists = backupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_barcode_save),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )

                                }
                            }

                        } else {
                            OnVibratorSound(false)
                            //실패
                            startBarcode(
                                getString(R.string.error_barcode_save),
                                isActivityFinish,
                                "",
                                scroll
                            )

                        }
//                        if (isActivityFinish) {
//                            finish()
//                        } else {
//                            startBarcode("", isActivityFinish)
//                        }

                    }, { error: Throwable ->
                        // 실패시 문구
                        OnVibratorSound(false)
                        startBarcode(
                            getString(R.string.error_barcode_save),
                            isActivityFinish,
                            "",
                            scroll
                        )
                        error.printStackTrace()
                    })
            )
        }
    }

    //
    //    /**
    //     * 테스트를 위한코드로 사용안함
    //     */
    //    void Test() {
    //        findViewById(R.id.scanTest).setOnClickListener(new View.OnClickListener() {
    //            @Override
    //            public void onClick(View view) {
    //                Intent intent = new Intent();
    //                intent.putExtra(KeyInfo.KEY_SCAN_TEST, "some value");
    //                setResult(RESULT_OK, intent);
    //                finish();
    //            }
    //        });
    //    }
    fun showOneButtonDialog(message: String?, callBack: DialogCallBack) {
        val dialog = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_alert, null)
        dialog.setView(view)
        val alertOneDialog = dialog.create()
        alertOneDialog.setCancelable(false)
        val messageTextView = view.findViewById<TextView>(R.id.message)
        messageTextView.text = message
        view.findViewById<View>(R.id.btn_ok)
            .setOnClickListener { click: View? ->
                callBack.onOkButtonClicked()
                alertOneDialog.dismiss()
            }
        alertOneDialog.show()
    }
}

