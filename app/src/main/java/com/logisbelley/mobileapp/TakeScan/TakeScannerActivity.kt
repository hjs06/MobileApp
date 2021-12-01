package com.logisbelley.mobileapp.TakeScan

import android.app.Activity
import android.app.AlertDialog
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
import com.google.zxing.integration.android.IntentIntegrator
import com.logisbelley.mobileapp.*
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.stopSeq
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONArray
import org.json.JSONObject

class TakeScannerActivity: AppCompatActivity() {
    private var isScanLoad = true
    private var compositeDisposable = CompositeDisposable()
    private var bigo = false

    companion object {
        @kotlin.jvm.JvmField
        var isTakeBigoCheck: Boolean = false

        // 실제 스캔된 데이터
        var takeScanLists = JSONArray()

        // 원복을 위한 백업 데이터
        var takeBackupScanLists = JSONArray()
        var takeLastList = JSONArray()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_scanner)

        val param = intent.extras!!.get(KeyInfo.KEY_PARAM) as String
        val rsnCode = intent.extras!!.get(KeyInfo.KEY_RSN_CODE) as String
        val drvno = intent.extras!!.get(KeyInfo.KEY_TAKE_DRV_NO) as String
        getLoadList(param, drvno)
    }

    /**
     * 바코드 화면 실행
     */
    //barcode: 방금 스캔한 아이템
    fun startBarcode(message: String, isFinish: Boolean, barcode: String, scroll: String) {

        val integrator = IntentIntegrator(this)
        integrator.captureActivity = TakeCustomScannerActivity::class.java
        integrator.setBeepEnabled(false)
        integrator.addExtra(KeyInfo.KEY_SCAN_LIST, takeScanLists)
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
        overridePendingTransition(0, 0)
        integrator.setBeepEnabled(true)

        integrator.initiateScan()

    }


    override fun onDestroy() {
        super.onDestroy()
        stopSeq = ""
        // 화면 종료될땐 off 초기화
        LogisbelleyApplication.isFlashOnMode = false

        if (compositeDisposable != null) {
            if(compositeDisposable.isDisposed) {
                compositeDisposable.dispose()
            }
        }
    }


    fun getLoadList(param: String, drvno : String) {
        val activity: Activity = this
        compositeDisposable = CompositeDisposable()

        if (isScanLoad) {
            compositeDisposable.add(
                InterfaceApi.getTakeList(this, param)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: GetTakeList ->
                        settingData(response.data,drvno)
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
     * 전송받은 데이터 셋팅
     */
    private fun settingData(list: ArrayList<GetTakeList.Data>, drvno: String) {

        takeScanLists = JSONArray()
        for (i in 0 until list.size) {
            val jsonObject = JSONObject()
            val data = list[i]

            jsonObject.put(KeyInfo.KEY_CENTER_CD, data.CENTER_CD)
            jsonObject.put(KeyInfo.KEY_DELI_DY, data.DELI_DY)
            jsonObject.put(KeyInfo.KEY_DELI_SEQ, data.DELI_SEQ)
            jsonObject.put(KeyInfo.KEY_ROUTE_ID, data.ROUTE_ID)
            jsonObject.put(KeyInfo.KEY_STOP_SEQ, data.STOP_SEQ)
            jsonObject.put(KeyInfo.KEY_OMS_EX_NO, data.OMS_EX_NO)
            if (data.DELI_NO != null) {
                if (data.DELI_NO != "") {
                    jsonObject.put(KeyInfo.KEY_DELI_NO, data.DELI_NO)
                }
            }
            jsonObject.put(KeyInfo.KEY_OMS_WAYBILL_NO, data.OMS_WAYBILL_NO)
            jsonObject.put(KeyInfo.KEY_OMS_PACKING_NO, data.OMS_PACKING_NO)
            jsonObject.put(KeyInfo.KEY_OMS_EX_NO,data.OMS_EX_NO)
            jsonObject.put(KeyInfo.KEY_STOP_NM,data.STOP_NM)
            if (isScanLoad) {
                jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, data.LOAD_SCAN_YN)
                jsonObject.put(KeyInfo.KEY_LOAD_RSN_CD, data.LOAD_RSN_CD)
                jsonObject.put(KeyInfo.KEY_TAKE_RSN_CD, data.LOAD_RSN_CD)

            }
            jsonObject.put(KeyInfo.KEY_ODR_STS_NM, data.ODR_STS_NM)
            jsonObject.put(KeyInfo.KEY_ROUTE_ID_ORG,data.ROUTE_ID_ORG)
            jsonObject.put(KeyInfo.KEY_STOP_SEQ_ORG,data.STOP_SEQ_ORG)
            jsonObject.put(KeyInfo.KEY_DRV_NO,drvno)
            jsonObject.put(KeyInfo.KEY_STOP_SEQ_VW,data.STOP_SEQ_VW)
            takeScanLists.put(jsonObject)
        }
        startBarcode("", false, "", "0")
    }


    /**
     * 바코드 스캔시 처리
     */
    private fun scanBarcodeSetting(barcode: String) {
        val backpList = takeScanLists
        takeBackupScanLists = JSONArray()
        takeBackupScanLists = takeScanLists

        val semdScanList = JSONArray()
        // 목록에 있는 리스트인지 체크
        var isCheck = false
        // 스캔 항목인지 체크 한 항목인지 체크
        var isScanYn = false

        var pos = 0

        takeScanLists = JSONArray()

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
                    takeScanLists.put(jsonObject)
                    semdScanList.put(jsonObject)
                    takeLastList.put(jsonObject)
                    pos = i
                    isCheck = true
                } else {
                    takeScanLists.put(jsonObject)
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
                startBarcode("",false,barcode,pos.toString())
            }

        } else {
            startBarcode(getString(R.string.error_barcode), false, "", "0")
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            // 완료 버튼시
            if (data != null && data.getStringExtra(KeyInfo.KEY_SCAN_COMPLETED) != null) {
                LogisbelleyApplication.recyclerViewState = null
                val sendScanList = JSONArray()
                for (i in 0 until takeScanLists.length()) {
                    val jsonObject = takeScanLists.getJSONObject(i)
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

                        if (jsonObject.get(KeyInfo.KEY_LOAD_RSN_CD) == "4"){
                            jsonObject.put(KeyInfo.KEY_TAKE_RSN_CD,"4")
                        }else{
                            jsonObject.put(KeyInfo.KEY_TAKE_RSN_CD,"0")
                        }

                        sendScanList.put(jsonObject)


                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                saveList(sendScanList, true, "", "")
                return
            }

            // 비고 선택시
            if (data != null && data.getStringExtra(KeyInfo.KEY_ETC_SELECT) != null) {
                bigo = true
                val barCstr = data.getStringExtra(KeyInfo.KEY_OMS_WAYBILL_NO)

                startBarcode("", false, barCstr, "0")

                return
            }

            // 바코드 스캔시 넘어오는 바코드
            if (result.contents != null) {
                LogisbelleyApplication.recyclerViewState = null
                val barcodeNumber = result.contents
                scanBarcodeSetting(barcodeNumber)
            }else if (isTakeBigoCheck){
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
        scroll: String
    ) {

        val activity: Activity = this

        compositeDisposable = CompositeDisposable()
        takeLastList = JSONArray()
        if (isScanLoad) {
            compositeDisposable.add(
                InterfaceApi.saveTake(this, jsonArray)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe({ response: BaseResponseModel ->

                        if (response.status == "200") {
                            //성공
                            when {
                                response.data == "SUCCESS" -> {
                                    startBarcode("인수 처리가 완료되었습니다.", isActivityFinish, barcode, scroll)
                                }
                                response.data == "FAIL" -> {
                                    // 실패 (저장 실패)
                                    takeScanLists = JSONArray()
                                    takeScanLists = takeBackupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_tak_barcode_save),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                response.data == "FAIL_CHK" -> {
                                    //실패 (운송장번호가 DB에 없을시):
                                    takeScanLists = JSONArray()
                                    takeScanLists = takeBackupScanLists
                                    OnVibratorSound(false)
                                    startBarcode(
                                        getString(R.string.error_faile_chk),
                                        isActivityFinish,
                                        "",
                                        scroll
                                    )
                                }
                                else -> {
                                    //그밖
                                    takeScanLists = JSONArray()
                                    takeScanLists = takeBackupScanLists
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
        }
    }


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

