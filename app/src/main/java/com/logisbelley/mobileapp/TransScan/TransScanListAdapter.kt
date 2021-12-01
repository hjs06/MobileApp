package com.logisbelley.mobileapp.TransScan

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.logisbelley.mobileapp.KeyInfo
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.isbarCodeAlert
import com.logisbelley.mobileapp.R
import kotlinx.android.synthetic.main.row_scan.view.*
import kotlinx.android.synthetic.main.row_transscan.view.*
import org.json.JSONArray


class TransScanListAdapter(
    private var context: Activity,
    private val curBarcode: String,
    private var scanInfoList: JSONArray,
    private val stopseq: String
) : RecyclerView.Adapter<TransScanListAdapter.TransScanListViewHolder>() {
    // 스캔 리스트 ViewHolder
    inner class TransScanListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemCount(): Int {
        return scanInfoList.length()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransScanListViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.row_transscan, parent, false)
        return TransScanListViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransScanListViewHolder, position: Int) {
        try {

            val jsonObject = scanInfoList.getJSONObject(position)



            holder.itemView.transscanNumber.text = jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO)
            var scanYN: String = jsonObject.getString(KeyInfo.KEY_UNLOAD_SCAN_YN)
            var stopNm: String = jsonObject.getString(KeyInfo.KEY_STOP_NM)

            // scanYN : O => 스캔
            // scanYn : X => 스캔안됨
            if (scanYN == "X") {
                scanYN = "-"
            }

            holder.itemView.transstatus.text = stopNm
            holder.itemView.transscanYn.text = scanYN
            holder.itemView.transorderNumber.text = jsonObject.getString(KeyInfo.KEY_STOP_SEQ_VW)
            holder.itemView.transscanNumber.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.transorderNumber.setTextColor(Color.parseColor("#231f20"))
            holder.itemView.transbackgroundlayout.setBackgroundColor(Color.parseColor("#ffffff"))



            // scanYN 이 "O"이고 ectName이 선택이 아니면 스캔한 항목으로 본다
            if (scanYN == "O") {
                holder.itemView.transscanNumber.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.transorderNumber.setTextColor(Color.parseColor("#50231f20"))
                holder.itemView.transbackgroundlayout.setBackgroundColor(Color.parseColor("#f2f2f2"))

            }else {
                holder.itemView.transscanNumber.setTextColor(Color.parseColor("#231f20"))
                holder.itemView.transorderNumber.setTextColor(Color.parseColor("#231f20"))
                holder.itemView.transbackgroundlayout.setBackgroundColor(Color.parseColor("#ffffff"))
            }

            // groupLine : 바코드 스캔시 빨간테두리 적용
            holder.itemView.transgroupLine.visibility = View.GONE
            // curBarcode : 현재 스캔한 바코드


            if (stopseq == jsonObject.getString(KeyInfo.KEY_STOP_SEQ) && isbarCodeAlert) {
                holder.itemView.transgroupLine.visibility = View.VISIBLE
                holder.itemView.transbackgroundlayout.setBackgroundColor(Color.parseColor("#f2f8ff"))
            }

        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

}