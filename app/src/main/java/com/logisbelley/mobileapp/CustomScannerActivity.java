package com.logisbelley.mobileapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * 바코드 화면 UI 커스텀 actvitiy
 */

public class CustomScannerActivity extends Activity {
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private ImageButton setting_btn, switchFlashlightButton;
    private JSONArray jsonArray;
    private RecyclerView recyclerView;
    private boolean isFinish = false;
    private TextView completebuttonText;
    private String curBarcode = "";
    private int curPosition = 0;
    ScanListAdapter scanListAdapter;
    static AlertDialog alertDialog;
    JSONObject jsonObject;
    String barcode;
    static AlertDialog alertOneDialog;
    private void showSelectDialog(int position) {
        final CharSequence[] oItems = LogisbelleyApplication.Companion.getScanTypeName().toArray(new CharSequence[LogisbelleyApplication.Companion.getScanTypeName().size()]);

        AlertDialog.Builder oDialog = new AlertDialog.Builder(this, R.style.AlertDialog);

        oDialog.setTitle("비고를 선택하세요")
                .setItems(oItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String formatBarcode = "";
                        Log.e("which",which+"");
                        if (which != 0)
                        {
                            try {
                                jsonObject = jsonArray.getJSONObject(position);
                                barcode = jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO);
                                formatBarcode = formatWaybill(barcode); // alert창에 보여주는 용도로만 사용
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            String s = null;
                            switch (which){
                                case 0:
                                    s = "\"선택\"";
                                    break;
                                case 1:
                                    s = "\"배송처 영업종료\"";
                                    break;
                                case 2:
                                    s = "\"화물사고 (분실 / 파손)\"";
                                    break;
                                case 3:
                                    s = "\"수취 거부(배달전 취소)\"";
                                    break;
                                case 4:
                                    s = "\"주소지 변경\"";
                                    break;
                                case 5:
                                    s = "\"고객 주소/연락처 오류\"";
                                    break;
                                case 6:
                                    s = "\"출입불가\"";
                                    break;
                                case 7:
                                    s = "\"배송중 취소\"";
                                    break;
                                case 8:
                                    s = "\"택배사 사유\"";
                                    break;
                                case 9:
                                    s = "\"지정일 배송건\"";
                                    break;
                                case 10:
                                    s = "\"천재지변 (배송)\"";
                                    break;
                                case 11:
                                    s = "\"배송점 분류 오류\"";
                                    break;
                                case 12:
                                    s = "\"고객사 출고 오류\"";
                                    break;
                                case 13:
                                    s = "\"배송기사 회수 지연\"";
                                    break;
                                default:
                                    break;
                            }
                            // 비고선택이 있는경우 알럿을 띄어서 보여줌
                            showTwoButtonTwoTextViewDialog("운송장번호: " + formatBarcode,"위 운송장의" + s + "을(를) 처리 하시겠습니까?", new DialogCallBack() {
                                @Override
                                public void onOkButtonClicked() {
                                    dialog.dismiss();
                                    alertDialog.dismiss();
                                    Intent intent = new Intent();
                                    intent.putExtra(KeyInfo.KEY_ETC_SELECT, "some value");
                                    intent.putExtra(KeyInfo.KEY_POSITION, String.valueOf(position));
                                    intent.putExtra(KeyInfo.KEY_SCROLL_POS, "0");
                                    intent.putExtra(KeyInfo.KEY_OMS_WAYBILL_NO, barcode);
                                    LogisbelleyApplication.Companion.setRecyclerViewState(recyclerView.getLayoutManager().onSaveInstanceState());
                                    intent.putExtra(KeyInfo.KEY_CODE, LogisbelleyApplication.Companion.getScanTypeCode().get(which));
                                    setResult(RESULT_OK, intent);

                                    finish();

                                }

                                @Override
                                public void onCancelButtonClicked() {
                                }
                            });

                        }else{
                            try {
                                jsonObject = jsonArray.getJSONObject(position);
                                barcode = jsonObject.getString(KeyInfo.KEY_OMS_WAYBILL_NO);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Intent intent = new Intent();
                            intent.putExtra(KeyInfo.KEY_ETC_SELECT, "some value");
                            intent.putExtra(KeyInfo.KEY_POSITION, String.valueOf(position));
                            intent.putExtra(KeyInfo.KEY_SCROLL_POS, "0");
                            intent.putExtra(KeyInfo.KEY_OMS_WAYBILL_NO, barcode);
                            LogisbelleyApplication.Companion.setRecyclerViewState(recyclerView.getLayoutManager().onSaveInstanceState());
                            intent.putExtra(KeyInfo.KEY_CODE, LogisbelleyApplication.Companion.getScanTypeCode().get(which));
                            setResult(RESULT_OK, intent);

                            finish();

                        }
                    }
                })
                .show();
    }

    // 운송장 번호를 xxxx-xxxx-xxxx or xxxx-xxxx-xx 형식으로 포멧하기 위한 메소드_20210825_ukheyonPark
    private String formatWaybill(String waybill){
        char[] array = waybill.toCharArray();
        String formatedWaybill = "";
        for(int i = 0; i<array.length; i++){
            if(i%4==0 && i!=0){ //앞에다 -
                formatedWaybill += "-" + array[i];
            } else {
                formatedWaybill += array[i];
            }
        }
        return formatedWaybill;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);
        String arrayList = getIntent().getStringExtra(KeyInfo.KEY_SCAN_LIST);

        try {
            jsonArray = new JSONArray(arrayList);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //finishYn : true이면 다이얼로그 실행후 종료를 위함
        if (getIntent().getStringExtra(KeyInfo.KEY_FINISH_YN) != null) {
            isFinish = true;
        }

        //curBarcode : 현재 찍은 바코드
        if (getIntent().getStringExtra(KeyInfo.KEY_BARCODE) != null) {
            curBarcode = getIntent().getStringExtra(KeyInfo.KEY_BARCODE);
        }
        //curBarcode : 현재 찍은 바코드
        if (getIntent().getStringExtra(KeyInfo.KEY_SCROLL_POS) != null) {
            String scrollStr = getIntent().getStringExtra(KeyInfo.KEY_SCROLL_POS);
            if (!"".equals(scrollStr)) {
                curPosition = Integer.parseInt(scrollStr);
            }
        }

        /**
         * 스캔률을 표시해주기 위한 코드
         */
        int scanCnt = 0;
        int allCnt = jsonArray.length();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                String scanYN = jsonArray.getJSONObject(i).getString(KeyInfo.KEY_LOAD_SCAN_YN);
                if ("O".equals(scanYN)||"1".equals(scanYN)) {
                    scanCnt++;
                }
            } catch (Exception e) {

            }

        }
        double rate = (double)((double)scanCnt/(double)allCnt)*100;
