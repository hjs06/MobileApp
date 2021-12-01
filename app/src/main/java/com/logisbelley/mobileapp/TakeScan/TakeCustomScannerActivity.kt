package com.logisbelley.mobileapp.TakeScan

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
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isFlashOnMode
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isbarCodeAlert
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.recyclerViewState
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.scanTypeName
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.setFlashON
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.stopSeq
import com.logisbelley.mobileapp.TakeScan.TakeScannerActivity.Companion.isTakeBigoCheck
import com.logisbelley.mobileapp.TakeScan.TakeScannerActivity.Companion.takeScanLists
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class TakeCustomScannerActivity: AppCompatActivity() {

    private var capture: CaptureManager? = null
    private var barcodeScannerView: DecoratedBarcodeView? = null
    private var setting_btn: ImageButton? = null
    private var switchFlashlightButton:ImageButton? = null
    private var jsonArray: JSONArray? = null
    private var recyclerView: RecyclerView? = null
    private var isFinish = false
    private var curBarcode = ""
    private var curPosition = 0
    var scanListAdapter: TakeScanListAdapter? = null
    var alertDialog: AlertDialog? = null
    var jsonObject: JSONObject? = null
    var barcode: String? = null
    var alertOneDialog: AlertDialog? = null
    var takeStopSeq = ""
    var takeBarcodeList = arrayListOf<String>()
    private fun showSelectDialog(position: Int) {
        val oItems = scanTypeName.toTypedArray<CharSequence>()
        val oDialog = AlertDialog.Builder(this, R.style.AlertDialog)

        oDialog.setTitle("비고를 선택하세요")
            .setItems(oItems) { dialog, which ->
                var formatBarcode = ""

                var name = ""
                if (which != 0) {
                    try {
                        jsonObject = jsonArray!!.getJSONObject(position)

                        barcode = jsonObject!!.getString(KeyInfo.KEY_OMS_WAYBILL_NO)
                        name = jsonObject!!.getString(KeyInfo.KEY_STOP_NM)
                        takeStopSeq = jsonObject!!.getString(KeyInfo.KEY_STOP_SEQ)
                        stopSeq = takeStopSeq
                        for (i in 0 until jsonArray!!.length()) {
                            try {
                                val jsonObject = jsonArray!!.getJSONObject(i)
                                if (takeStopSeq == jsonObject!!.getString(KeyInfo.KEY_STOP_SEQ)){
                                    takeBarcodeList.add(jsonObject!!.getString(KeyInfo.KEY_OMS_WAYBILL_NO))
                                }
                            }catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    var s: String? = null
                    when (which) {
                        1 -> s = "\"인수거부\""
                        else -> {
                        }
                    }
                    Log.e("TakeCutom SSSSS", s)
                    Log.e("TakeCutom takeCansList", takeScanLists.toString())
                    Log.e("TakeCutom posi", position.toString())
                    val jsonObject = takeScanLists!!.getJSONObject(position)
                    curBarcode = jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO)
                    Log.e("curBarcode121212",curBarcode)
                    if (jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD) != "4"){
                        // 비고선택이 있는경우 알럿을 띄어서 보여줌
                        showTwoButtonTwoTextViewDialog(
                            name + "에게 인수거부한 운송장 번호가 아래와 같습니다. \n 일괄 인수거부 처리 하시겠습니까?",

                            "" + takeBarcodeList,
                            object : DialogCallBack {
                                override fun onOkButtonClicked() {
                                    takeBarcodeList.clear()
                                    for (i in 0 until takeScanLists!!.length()) {
                                        val jsonObject = takeScanLists!!.getJSONObject(i)
                                        try {

                                            if (takeStopSeq == jsonObject.getString(KeyInfo.KEY_STOP_SEQ)) {
                                                if (jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN) == "X" ||
                                                    "1" == jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN)
                                                ) {
                                                    jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "1")
                                                    jsonObject.put(KeyInfo.KEY_LOAD_RSN_CD, "4")
                                                } else if (jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN) == "O" ||
                                                    "0" == jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN)
                                                ) {
                                                    jsonObject.put(
                                                        KeyInfo.KEY_LOAD_SCAN_YN,
                                                        "1"
                                                    )
                                                    jsonObject.put(KeyInfo.KEY_LOAD_RSN_CD, "4")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    Log.e("SHSHSH take", takeScanLists.toString())
                                    recyclerViewState =
                                        recyclerView!!.layoutManager!!.onSaveInstanceState()
                                    isbarCodeAlert = true
                                    scanListAdapter!!.notifyDataSetChanged()
                                    val intent = Intent()
                                    intent.putExtra(KeyInfo.KEY_ETC_SELECT, "some value")
                                    intent.putExtra(KeyInfo.KEY_OMS_WAYBILL_NO,curBarcode)
                                    setResult(RESULT_OK, intent)


                                    finish()

                                }

                                override fun onCancelButtonClicked() {
                                    isbarCodeAlert = false
                                    takeBarcodeList.clear()
                                }
                            })
                    }else{

                    }
                }else {

                    try {
                        jsonObject = jsonArray!!.getJSONObject(position)
                        barcode = jsonObject!!.getString(KeyInfo.KEY_OMS_WAYBILL_NO)
                        name = jsonObject!!.getString(KeyInfo.KEY_STOP_NM)
                        Log.e("SHTakeCustome name", name.toString())
                        takeStopSeq = jsonObject!!.getString(KeyInfo.KEY_STOP_SEQ)
                        Log.e("SHTakeCustome takeStopSeq", takeStopSeq.toString())
                        stopSeq = takeStopSeq
                        formatBarcode = formatWaybill(barcode) // alert창에 보여주는 용도로만 사용
                        for (i in 0 until jsonArray!!.length()) {
                            try {
                                val jsonObject = jsonArray!!.getJSONObject(i)
                                if (takeStopSeq == jsonObject!!.getString(KeyInfo.KEY_STOP_SEQ)){
                                    takeBarcodeList.add(jsonObject!!.getString(KeyInfo.KEY_OMS_WAYBILL_NO))
                                }else{

                                }
                            }catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    val jsonObject = takeScanLists!!.getJSONObject(position)
                    if (jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD) != "0"){
                        showTwoButtonTwoTextViewDialog(
                            "인수거부 취소한 운송장 번호가\n아래와 같습니다. \n 일괄 취소 처리 하시겠습니까?",
                            "" + takeBarcodeList,
                            object : DialogCallBack {
                                override fun onOkButtonClicked() {
                                    takeBarcodeList.clear()
                                    for (i in 0 until takeScanLists!!.length()) {
                                        val jsonObject = takeScanLists!!.getJSONObject(i)
                                        try {
                                            if (takeStopSeq == jsonObject.getString(KeyInfo.KEY_STOP_SEQ)) {
                                                if (jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN) == "1") {
                                                    jsonObject.put(KeyInfo.KEY_LOAD_SCAN_YN, "X")
                                                    jsonObject.put(KeyInfo.KEY_LOAD_RSN_CD, "0")
                                                }

                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    isbarCodeAlert = false

                                    scanListAdapter!!.notifyDataSetChanged()
                                    val intent = Intent()
                                    intent.putExtra(KeyInfo.KEY_ETC_SELECT, "some value")
                                    intent.putExtra(KeyInfo.KEY_OMS_WAYBILL_NO,curBarcode)
                                    setResult(RESULT_OK, intent)


                                    finish()
                                }

                                override fun onCancelButtonClicked() {
                                    takeBarcodeList.clear()
                                }
                            })
                    }else{
                        Log.e("?????1", "?????1")
                        takeBarcodeList.clear()
                    }

                }

            }
            .show()
    }

    // 운송장 번호를 xxxx-xxxx-xxxx or xxxx-xxxx-xx 형식으로 포멧하기 위한 메소드_20210825_ukheyonPark
    private fun formatWaybill(waybill: String?): String {
        val array = waybill!!.toCharArray()
        var formatedWaybill = ""
        for (i in array.indices) {
            if (i % 4 == 0 && i != 0) { //앞에다 -
                formatedWaybill += "-" + array[i]
            } else {
                formatedWaybill += array[i]
            }
        }
        return formatedWaybill
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_custom_scanner)

        val arrayList = intent.getStringExtra(KeyInfo.KEY_SCAN_LIST)

        try {
            jsonArray = JSONArray(arrayList)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

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
                Log.e("curPosition", curPosition.toString() + "LLLLL")
                curPosition = scrollStr.toInt()
            }
        }


        /**
         * 스캔률을 표시해주기 위한 코드
         */
        Log.e("SHSH", jsonArray.toString())
        var scanCnt = 0
        val allCnt = jsonArray!!.length()
        for (i in 0 until jsonArray!!.length()) {
            try {
                val scanYN = jsonArray!!.getJSONObject(i).getString(KeyInfo.KEY_LOAD_SCAN_YN)
                if ("O" == scanYN || "1" == scanYN) {
                    scanCnt++
                }
            } catch (e: Exception) {
            }
        }
        val rate = (scanCnt.toDouble() / allCnt.toDouble()) * 100
//        String disPattern = "0.##";
//        DecimalFormat decimalFormat = new DecimalFormat(disPattern);

        // 스캔된 갯수
        //        String disPattern = "0.##";
//        DecimalFormat decimalFormat = new DecimalFormat(disPattern);

        // 스캔된 갯수
        val scanCntTextView = findViewById<TextView>(R.id.takescanCntTextView)
        scanCntTextView.text = Integer.toString(scanCnt)

        // 전체 스캔 갯수

        // 전체 스캔 갯수
        val allCntTextView = findViewById<TextView>(R.id.takeallCntTextView)
        allCntTextView.text = Integer.toString(jsonArray!!.length())

        // 스캔률 퍼센트
        val percentTextView = findViewById<TextView>(R.id.takepercentTextView)

        val per = Math.round(rate).toInt()
        percentTextView.text = Integer.toString(per)

        // isFinish : true 일때 화면 바로 종료
        if (intent.getStringExtra(KeyInfo.KEY_MESSAGE) != null) {
            val message = intent.getStringExtra(KeyInfo.KEY_MESSAGE)
            if ("" != message) {
                showOneButtonDialog(message, object : DialogCallBack {
                    override fun onOkButtonClicked() {
                        if (isFinish) {
                            scanListAdapter!!.notifyDataSetChanged()
                            finish()
                        }
                    }

                    override fun onCancelButtonClicked() {}
                })
            }
            else if (intent.getStringExtra(KeyInfo.KEY_BARCODE) != null){
                Log.e("SHSHSH2","SH2SHSH")
            }
            else {
                Log.e("SHSHSH","SHSHSH")
                if (isFinish) {
                    finish()
                }
            }
        }

        findViewById<View>(R.id.takeonClickClosed).setOnClickListener { v: View? ->
            isTakeBigoCheck = true
            finish()
        }


//        setting_btn = findViewById<View>(R.id.setting_btn) as ImageButton
//        switchFlashlightButton = findViewById<View>(R.id.switch_flashlight) as ImageButton

        if (!hasFlash()) {
            switchFlashlightButton!!.setVisibility(View.GONE)
        }

        findViewById<View>(R.id.takeplashOn).setOnClickListener {
            switchFlashlight(true)
            setFlashON(false)
        }

        findViewById<View>(R.id.takeplashOff).setOnClickListener {
            setFlashON(true)
            switchFlashlight(false)
        }


        // 상차완료,하차완료 버튼 선택시
        val bottomLayout = findViewById<View>(R.id.takebottomLayout) as LinearLayout
        bottomLayout.setOnClickListener { v: View? ->
            try {
                val need_scan_message = "인수 스캔을 하지 않은 상품이 있습니다.\n인수 스캔을 완료하시기 바랍니다."

                // 미 스캔 항목 체크
                for (i in 0 until takeScanLists!!.length()) {
                    val jsonObject = takeScanLists!!.getJSONObject(i)
                    if (jsonObject.has(KeyInfo.KEY_LOAD_RSN_CD)) {
                        if ("정상" == jsonObject.getString(KeyInfo.KEY_ODR_STS_NM)
                            && "0" == jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD) && ("X" == jsonObject.getString(
                                KeyInfo.KEY_LOAD_SCAN_YN
                            ) || "0" == jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN)) /*    스캔 안된 것 */) {
                            showOneButtonDialog(need_scan_message, object : DialogCallBack {
                                override fun onOkButtonClicked() {}
                                override fun onCancelButtonClicked() {}
                            })
                            return@setOnClickListener
                        }
                    }
                }


                var isAllScanMode = true
                // 스캔으로만 이뤄질 경우 바로 완료 처리
                if (isAllScanMode) {
                    isTakeBigoCheck = true
                    val intent = Intent()
                    intent.putExtra(
                        KeyInfo.KEY_SCAN_COMPLETED,
                        "some value"
                    )
                    //                    intent.putExtra(KeyInfo.KEY_SCROLL_POS, Integer.toString(recyclerView.getScrollState()));
                    recyclerViewState =
                        recyclerView!!.layoutManager!!.onSaveInstanceState()
                    setResult(RESULT_OK, intent)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        recyclerView = findViewById<View>(R.id.takerecyclerView) as RecyclerView
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView!!.setLayoutManager(linearLayoutManager)
        scanListAdapter = TakeScanListAdapter(this, curBarcode, takeScanLists!!, takeStopSeq,
            object :
                TakeScanListAdapter.SelectEtcClick {
                override fun onClickSelectEct(position: Int) {
                    showSelectDialog(position)
                }
            })


        recyclerView!!.setAdapter(scanListAdapter)

        if (recyclerViewState != null) {
            recyclerView!!.getLayoutManager()?.onRestoreInstanceState(recyclerViewState)
        } else {
            recyclerView!!.scrollToPosition(curPosition)
        }
        barcodeScannerView = findViewById<View>(R.id.takezxing_barcode_scanner) as DecoratedBarcodeView
        capture = CaptureManager(this, barcodeScannerView)
        capture!!.initializeFromIntent(intent, savedInstanceState)
        capture!!.decode()

        switchFlashlight(!isFlashOnMode)
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

    /**
     * 알럿
     */
    fun showTwoButtonDialog(message: String?, callBack: DialogCallBack) {
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

    fun showTwoButtonTwoTextViewDialog(
        message: String?,
        submessage: String?,
        callBack: DialogCallBack
    ) {
        val dialog = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_custom2_dialog, null)
        dialog.setView(view)
        val alertDialog = dialog.create()
        alertDialog.setCancelable(false)
        val messageTextView = view.findViewById<TextView>(R.id.tv_message)
        val messageSubTextView = view.findViewById<TextView>(R.id.tv_sub_message)
        messageTextView.text = message
        messageSubTextView.text = submessage
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

    override fun onBackPressed() {
        super.onBackPressed()
        isTakeBigoCheck = true
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture!!.onSaveInstanceState(outState)
    }


    /**
     * 손전등 키고 끄기
     * @param isOnOff
     */
    fun switchFlashlight(isOnOff: Boolean) {
        if (isOnOff) {
            barcodeScannerView!!.setTorchOff()
            findViewById<View>(R.id.takeplashOff).visibility = View.VISIBLE
            findViewById<View>(R.id.takeplashOn).visibility = View.GONE
        } else {
            barcodeScannerView!!.setTorchOn()
            findViewById<View>(R.id.takeplashOff).visibility = View.GONE
            findViewById<View>(R.id.takeplashOn).visibility = View.VISIBLE
        }
    }

    private fun hasFlash(): Boolean {
        return applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }
}