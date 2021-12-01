package com.logisbelley.mobileapp.TransScan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.zxing.integration.android.IntentIntegrator
import com.logisbelley.mobileapp.*
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isBarCodeCheck
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.transBarcodeList
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.transbackupScanLists
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_custom2_dialog.view.*
import org.json.JSONArray
import org.json.JSONObject

class TransScannerActivity : AppCompatActivity() {
    private var compositeDisposable = CompositeDisposable()
    // 원복을 위한 백업 데이터
    var turnOverbackupScanLists = JSONArray()

    // 실제 스캔된 데이터
    var turnOverscanLists = JSONArray()

    var transRsnCd = "990"
    var unloadRsncd = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trans_scanner)
        val param = intent.extras!!.get(KeyInfo.KEY_PARAM) as String

        geTurnOverList(param)




    }
    override fun onDestroy() {
        super.onDestroy()
        Log.e("onDestroy","onDestroy")
        // 화면 종료될땐 off 초기화
        LogisbelleyApplication.isFlashOnMode = false
        transBarcodeList.clear()
        if (compositeDisposable != null) {
            if(compositeDisposable.isDisposed) {
                compositeDisposable.dispose()
            }
        }

    }


    fun geTurnOverList(param: String) {
        val activity: Activity = this
        compositeDisposable = CompositeDisposable()

        compositeDisposable.add(
            InterfaceApi.getTransList(this, param)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe({ response: GetTransList ->
                    settingData(response.data)
                }, { error: Throwable ->
                    error.printStackTrace()
                })
        )
    }

    /**
     * 전송받은 데이터 셋팅
     */
    private fun settingData(list: ArrayList<GetTransList.Data>) {
        transbackupScanLists = JSONArray()
        for (i in 0 until list.size) {
            val jsonObject = JSONObject()
            val data = list[i]
            jsonObject.put(KeyInfo.KEY_CENTER_CD, data.CENTER_CD)
            jsonObject.put(KeyInfo.KEY_DELI_DY, data.DELI_DY)
            jsonObject.put(KeyInfo.KEY_DELI_SEQ, data.DELI_SEQ)
            jsonObject.put(KeyInfo.KEY_ROUTE_ID, data.ROUTE_ID)
            jsonObject.put(KeyInfo.KEY_STOP_SEQ, data.STOP_SEQ)
            jsonObject.put(KeyInfo.KEY_OMS_WAYBILL_NO, data.OMS_WAYBILL_NO)
            jsonObject.put(KeyInfo.KEY_OMS_PACKING_NO, data.OMS_PACKING_NO)
            jsonObject.put(KeyInfo.KEY_OMS_EX_NO, data.OMS_EX_NO)
            jsonObject.put(KeyInfo.KEY_UNLOAD_SCAN_YN, data.UNLOAD_SCAN_YN)
            jsonObject.put(KeyInfo.KEY_STOP_NM, data.STOP_NM)
            jsonObject.put(KeyInfo.KEY_UNLOAD_RSN_CD, data.UNLOAD_RSN_CD)
            jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN,data.UNLOAD_SCAN_YN)
            jsonObject.put(KeyInfo.KEY_TRANS_ROUTE_ID, MySharedPreferences(this).transRoutId)
            jsonObject.put(KeyInfo.KEY_DRV_NO, MySharedPreferences(this).drvNo)
            jsonObject.put(KeyInfo.KEY_TRANS_RSN_CD, transRsnCd)
            jsonObject.put(KeyInfo.KEY_STOP_CD,data.STOP_CD)
            jsonObject.put(KeyInfo.KEY_STOP_SEQ_VW,data.STOP_SEQ_VW)

            turnOverscanLists.put(jsonObject)
            transbackupScanLists.put(jsonObject)

        }
        startBarcode("", false, "", "0","")
    }


    /**
     * 바코드 화면 실행
     */
    //barcode: 방금 스캔한 아이템
    fun startBarcode(message: String, isFinish: Boolean, barcode: String, scroll: String, stopSeq: String) {
        val integrator = IntentIntegrator(this)
        integrator.captureActivity = TurnOverScannerActivity::class.java
        integrator.setBeepEnabled(false)
        integrator.addExtra(KeyInfo.KEY_SCAN_LIST, transbackupScanLists)
        Log.e("isFinish",isFinish.toString())
        if (isFinish) {
            integrator.addExtra(KeyInfo.KEY_FINISH_YN, "true")
        }
        integrator.addExtra(KeyInfo.KEY_MESSAGE, message)

        if (barcode != "") {
            integrator.addExtra(KeyInfo.KEY_BARCODE, barcode)
        }
        if (scroll != "") {
            integrator.addExtra(KeyInfo.KEY_SCROLL_POS, scroll)
        }
        if (stopSeq != ""){
            integrator.addExtra(KeyInfo.KEY_STOP_SEQ, stopSeq)
        }
        overridePendingTransition(0, 0)
        integrator.setBeepEnabled(true)

        integrator.initiateScan()

    }
    /**
     * 바코드 스캔시 처리
     */
    private fun scanBarcodeSetting(barcode: String) {
        val backpList = turnOverscanLists
        turnOverbackupScanLists = JSONArray()
        turnOverbackupScanLists = turnOverscanLists
        transBarcodeList.clear()
        val semdScanList = JSONArray()
        // 목록에 있는 리스트인지 체크
        var isCheck = false
        // 스캔 항목인지 체크 한 항목인지 체크
        var isScanYn = false
        var pos = 0
        turnOverscanLists = JSONArray()
        var stopSeq = ""
        var msg = ""
        var backupBarcode = JSONArray()
        for (i in 0 until backpList.length()) {
            try {
                val jsonObject = backpList.getJSONObject(i)
                if (jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO) == barcode) {
                    stopSeq = jsonObject.getString(KeyInfo.KEY_STOP_SEQ)
                    msg = jsonObject.getString(KeyInfo.KEY_STOP_NM)
                 }


                if (stopSeq == backpList.getJSONObject(i).getString(KeyInfo.KEY_STOP_SEQ)){
                    backupBarcode = transbackupScanLists
                    if (backupBarcode.getJSONObject(i).getString(KeyInfo.KEY_OMS_WAYBILL_NO) == barcode &&
                        backupBarcode.getJSONObject(i).getString(KeyInfo.KEY_UNLOAD_SCAN_YN) == "O"){
                        isScanYn = true
                        unloadRsncd = backupBarcode.getJSONObject(i).getString(KeyInfo.KEY_UNLOAD_RSN_CD)
                    }
                    semdScanList.put(jsonObject)
                    pos = i
                    isCheck = true
                }
                turnOverscanLists.put(jsonObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        transBarcodeList.clear()
        for (i in 0 until backpList.length()) {
            try{
                val jsonObject = backpList.getJSONObject(i)
                if (stopSeq == jsonObject!!.getString(KeyInfo.KEY_STOP_SEQ)){
                    Log.e("몇번 타니??",i.toString())
                    transBarcodeList.add(jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO))
                }
            }catch (e:Exception){

            }
        }
        Log.e("transBarcodeList",transBarcodeList.toString())
        // 운송장번호가 있는경우
        if (isCheck) {
            //스캔항항목
            if (isScanYn) {
                //동일 한바코드가 있을때
                isBarCodeCheck = true
                OnVibratorSound(false)
                var message = "스캔을 취소하시겠습니까?\n" + transBarcodeList
                if (unloadRsncd == "990"){
                    transBarcodeList.clear()
                    unloadRsncd = ""
                    message = "인계 완료된 운송장 입니다."
                    startBarcode(message, false, "", "0","")
                }else{
                    transBarcodeList.clear()

                    startBarcode(message, false, "", "0",stopSeq)
                }

            } else {
                isBarCodeCheck = false
                val message = "스캔한" + msg + "에게 배송할 운송장 번호가 아래와 같습니다. \n 일괄 인계 처리 하시겠습니까?\n" + transBarcodeList
                transBarcodeList.clear()
                startBarcode(message, false, barcode, "0", stopSeq)


            }

        }else{
            startBarcode(getString(R.string.error_barcode), false, "", "0","")
        }
    }

    private fun saveList(jsonArray: JSONArray, isActivityFinish: Boolean, barcode: String, scroll: String) {

        compositeDisposable = CompositeDisposable()
        Log.e("SNSAVELIST json",jsonArray.toString())
        compositeDisposable.add(
            InterfaceApi.saveTrans(this, jsonArray)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe({ response: BaseResponseModel ->
                    Log.e("SNSAVELIST", response.toString())
                    Log.e("SNSAVELIST", response.status)
                    Log.e("SNSAVELIST", response.data)
                    if (response.status == "200") {
                        //성공
                        when {
                            response.data == "SUCCESS" -> {
                                //  성공
                                if (isActivityFinish) {
                                    startBarcode(getString(R.string.trans_load_complete), isActivityFinish, "", scroll, "")
                                } else {
                                    OnVibratorSound(true)
                                    startBarcode("", isActivityFinish, barcode, scroll, "")
                                }

                            }
                            response.data == "FAIL" -> {
                                // 실패 (저장 실패)
                                ScannerActivity.scanLists = JSONArray()
                                ScannerActivity.scanLists = ScannerActivity.backupScanLists
                                OnVibratorSound(false)
                                startBarcode(
                                    getString(R.string.error_barcode_save),
                                    isActivityFinish,
                                    "",
                                    scroll,
                                    ""
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
                            scroll,
                            ""
                        )

                    }

                }, { error: Throwable ->
                    // 실패시 문구
                    OnVibratorSound(false)
                    startBarcode(
                        getString(R.string.error_barcode_save),
                        isActivityFinish,
                        "",
                        scroll,
                        ""
                    )
                    error.printStackTrace()
                })
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        Log.e("onActvityResult","onActvityResult")
        if (result != null) {
            // 완료 버튼시
            if (data != null && data.getStringExtra(KeyInfo.KEY_SCAN_COMPLETED) != null) {
                LogisbelleyApplication.recyclerViewState = null
                val sendScanList = JSONArray()
                for (i in 0 until transbackupScanLists.length()) {
                    val jsonObject = transbackupScanLists.getJSONObject(i)
                    try
                    {
                        if (jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN) == "1"){
                            sendScanList.put(jsonObject)
                        }

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
            }else {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
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
}