//        String disPattern = "0.##";
//        DecimalFormat decimalFormat = new DecimalFormat(disPattern);

        // 스캔된 갯수
        TextView scanCntTextView = findViewById(R.id.scanCntTextView);
        scanCntTextView.setText(Integer.toString(scanCnt));

        // 전체 스캔 갯수
        TextView allCntTextView = findViewById(R.id.allCntTextView);
        allCntTextView.setText(Integer.toString(jsonArray.length()));

        // 스캔률 퍼센트
        TextView percentTextView = findViewById(R.id.percentTextView);

        int per = (int) Math.round(rate);
        percentTextView.setText(Integer.toString(per));

        // 상&하차 같은 화면을 사용하기에 하차시 텍스트 변경
        completebuttonText = findViewById(R.id.completeButton);
        if (getIntent().getStringExtra(KeyInfo.KEY_TYPE) != null) {
            completebuttonText.setText("하차 완료");
        }

        // isFinish : true 일때 화면 바로 종료
        if (getIntent().getStringExtra(KeyInfo.KEY_MESSAGE) != null) {
            String message = getIntent().getStringExtra(KeyInfo.KEY_MESSAGE);
            if(!"".equals(message)) {
                showOneButtonDialog(message, new DialogCallBack() {
                    @Override
                    public void onOkButtonClicked() {
                        if (isFinish) {
                            alertOneDialog.dismiss();
                            scanListAdapter.notifyDataSetChanged();

                            finish();
                        }
                    }
                    @Override
                    public void onCancelButtonClicked() {

                    }
                });
            }else {
                if (isFinish) {
                    finish();
                }
            }
        }

        findViewById(R.id.onClickClosed).setOnClickListener(v -> {
            ScannerActivity.isBigoCheck = true;
            finish();
        });


        setting_btn = (ImageButton) findViewById(R.id.setting_btn);
        switchFlashlightButton = (ImageButton) findViewById(R.id.switch_flashlight);

        if (!hasFlash()) {
            switchFlashlightButton.setVisibility(View.GONE);
        }

        findViewById(R.id.plashOn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchFlashlight(true);
                LogisbelleyApplication.Companion.setFlashON(false);
            }
        });

        findViewById(R.id.plashOff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogisbelleyApplication.Companion.setFlashON(true);
                switchFlashlight(false);
            }
        });



        // 상차완료,하차완료 버튼 선택시
        LinearLayout bottomLayout = (LinearLayout) findViewById(R.id.bottomLayout);
        bottomLayout.setOnClickListener(v -> {
            try {
                String need_scan_message = "상차 스캔을 하지 않은 상품이 있습니다.\n상차 스캔을 완료하시기 바랍니다.";
                if (completebuttonText.getText().toString().contains("하차")) {
                    need_scan_message = "하차 스캔을 하지 않은 상품이 있습니다.\n하차 스캔을 완료하시기 바랍니다.";
                }

                String cancel_ord_need_scan_message = "취소 상품이 스캔되지 않았습니다.\n스캔을 완료하시기 바랍니다.";
                String cancel_ord_not_load_message = "취소 상품은 상차하실 수 없습니다.\n상차취소로 선택하시기 바랍니다.";
                String cancel_ord_not_unload_message = "취소 상품은 하차하실 수 없습니다.\n배송중취소로 선택하시기 바랍니다.";

                // 미 스캔 항목 체크
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    /**
                     * 상차완료 시 처리 조건
                     * 1. 모두 스캔 또는 미배송처리
                     * 2. 취소 주문은 반드시 미배송 처리 되어야 한다.
                     * 3. 취소 주문은 반드시 스캔 되어야 한다.
                     */
                    if ( jsonObject.has(KeyInfo.KEY_LOAD_RSN_CD) ) {
                        if(
                                ("정상".equals(jsonObject.getString(KeyInfo.KEY_ODR_STS_NM)))
                                        && "0".equals(jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD))   /*   정상 배송   */
                                        && (("X".equals(jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN))) || ("0".equals(jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN))))  /*    스캔 안된 것 */
                        ) {
                            showOneButtonDialog(need_scan_message, new DialogCallBack() {
                                @Override
                                public void onOkButtonClicked() {
                                }

                                @Override
                                public void onCancelButtonClicked() {
                                }
                            });
                            return;
                        } else if(
                                ("취소".equals(jsonObject.getString(KeyInfo.KEY_ODR_STS_NM))) /* 주문 취소 */
                                        && (("X".equals(jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN))) || ("0".equals(jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN))))  /*    스캔 안된 것 */
                        ) {
                            showOneButtonDialog(cancel_ord_need_scan_message, new DialogCallBack() {
                                @Override
                                public void onOkButtonClicked() {
                                }

                                @Override
                                public void onCancelButtonClicked() {
                                }
                            });
                            return;

                        } else if(
                                ("취소".equals(jsonObject.getString(KeyInfo.KEY_ODR_STS_NM))) /* 주문 취소 */
                                        && "0".equals(jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD)) /* 정상배송 */
                        ){
                            showOneButtonDialog(cancel_ord_not_load_message, new DialogCallBack() {
                                @Override
                                public void onOkButtonClicked() {}
                                @Override
                                public void onCancelButtonClicked() {}
                            });
                            return;
                        }

                        /**
                         * 하차완료 시 처리 조건
                         * 1. 정상배송 스캔 확인
                         * 2. 취소주문의 미배송 처리.
                         */
                    } else if(
                            jsonObject.has(KeyInfo.KEY_UNLOAD_RSN_CD)
//                        && jsonObject.getString(KeyInfo.KEY_UNLOAD_RSN_CD).equals("0")
                    ){
                        Log.e("MSG", "KeyInfo.KEY_UNLOAD_RSN_CD : "+jsonObject.getString(KeyInfo.KEY_UNLOAD_RSN_CD) );
                        if(
                                ("정상".equals(jsonObject.getString(KeyInfo.KEY_ODR_STS_NM)))
                                        && ((jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN).equals("X")) || (jsonObject.getString(KeyInfo.KEY_LOAD_SCAN_YN).equals("0")))
                        ){
                            showOneButtonDialog(need_scan_message, new DialogCallBack() {
                                @Override
                                public void onOkButtonClicked() {
                                }

                                @Override
                                public void onCancelButtonClicked() {
                                }
                            });
                            return;
                        } else if(
                                ("취소".equals(jsonObject.getString(KeyInfo.KEY_ODR_STS_NM))) /* 주문 취소 */
                                        && jsonObject.getString(KeyInfo.KEY_UNLOAD_RSN_CD).equals("0")
                        ) {
                            showOneButtonDialog(cancel_ord_not_unload_message, new DialogCallBack() {
                                @Override
                                public void onOkButtonClicked() {}
                                @Override
                                public void onCancelButtonClicked() {}
                            });
                            return;
                        }
                    }
                }
                boolean isAllScanMode = true;
                //상차인경우에만 체크
                if (completebuttonText.getText().toString().contains("상차")) {
                    //isAllScanMode: 비고선택 아이템이 있는지 체크

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        // 비고 선택시 무조건 알럿을 띄어준다
                        if (!jsonObject.getString(KeyInfo.KEY_LOAD_RSN_CD).equals("0")) {
                            isAllScanMode = false;
                        }
                    }
                }

                // 스캔으로만 이뤄질 경우 바로 완료 처리
                if (isAllScanMode) {
                    ScannerActivity.isBigoCheck = true;
                    Intent intent = new Intent();
                    intent.putExtra(KeyInfo.KEY_SCAN_COMPLETED, "some value");
//                    intent.putExtra(KeyInfo.KEY_SCROLL_POS, Integer.toString(recyclerView.getScrollState()));
                    LogisbelleyApplication.Companion.setRecyclerViewState(recyclerView.getLayoutManager().onSaveInstanceState());
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    // 비고선택이 있는경우 알럿을 띄어서 보여줌
                    showTwoButtonDialog("미상차로 처리한 상품이 있습니다.이대로 진행하시겠습니까?\n\n해당상품은 상차검수목록에서 삭제되며 상차하실 수 없습니다.", new DialogCallBack() {
                        @Override
                        public void onOkButtonClicked() {
                            Intent intent = new Intent();
                            intent.putExtra(KeyInfo.KEY_SCAN_COMPLETED, "some value");
                            intent.putExtra(KeyInfo.KEY_SCROLL_POS, Integer.toString(recyclerView.getScrollState()));
                            LogisbelleyApplication.Companion.setRecyclerViewState(recyclerView.getLayoutManager().onSaveInstanceState());
                            setResult(RESULT_OK, intent);
                            finish();
                        }

                        @Override
                        public void onCancelButtonClicked() {
                        }
                    });
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        scanListAdapter = new ScanListAdapter(this, curBarcode, jsonArray, new ScanListAdapter.SelectEtcClick() {
            @Override
            public void onClickSelectEct(int position) {
                showSelectDialog(position);
            }
        });


        recyclerView.setAdapter(scanListAdapter);

        if (LogisbelleyApplication.Companion.getRecyclerViewState() != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(LogisbelleyApplication.Companion.getRecyclerViewState());
        } else {
            recyclerView.scrollToPosition(curPosition);
        }
        barcodeScannerView = (DecoratedBarcodeView) findViewById(R.id.zxing_barcode_scanner);
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        switchFlashlight(!LogisbelleyApplication.Companion.isFlashOnMode());
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

    /**
     * 알럿
     */
    public void showOneButtonDialog(String message, DialogCallBack callBack) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_alert, null);
        dialog.setView(view);

        alertOneDialog = dialog.create();
        alertOneDialog.setCancelable(false);
        TextView messageTextView = view.findViewById(R.id.message);
        messageTextView.setText(message);
        view.findViewById(R.id.btn_ok).setOnClickListener(click -> {
            callBack.onOkButtonClicked();
            alertOneDialog.dismiss();
        });
        alertOneDialog.show();
    }

    /**
     * 알럿
     */
    public void showTwoButtonDialog(String message, DialogCallBack callBack) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_dialog, null);
        dialog.setView(view);
        AlertDialog alertDialog = dialog.create();
        alertDialog.setCancelable(false);
        TextView messageTextView = view.findViewById(R.id.tv_message);
        messageTextView.setText(message);
        view.findViewById(R.id.btn_ok).setOnClickListener(click -> {
            callBack.onOkButtonClicked();
            alertDialog.dismiss();
        });
        view.findViewById(R.id.btn_cancel).setOnClickListener(click -> {
            callBack.onCancelButtonClicked();
            alertDialog.dismiss();
        });
        alertDialog.show();
    }

    public void showTwoButtonTwoTextViewDialog(String message, String submessage, DialogCallBack callBack) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_custom2_dialog, null);
        dialog.setView(view);

        alertDialog = dialog.create();
        alertDialog.setCancelable(false);
        TextView messageTextView = view.findViewById(R.id.tv_message);
        TextView messageSubTextView = view.findViewById(R.id.tv_sub_message);
        messageTextView.setText(message);
        messageSubTextView.setText(submessage);
        view.findViewById(R.id.btn_ok).setOnClickListener(click -> {
            callBack.onOkButtonClicked();
            alertDialog.dismiss();
        });
        view.findViewById(R.id.btn_cancel).setOnClickListener(click -> {
            callBack.onCancelButtonClicked();
            alertDialog.dismiss();
        });
        alertDialog.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
        if (alertDialog != null && alertDialog.isShowing()){
            alertDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
        if (alertDialog != null && alertDialog.isShowing()){
            alertDialog.dismiss();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ScannerActivity.isBigoCheck = true;
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    /**
     * 손전등 키고 끄기
     * @param isOnOff
     */
    public void switchFlashlight(boolean isOnOff) {
        if (isOnOff) {
            barcodeScannerView.setTorchOff();
            findViewById(R.id.plashOff).setVisibility(View.VISIBLE);
            findViewById(R.id.plashOn).setVisibility(View.GONE);
        } else {
            barcodeScannerView.setTorchOn();
            findViewById(R.id.plashOff).setVisibility(View.GONE);
            findViewById(R.id.plashOn).setVisibility(View.VISIBLE);

        }
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
}