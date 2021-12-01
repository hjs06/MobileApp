package com.logisbelley.mobileapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.recyclerViewState
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.scanTypeCode
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.scanTypeName
import com.logisbelley.mobileapp.ScannerActivity.Companion.isScanLoad
import kotlinx.android.synthetic.main.row_scan.view.*
import org.json.JSONArray

/**
 * 상&하차 리스트 목록  Adapter
 * curBarcode : 현재 스캔한 바코드
 * scanInfoList : 상하차 정보 리스트
 * listener : 비고 클릭 리스너
 */
class ScanListAdapter(
    private var context: Activity,
    private val curBarcode: String,
    private var scanInfoList: JSONArray,
    private val listener: SelectEtcClick
) :
    RecyclerView.Adapter<ScanListAdapter.ScanListViewHolder>() {

    // 비고 클릭 리스너
    interface SelectEtcClick {
        fun onClickSelectEct(position: Int)
    }

    // 스캔 리스트 ViewHolder
    inner class ScanListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemCount(): Int {
        return scanInfoList.length()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanListViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.row_scan, parent, false)
        return ScanListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScanListViewHolder, position: Int) {
        try {
            val jsonObject = scanInfoList.getJSONObject(position)
            if (isScanLoad){
                holder.itemView.scanNumber.text = jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO) +"\n"+"("+ jsonObject.getString(KeyInfo.KEY_WCS_ID) +")"
            }else{
                holder.itemView.scanNumber.text = jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO)
            }

            var scanYN: String = jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN)
            // scanYN : 1 => 스캔
            // scanYn : 0 => 스캔안됨
            if (scanYN == "1") {
                scanYN = "O"
            }
            if (isScanLoad){
                holder.itemView.orderNumber.text = jsonObject.getString(KeyInfo.KEY_STOP_SEQ_VW)
            }else{
                holder.itemView.orderNumber.text = (position + 1).toString()
            }

            holder.itemView.scanNumber.setTextColor(Color.parseColor("#231f20"))
//            holder.itemView.scanYN.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.scanEtc.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.orderNumber.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.backgroundlayout.setBackgroundColor(Color.parseColor("#ffffff"))

            var ectName = "선택"
            //scanTypeCode : 서버에서 내려준 비고 목록 코드
            //scanTypeName : 서버에서 내려준 비고 목록 명칭
            for (i in 0 until scanTypeCode.size) {
                if (jsonObject.has(KeyInfo.KEY_LOAD_RSN_CD)) {
                    if (jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD) == scanTypeCode[i]) {
                        ectName = scanTypeName[i]
                    }
                } else {
                    if (jsonObject.getString(KeyInfo.KEY_UNLOAD_RSN_CD) == scanTypeCode[i]) {
                        ectName = scanTypeName[i]
                    }
                }
            }

            // 비고 선택시 이벤트
            holder.itemView.scanEtcLayout.setOnClickListener {
                listener.onClickSelectEct(position)
            }


            holder.itemView.scanEtc.text = ectName


            // scanYN 이 "O"이고 ectName이 선택이 아니면 스캔한 항목으로 본다
            if (scanYN == "O" || ectName != "선택") {
                holder.itemView.scanNumber.setTextColor(Color.parseColor("#50231f20"))
//                holder.itemView.scanYN.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.scanEtc.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.orderNumber.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.backgroundlayout.setBackgroundColor(Color.parseColor("#f2f2f2"))


            }
            // groupLine : 바코드 스캔시 빨간테두리 적용
            holder.itemView.groupLine.visibility = View.GONE

            // curBarcode : 현재 스캔한 바코드
            if (curBarcode == (jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO))) {
                holder.itemView.groupLine.visibility = View.VISIBLE
                holder.itemView.backgroundlayout.setBackgroundColor(Color.parseColor("#f2f8ff"))
            }

            val statusCode = jsonObject.getString(KeyInfo.KEY_ODR_STS_NM)

            /*  취소 주문의 경우 빨간색 강조 표시 */
            if( statusCode == "취소"){
                holder.itemView.scanNumber.setTextColor(Color.parseColor("#ff0000"))
                holder.itemView.status.setTextColor(Color.parseColor("#ff0000"))
//                holder.itemView.scanEtc.setTextColor(Color.parseColor("#ff0000"))
                holder.itemView.orderNumber.setTextColor(Color.parseColor("#ff0000"))
            }


            holder.itemView.status.text = statusCode

//            // 스캔 여부 O,X
            holder.itemView.scanYN.text = scanYN

        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

}

