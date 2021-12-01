package com.logisbelley.mobileapp.TakeScan

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.logisbelley.mobileapp.KeyInfo
import com.logisbelley.mobileapp.LogisbelleyApplication
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isbarCodeAlert
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.recyclerViewState
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.scanTypeCode
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.scanTypeName
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.stopSeq
import com.logisbelley.mobileapp.R
import com.logisbelley.mobileapp.ScanListAdapter
import kotlinx.android.synthetic.main.row_takescan.view.*
import org.json.JSONArray

/**
 * 상&하차 리스트 목록  Adapter
 * curBarcode : 현재 스캔한 바코드
 * scanInfoList : 상하차 정보 리스트
 * listener : 비고 클릭 리스너
 */
class TakeScanListAdapter(
    private var context: Activity,
    private val curBarcode: String,
    private var scanInfoList: JSONArray,
    private var stopseq: String,
    private val listener: SelectEtcClick
) :
    RecyclerView.Adapter<TakeScanListAdapter.TakeScanListViewHolder>() {

    // 비고 클릭 리스너
    interface SelectEtcClick {
        fun onClickSelectEct(position: Int)
    }

    // 스캔 리스트 ViewHolder
    inner class TakeScanListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemCount(): Int {
        return scanInfoList.length()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TakeScanListViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.row_takescan, parent, false)
        return TakeScanListViewHolder(view)
    }

    override fun onBindViewHolder(holder: TakeScanListViewHolder, position: Int) {
        try {
            val jsonObject = scanInfoList.getJSONObject(position)
            holder.itemView.takescanNumber.text = jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO)
            var scanYN: String = jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN)
            // scanYN : 1 => 스캔
            // scanYn : 0 => 스캔안됨
            if (scanYN == "1") {
                scanYN = "O"
            }

            holder.itemView.takeorderNumber.text = jsonObject.getString(KeyInfo.KEY_STOP_SEQ_VW)
            holder.itemView.takescanNumber.setTextColor(Color.parseColor("#231f20"))
//            holder.itemView.scanYN.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.takescanEtc.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.takeorderNumber.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.takebackgroundlayout.setBackgroundColor(Color.parseColor("#ffffff"))

            var ectName = "선택"
            //scanTypeCode : 서버에서 내려준 비고 목록 코드
            //scanTypeName : 서버에서 내려준 비고 목록 명칭
            for (i in 0 until scanTypeCode.size) {
                if (jsonObject.has(KeyInfo.KEY_LOAD_RSN_CD)) {
                    if (jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD) == scanTypeCode[i]) {
                        ectName = scanTypeName[i]
                    }
                }
            }

            // 비고 선택시 이벤트
            holder.itemView.takescanEtcLayout.setOnClickListener {
                listener.onClickSelectEct(position)
            }

            holder.itemView.takescanEtc.text = ectName

            // scanYN 이 "O"이고 ectName이 선택이 아니면 스캔한 항목으로 본다
            if (scanYN == "O" || ectName != "선택") {
                holder.itemView.takescanNumber.setTextColor(Color.parseColor("#50231f20"))
//                holder.itemView.scanYN.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.takescanEtc.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.takeorderNumber.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.takebackgroundlayout.setBackgroundColor(Color.parseColor("#f2f2f2"))

            }
            // groupLine : 바코드 스캔시 빨간테두리 적용
            holder.itemView.takegroupLine.visibility = View.GONE

            // curBarcode : 현재 스캔한 바코드
            Log.e("jsonPO",jsonObject.toString())
            Log.e("curBarcode",curBarcode.toString())
            Log.e("curBarcode22",jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO))
            if (curBarcode == (jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO)) ){
                holder.itemView.takegroupLine.visibility = View.VISIBLE
                holder.itemView.takebackgroundlayout.setBackgroundColor(Color.parseColor("#f2f8ff"))
            }else{
                holder.itemView.takegroupLine.visibility = View.GONE
            }

            val statusCode = jsonObject.getString(KeyInfo.KEY_STOP_NM)


            holder.itemView.takestatus.text = statusCode

//            // 스캔 여부 O,X
            holder.itemView.takescanYN.text = scanYN

        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

}

