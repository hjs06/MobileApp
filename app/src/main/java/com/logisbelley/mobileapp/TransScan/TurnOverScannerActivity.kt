package com.logisbelley.mobileapp.TransScan

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.logisbelley.mobileapp.*
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isBarCodeCheck
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isFlashOnMode
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isbarCodeAlert
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.recyclerViewState
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.setFlashON
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.transbackupScanLists
import com.logisbelley.mobileapp.TakeScan.TakeScannerActivity
import kotlinx.android.synthetic.main.activity_turn_over_scanner.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class TurnOverScannerActivity : AppCompatActivity() {
    private var capture: CaptureManager? = null
    private var barcodeScannerView: DecoratedBarcodeView? = null
    private var setting_btn: ImageButton? = null
    private var switchFlashlightButton: ImageButton? = null
    private var jsonArray: JSONArray? = null
    private var recyclerView: RecyclerView? = null
    private var isFinish = false
    private var completebuttonText: TextView? = null
    private var curBarcode = ""
    private var curPosition = 0
    var scanListAdapter: TransScanListAdapter? = null
    var alertDialog: AlertDialog? = null
    var jsonObject: JSONObject? = null
    var barcode: String? = null
    var alertOneDialog: AlertDialog? = null
    var stopseq = ""
    var isCompleted = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turn_over_scanner)

        barcodeScannerView =
            findViewById<View>(R.id.trunover_zxing_barcode_scanner) as DecoratedBarcodeView
        capture = CaptureManager(this, barcodeScannerView)
        capture!!.initializeFromIntent(intent, savedInstanceState)
        capture!!.decode()

        val arrayList = intent.getStringExtra(KeyInfo.KEY_SCAN_LIST)
        try {
            jsonArray = JSONArray(arrayList)
            transbackupScanLists = jsonArray as JSONArray
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        //finishYn : true이면 다이얼로그 실행후 종료를 위함

        //finishYn : true이면 다이얼로그 실행후 종료를 위함
        if (intent.getStringExtra(KeyInfo.KEY_FINISH_YN) != null) {
            isFinish = true
        }

        //curBarcode : 현재 찍은 바코드
        if (intent.getStringExtra(KeyInfo.KEY_BARCODE) != null) {
            curBarcode = intent.getStringExtra(KeyInfo.KEY_BARCODE)
        }
        if (intent.getStringExtra(KeyInfo.KEY_SCROLL_POS) != null) {
            val scrollStr = intent.getStringExtra(KeyInfo.KEY_SCROLL_POS)
            if ("" != scrollStr) {
                curPosition = scrollStr.toInt()
            }
        }
        if (intent.getStringExtra(KeyInfo.KEY_STOP_SEQ) != null) {
            stopseq = intent.getStringExtra(KeyInfo.KEY_STOP_SEQ)
        }


        /**
         * 스캔률을 표시해주기 위한 코드
         */
        var scanCnt = 0
        val allCnt: Int = jsonArray!!.length()
        for (i in 0 until jsonArray!!.length()) {
            try {
                val scanYN: String =
                    jsonArray!!.getJSONObject(i).getString(KeyInfo.KEY_LOAD_SCAN_YN)
                if ("O" == scanYN || "1" == scanYN) {
                    scanCnt++
                }
            } catch (e: Exception) {
            }
        }
        val rate = (scanCnt.toDouble() / allCnt.toDouble()) * 100

        // 스캔된 갯수
        val scanCntTextView = findViewById<TextView>(R.id.turnoverScanCntTextView)
        scanCntTextView.text = Integer.toString(scanCnt)

        // 전체 스캔 갯수
        val allCntTextView = findViewById<TextView>(R.id.turnoverAllCntTextView)
        allCntTextView.text = Integer.toString(jsonArray!!.length())

        // 스캔률 퍼센트
        val percentTextView = findViewById<TextView>(R.id.turnoverPercentTextView)

        val per = Math.round(rate).toInt()
        percentTextView.text = Integer.toString(per)


        // isFinish : true 일때 화면 바로 종료
        if (intent.getStringExtra(KeyInfo.KEY_MESSAGE) != null) {

            var message = intent.getStringExtra(KeyInfo.KEY_MESSAGE)
            if ("" != message) {
               if (stopseq == ""){
                    showOneButtonDialog(message, object : DialogCallBack {
                        override fun onOkButtonClicked() {
                            if (message.equals("인계를 완료 합니다.")){
                                finish()
                            }

                        }
                        override fun onCancelButtonClicked() {}
                    })
                }else{
                    showTwoButtonDialog(message, object : DialogCallBack {
                        override fun onOkButtonClicked() {
                            if (isFinish) {
                                Log.e("isfinish",isFinish.toString())
                                scanListAdapter!!.notifyDataSetChanged()
                                finish()
                            } else {
                                Log.e("isfinish!!",isFinish.toString())
                                if (isBarCodeCheck) {
                                    Log.e("isBarCodeCheck!!",isBarCodeCheck.toString())
                                    for (i in 0 until jsonArray!!.length()) {
                                        val jsonObject = jsonArray!!.getJSONObject(i)
                                        try {
                                            if (stopseq == jsonObject.getString(KeyInfo.KEY_STOP_SEQ)) {
                                                if (jsonObject.getString(KeyInfo.KEY_UNLOAD_SCAN_YN) == "O") {
                                                    jsonObject.put(KeyInfo.KEY_UNLOAD_SCAN_YN, "X")
                                                    jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "0")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    isbarCodeAlert = false
                                } else {
                                    Log.e("isBarCodeCheck!!",isBarCodeCheck.toString())
                                    for (i in 0 until jsonArray!!.length()) {
                                        val jsonObject = jsonArray!!.getJSONObject(i)
                                        try {
                                            if (stopseq == jsonObject.getString(KeyInfo.KEY_STOP_SEQ)) {
                                                if (jsonObject.getString(KeyInfo.KEY_UNLOAD_SCAN_YN) == "X") {
                                                    jsonObject.put(KeyInfo.KEY_UNLOAD_SCAN_YN, "O")
                                                    jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "1")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    isbarCodeAlert = true
                                }
                                scanListAdapter!!.notifyDataSetChanged()
                            }
                        }

                        override fun onCancelButtonClicked() {
                            isbarCodeAlert = false
                            scanListAdapter!!.notifyDataSetChanged()
                        }
                    })
                }

            } else {
                if (isFinish) {
                    finish()
                }
            }
        }


//        setting_btn = findViewById<View>(R.id.setting_btn) as ImageButton
//        switchFlashlightButton = findViewById<View>(R.id.switch_flashlight) as ImageButton
//
        if (!hasFlash()) {
            switchFlashlightButton!!.setVisibility(View.GONE)
        }

        findViewById<View>(R.id.turnoverPlashOn).setOnClickListener {
            switchFlashlight(true)
            setFlashON(false)
        }

        findViewById<View>(R.id.turnoverPlashOff).setOnClickListener {
            setFlashON(true)
            switchFlashlight(false)
        }


        //확인 버튼
        val bottomLayout = findViewById<View>(R.id.turnoverBottomLayout) as LinearLayout
        bottomLayout.setOnClickListener { v: View? ->
            Log.e("transbackupScanLists",transbackupScanLists.toString())
            try {
                var success_Message =
                    "스캔한 상품을 " + "[" + MySharedPreferences(this).carNum + "]" + "\n 기사님께 인계 하시겠습니까?"
                var faile_Message = "인계 상품이 없습니다.\n확인후 다시 시도해주세요"
                Log.e("확인 버튼", "확인버튼")
                var succes = false
                // 미 스캔 항목 체크
                for (i in 0 until transbackupScanLists!!.length()) {
                    val jsonObject = transbackupScanLists!!.getJSONObject(i)
                    if (jsonObject.has(KeyInfo.KEY_LOAD_SCAN_YN)) {
                        if ("1" == jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN)) /*    스캔 안된 것 */ {
                            succes = true
                            break
                        }else{
                            succes = false
                        }
                    }
                }


                Log.e("succes",succes.toString())

                if (succes){
                    showTwoButtonDialog(success_Message, object : DialogCallBack {
                        override fun onOkButtonClicked() {
                            isCompleted = true
                            if (isCompleted) {
                                val intent = Intent()
                                intent.putExtra(KeyInfo.KEY_SCAN_COMPLETED, "some value")
                                recyclerViewState =
                                    recyclerView!!.layoutManager!!.onSaveInstanceState()
                                setResult(RESULT_OK, intent)
                                finish()
                            }
                        }

                        override fun onCancelButtonClicked() {}
                    })
                }else{
                    showOneButtonDialog(faile_Message, object : DialogCallBack {
                        override fun onOkButtonClicked() {}
                        override fun onCancelButtonClicked() {}
                    })
                }



            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        recyclerView = findViewById<View>(R.id.turnoverRecyclerView) as RecyclerView
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView!!.setLayoutManager(linearLayoutManager)
        scanListAdapter = TransScanListAdapter(this, curBarcode, jsonArray!!, stopseq)


        recyclerView!!.setAdapter(scanListAdapter)

        if (recyclerViewState != null) {
            recyclerView!!.getLayoutManager()?.onRestoreInstanceState(recyclerViewState)
        } else {
            recyclerView!!.scrollToPosition(curPosition)
        }


        switchFlashlight(!isFlashOnMode)


        turnoverOnClickClosed.setOnClickListener {
            Log.e("transBarcodeList", LogisbelleyApplication.transBarcodeList.toString())
            LogisbelleyApplication.transBarcodeList.clear()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        capture!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture!!.onPause()
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        capture!!.onDestroy()
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
    }

    /**
     * 알럿
     */
    open fun showTwoButtonDialog(message: String?, callBack: DialogCallBack) {
        val dialog = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_custom_dialog, null)
        dialog.setView(view)
        val alertDialog = dialog.create()
        alertDialog.setCancelable(false)
        val messageTextView = view.findViewById<TextView>(R.id.tv_message)
        messageTextView.text = message
        view.findViewById<View>(R.id.btn_ok).setOnClickListener { click: View? ->
            callBack.onOkButtonClicked()
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener { click: View? ->
            callBack.onCancelButtonClicked()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    /**
     * 알럿
     */
    fun showOneButtonDialog(message: String?, callBack: DialogCallBack) {
        val dialog = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_alert, null)
        dialog.setView(view)
        val alertOneDialog = dialog.create()
        alertOneDialog.setCancelable(false)
        val messageTextView = view.findViewById<TextView>(R.id.message)
        messageTextView.text = message
        view.findViewById<View>(R.id.btn_ok).setOnClickListener { click: View? ->
            callBack.onOkButtonClicked()
            alertOneDialog.dismiss()
        }
        alertOneDialog.show()
    }

    private fun hasFlash(): Boolean {
        return applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    /**
     * 손전등 키고 끄기
     * @param isOnOff
     */
    fun switchFlashlight(isOnOff: Boolean) {
        if (isOnOff) {
            barcodeScannerView!!.setTorchOff()
            findViewById<View>(R.id.turnoverPlashOff).visibility = View.VISIBLE
            findViewById<View>(R.id.turnoverPlashOn).visibility = View.GONE
        } else {
            barcodeScannerView!!.setTorchOn()
            findViewById<View>(R.id.turnoverPlashOff).visibility = View.GONE
            findViewById<View>(R.id.turnoverPlashOn).visibility = View.VISIBLE
        }
    }
}

