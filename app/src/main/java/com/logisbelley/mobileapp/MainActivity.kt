package com.logisbelley.mobileapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.kakao.sdk.common.util.Utility.getKeyHash
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOptions
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.FLAG_CAMERRA
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.FLAG_GALLERY
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.MAIN_URL
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.recyclerViewState
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.scanTypeCode
import com.logisbelley.mobileapp.LogisbelleyApplication.Companion.scanTypeName
import com.logisbelley.mobileapp.TakeScan.TakeScannerActivity
import com.logisbelley.mobileapp.TransScan.TransScannerActivity
import com.logisbelley.mobileapp.databinding.ActivityMainBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import kotlin.system.exitProcess

/**
 * 메인화면
 */
class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {

    var isErrorPage = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 카카오 네비 HashKey발급 => 1회 발급 하여 사용 안함
        val kakaoNaviHashKey = getKeyHash(this)
        // 퍼미션 체크
        if (checkPermissions()) {
            if (isAppValidityCheckPass()) {
                webviewSetting(this)

                isGpsUsable()

                val stepCounterService = Intent(this, LocationService::class.java)
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(stepCounterService)
                } else {
                    startService(stepCounterService)
                }
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun webviewSetting(activity: Activity) {
        // 웹뷰내의 위치 정보 사용 여부
        webView.settings.setGeolocationEnabled(true)

        // 자바스크립트 사용 여부
        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        // 웹뷰내의 localStorage 사용 여부
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.setSupportMultipleWindows(true)

        // 뷰 셋팅
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true

        // 캐시 셋팅
        webView.settings.setAppCacheEnabled(true)
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.settings.setAppCachePath(cacheDir.path)

        // 이미지 셋팅
        webView.settings.blockNetworkImage = false
        webView.settings.loadsImagesAutomatically = true
        webView.settings.allowFileAccess = true

        // 캐시 삭제
        webView.clearCache(true)
        webView.clearHistory()

        // 쿠키관련 대응
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {

            override fun onReceivedError(
                view: WebView,
                req: WebResourceRequest,
                rerr: WebResourceError
            ) {
                // 알 수 없는 오류일때는 제외
                if (rerr.errorCode != -1) {
                    isErrorPage = true
                }

            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 쿠키 관련 대응 롤리팝 이하
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    //noinspection deprecation
                    CookieSyncManager.getInstance().sync();
                } else {
                    // 쿠키 관련 대응 롤리팝 이상
                    CookieManager.getInstance().flush();
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                showOneButtonDialog(
                    activity!!,
                    getString(R.string.alert), getString(R.string.gps_on), object : DialogCallBack {
                        override fun onCancelButtonClicked() {
                            callback?.invoke(origin, true, false)
                        }

                        override fun onOkButtonClicked() {
                            callback?.invoke(origin, false, false)
                        }
                    })

            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                val messages = message ?: getString(R.string.error_has_occurred)
                showOneButtonDialog2(
                    getString(R.string.alert),
                    messages,
                    object : DialogCallBack {
                        override fun onCancelButtonClicked() {
                        }

                        override fun onOkButtonClicked() {
                            result!!.confirm()
                        }
                    }
                )
                return true
            }

            override fun onJsConfirm(
                view: WebView,
                url: String,
                message: String,
                result: JsResult?
            ): Boolean {
                val messages = message ?: getString(R.string.error_has_occurred)
                showTwoButtonDialog2(
                    getString(R.string.alert),
                    messages,
                    object : DialogCallBack {
                        override fun onCancelButtonClicked() {
                            result!!.cancel()
                        }

                        override fun onOkButtonClicked() {
                            result!!.confirm()
                        }
                    }
                )
                return true
            }
        }

        webView.addJavascriptInterface(JavascriptInterfaces(webView, this), "android")

        // 빌드 버전
        val buildVersion = BuildConfig.VERSION_NAME
        val url = MAIN_URL

        webView.loadUrl("$url?v=$buildVersion")

    }


    /**
     * 자바 인터페이스 정리
     */
    class JavascriptInterfaces {
        var web: WebView? = null
        var activity: Activity? = null

        constructor (webView: WebView, act: Activity) {
            web = webView
            activity = act

        }

        /**
         * 2021.10.14 추가 수정 정성훈
         *
         */
        @JavascriptInterface
        fun runTrans(param: String){
            recyclerViewState = null
            val intent = Intent(activity!!, TransScannerActivity::class.java)
            intent.putExtra(KeyInfo.KEY_PARAM, param)
            val paramData = param.split("^")
            val deilSeq = paramData[2]
            val transRoutId = paramData[4]
            val driverN = paramData[5]
            val carN = paramData[6]
            val drvNo = paramData[7]
            MySharedPreferences(activity!!).dvName = driverN
            MySharedPreferences(activity!!).carNum = carN
            MySharedPreferences(activity!!).drvNo = drvNo
            MySharedPreferences(activity!!).transRoutId = transRoutId

            activity!!.startActivityForResult(intent, 555)
        }

        /**
         * 상차검수 화면 호출 함수
         * 해당 함수 호출시 상차검수 액티비티로 이동한다.
         * param : 파
         * rsnCode: 비고코드(파손,분실,상차취소)
         */
        @JavascriptInterface
        fun runLoad(param: String, rsnCode: String) {
            // recyclerView 스크롤 상태값 초기화
            recyclerViewState = null
            val intent = Intent(activity!!, ScannerActivity::class.java)
            intent.putExtra(KeyInfo.KEY_PARAM, param)
            val rsnCodeData = rsnCode.split("^")
            scanTypeCode = arrayListOf<String>()
            scanTypeName = arrayListOf<String>()

            // 선택 (code:1) 을 추가
            scanTypeCode.add("0")
            scanTypeName.add("선택")

            for (i in 0 until rsnCodeData.size) {
                val data = rsnCodeData[i]
                Log.e("KMY", "KMY]runLoad:data=$data")
                val spritData = data.split(",")
                Log.e("KMY", "KMY]runLoad:data=$spritData")
                scanTypeCode.add(spritData[0])
                scanTypeName.add(spritData[1])
            }
            intent.putExtra(KeyInfo.KEY_RSN_CODE, rsnCode)
            activity!!.startActivityForResult(intent, 444)
        }

        @JavascriptInterface
        fun runTake(param: String, rsnCode: String){
            recyclerViewState = null
            val intent = Intent(activity!!, TakeScannerActivity::class.java)
            intent.putExtra(KeyInfo.KEY_PARAM, param)
            val rsnCodeData = rsnCode.split("^")
            val paramData = param.split("^")
            val driverN = paramData[4]

            Log.e("SHSHdriverN",driverN.toString())
            scanTypeCode = arrayListOf<String>()
            scanTypeName = arrayListOf<String>()

            // 선택 (code:1) 을 추가
            scanTypeCode.add("0")
            scanTypeName.add("선택")
            for (i in 0 until rsnCodeData.size) {
                val data = rsnCodeData[i]
                val spritData = data.split(",")
                scanTypeCode.add(spritData[0])
                scanTypeName.add(spritData[1])
            }
            intent.putExtra(KeyInfo.KEY_RSN_CODE, rsnCode)
            intent.putExtra(KeyInfo.KEY_TAKE_DRV_NO,driverN)
            activity!!.startActivityForResult(intent, 444)
        }

        /**
         * 하차검수  화면 호출 함수
         * 해당 함수 호출시 상차검수 액티비티로 이동한다.
         * param : 파
         * rsnCode: 비고코드(파손,분실,상차취소)
         */
        @JavascriptInterface
        fun runUnload(param: String, rsnCode: String) {
            // recyclerView 스크롤 상태값 초기화
            recyclerViewState = null
            Log.e("KMY", "KMY]runLoad:param=$param,rsnCode=$rsnCode")
            val intent = Intent(activity!!, ScannerActivity::class.java)
            intent.putExtra(KeyInfo.KEY_PARAM, param)
            MySharedPreferences(activity!!).param = param
            val rsnCodeData = rsnCode.split("^")
            scanTypeCode = arrayListOf<String>()
            scanTypeName = arrayListOf<String>()

            // 선택 (code:1) 을 추가
            scanTypeCode.add("0")
            scanTypeName.add("선택")

            for (i in 0 until rsnCodeData.size) {
                val data = rsnCodeData[i]
                Log.e("KMY", "KMY]runLoad:data=$data")
                val spritData = data.split(",")
                Log.e("KMY", "KMY]runLoad:data=$spritData")
                scanTypeCode.add(spritData[0])
                scanTypeName.add(spritData[1])
            }
            intent.putExtra(KeyInfo.KEY_RSN_CODE, rsnCode)
            MySharedPreferences(activity!!).rsnCode = param
            intent.putExtra(KeyInfo.KEY_TYPE, "true")
            activity!!.startActivityForResult(intent, 444)
        }


        /**
         * 카메라 열기 함수
         * 해당 함수 호출시 카메라&갤러리 선택 팝업이 실행된다.
         */
        @JavascriptInterface
        fun runCamera() {
            //카메라 팝업 열기
            //mfRunCameraCallback=>사진정보
            imgPath = null
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, getFileUri())
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, getFileUri())
            }

            activity!!.startActivityForResult(
                cameraIntent,
                FLAG_CAMERRA
            )

        }

        /**
         * 현재 위치 가져오기 함수
         */
        @JavascriptInterface
        fun getCurrentGps() {

            //현재 위치 GPS
            //mfGetCurrentGpsCallback(latt,long)
            try {
                val longitude: String =
                    LogisbelleyApplication.curLocation!!.longitude.toString()
                val latitude: String =
                    LogisbelleyApplication.curLocation!!.latitude.toString()
                val url = "javascript:mfGetCurrentGpsCallback('$latitude','$longitude')"
                web!!.post(Runnable() {
                    web!!.loadUrl(url)
                })

            } catch (e: Exception) {

                val url = "javascript:mfGetCurrentGpsCallback(null,null)"

                web!!.post(Runnable() {
                    web!!.loadUrl(url)
                })
            }

        }

        /**
         * 데이터 저장 함수
         */
        @JavascriptInterface
        fun writeDataFile(data: String) {
            MySharedPreferences(activity!!).dataFile = data
        }


        @JavascriptInterface
        fun appDownload(url: String) {
            // 어플 다운로드
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity!!.startActivity(intent)
        }

        /**
         * 데이터 삭제 함수
         */
        @JavascriptInterface
        fun deleteDataFile() {
            MySharedPreferences(activity!!).dataFile = ""
        }

        /**
         * 저장된 데이터 전달
         */
        @JavascriptInterface
        fun returnDataFile() {
            val data = MySharedPreferences(activity!!).dataFile
            val url = "javascript:mfReturnDataFileCallback('$data')"
            web!!.post(Runnable() {
                web!!.loadUrl(url)
            })
        }

        /**
         * 카카오 네비 실행
         */
        @JavascriptInterface
        fun runKakaoNavi(longitude: String, latitude: String, title: String) {

            // 카카오 네비 설치 유무
            if (NaviClient.instance.isKakaoNaviInstalled(activity!!)) {
                activity!!.startActivity(
                    NaviClient.instance.navigateIntent(
                        Location(title, latitude.toDouble(), longitude.toDouble()),
                        NaviOptions(coordType = CoordType.WGS84)
                    )
                )

            } else {
                // 미설치시 설치페이지 이동
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.locnall.KimGiSa")
                )
                activity!!.startActivity(intent)
            }
        }


        /**
         * 바코드 열기 함수
         */
        @JavascriptInterface
        fun runBarcode() {
            // 페어링 목록 열기
//
//            //mfGetBarcode(바코드,YYYYMMDDHH24MISS)=> 스캔정보
//            mWebview!!.post(Runnable() {
//                if (BluetoothAdapter.getDefaultAdapter() == null) {
//                    //블루투스 지원안함
//                } else {
//                    //블루투스 연결 활성화 여부 확인
//                    if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
//                        // 블루투스 연결 확인
//                        if (true) {//연결여부 체크
//
//
////                            if (MySharedPreferences(activity!!).bluetoothAddress == "") {
////                                val intent = Intent(activity, InquiryBluetoothLE::class.java)
////                                activity!!.startActivityForResult(intent, 999)
////
////                            } else {
////                                connectBarcode(MySharedPreferences(mActivity!!).bluetoothAddress!!)
////                            }
//
//
//                        } else {
//                            Toast.makeText(
//                                activity,
//                                activity!!.getString(R.string.already_connected_scanner),
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    } else {
//                        var i = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                        activity!!.startActivityForResult(i, 777)
//                    }
//
//                }
//            })


        }


        /**
         * GPS 서버  시작하기 함수
         */
        @JavascriptInterface
        fun startSaveGps(param: String, gpsCycle: String) {
            //수집 시작

            MySharedPreferences(activity!!).gpsParam = param
            MySharedPreferences(activity!!).gpsCycle = gpsCycle
            val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
            val date = simpleDateFormat.format(LogisbelleyApplication.curLocation!!.time)

            var longitude = LogisbelleyApplication.curLocation!!.longitude.toString()
            var latitude = LogisbelleyApplication.curLocation!!.latitude.toString()

            var hashMap = HashMap<String, String>()
            hashMap[KeyInfo.KEY_PARAM] = param
            hashMap[KeyInfo.KEY_LATT] = latitude
            hashMap[KeyInfo.KEY_LNGT] = longitude
            hashMap[KeyInfo.KEY_GPS_DT] = date

            InterfaceApi.sendLocation(activity!!, hashMap)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe({ response: LocationResponseModel ->
                    Log.e("MainActivity", "kimitestsetset=====" + response.responseMsg)
                }, { error: Throwable ->
                    error.printStackTrace()
                })
            LogisbelleyApplication.curLocation = null
        }

        // GPS 수집하여 서버로 전송중인 경우에 해당 서버 전송을 바로 정지
        @JavascriptInterface
        fun stopSaveGps() {
            //수집 종료
            MySharedPreferences(activity!!).gpsParam = null
            MySharedPreferences(activity!!).gpsCycle = null
        }

        /**
         * gps 서버 종료
         * 한번 보내고 종료
         */
        @JavascriptInterface
        fun saveEndGps(param: String) {
            //수집 종료

            val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
            val date = simpleDateFormat.format(LogisbelleyApplication.curLocation!!.time)

            var longitude = LogisbelleyApplication.curLocation!!.longitude.toString()
            var latitude = LogisbelleyApplication.curLocation!!.latitude.toString()

            var hashMap = HashMap<String, String>()
            hashMap[KeyInfo.KEY_PARAM] = param
            hashMap[KeyInfo.KEY_LATT] = latitude
            hashMap[KeyInfo.KEY_LNGT] = longitude
            hashMap[KeyInfo.KEY_GPS_DT] = date

            InterfaceApi.sendLocation(activity!!, hashMap)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe({ response: LocationResponseModel ->

                }, { error: Throwable ->
                    error.printStackTrace()
                })
            LogisbelleyApplication.curLocation = null

            MySharedPreferences(activity!!).gpsParam = null
            MySharedPreferences(activity!!).gpsCycle = null
        }

        /**
         * 앱 종료 함수
         */
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @JavascriptInterface
        fun setOffApplication() {
            //앱 종료
            try {
                activity!!.stopService(Intent(activity!!, LocationService::class.java))
                activity!!.moveTaskToBack(true)
                activity!!.finishAndRemoveTask()
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e: Exception) {
            }
        }


        private fun getFileUri(): Uri? {
            val dir = File(activity!!.filesDir, "img")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file =
                File(dir, System.currentTimeMillis().toString() + ".png")
            imgPath = file.absolutePath
            val imgUris: Uri
            imgUris = FileProvider.getUriForFile(
                activity!!,
                activity!!.packageName.toString() + ".provider",
                file
            )
            return imgUris
        }


        /**
         * 티맵 실행
         */
        @JavascriptInterface
        fun runTmapNavi(latitude: String, longitude: String, title: String) {

            val packageName = "com.skt.tmap.ku"
            val tampIntent = activity!!.packageManager.getLaunchIntentForPackage(packageName)
            if (tampIntent == null) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("market://details?id=$packageName")
                    activity!!.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            } else {
                var intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("tmap://route?goalx=$longitude&goaly=$latitude&goalname=$title")
                )
                activity!!.startActivity(intent)
            }
        }


        /**
         * 전화연결 함수
         */
        @JavascriptInterface
        fun callDial(phone: String) {
            //앱 종료
            web!!.post(Runnable() {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                activity!!.startActivity(intent)
            })
        }


    }


    companion object {
        var imgPath: String? = null
        var mActivity: Activity? = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val IMAGE_SIZE = 1024
        // 배터리 최적화 예외처리
        if (requestCode == 99) {
            if (resultCode == Activity.RESULT_OK) {
                if (!isIgnoringBatteryOptimizations()) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.data = Uri.parse("package:" + packageName);
                    startActivityForResult(intent, 88);
                }
            } else {
                showOneButtonDialog2("알림", getString(R.string.no_gps2), object : DialogCallBack {
                    override fun onCancelButtonClicked() {
                        stopService(Intent(this@MainActivity, LocationService::class.java))
                        ActivityCompat.finishAffinity(this@MainActivity);
                        exitProcess(0)
                    }

                    override fun onOkButtonClicked() {
                        isGpsUsable()
                    }
                })
            }
        } else if (requestCode == 88) {
            // gps 정확도 향상을 위한 wifi 설정
            if (resultCode != Activity.RESULT_OK) {
                showOneButtonDialog2(
                    "알림",
                    getString(R.string.not_work_gps),
                    object : DialogCallBack {
                        override fun onCancelButtonClicked() {
                            stopService(Intent(this@MainActivity, LocationService::class.java))
                            ActivityCompat.finishAffinity(this@MainActivity);
                            exitProcess(0)
                        }

                        override fun onOkButtonClicked() {
                            if (!isIgnoringBatteryOptimizations()) {
                                val intent =
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                intent.data = Uri.parse("package:$packageName")
                                startActivityForResult(intent, 88)
                            }
                        }
                    })
            }
        } else if (requestCode == 444 || requestCode == 555) {
            // scan 화면 갱신
            val url = "javascript:cfReloadPage()"
            webView.loadUrl(url)
        } else if (requestCode == 777 && resultCode == Activity.RESULT_OK) {
//            if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
//                showOneButtonDialog(
//                    getString(R.string.alert),
//                    getString(R.string.bluetooth_off_error),
//                    true
//                )
//            }
        } else if (requestCode == 999 && resultCode == Activity.RESULT_OK) { //String logicalName = data.getStringExtra(InquiryBluetooth.EXTRA_DEVICE_NAME);
            // 블루투스 연결
//            val address = data!!.getStringExtra(InquiryBluetoothLE.EXTRA_DEVICE_ADDRESS)
//            MySharedPreferences(this).bluetoothAddress = address
//            connectBarcode(address)
        } else if (requestCode == FLAG_CAMERRA || requestCode == FLAG_GALLERY) {

            if (resultCode == RESULT_CANCELED) {
                return
            }
            if (data == null) {
                if (imgPath != null) { //카메라
                    var selPhoto: Bitmap? = null
                    val options = BitmapFactory.Options()
                    selPhoto = BitmapFactory.decodeFile(imgPath, options)
                    val width = selPhoto.width.toFloat()
                    val hight = selPhoto.height.toFloat()
                    val rotate: Int = getImageOrientation(imgPath)
                    val matrix = Matrix()
                    matrix.postRotate(rotate.toFloat())
                    var rotateBitmap = Bitmap.createBitmap(
                        selPhoto,
                        0,
                        0,
                        width.toInt(),
                        hight.toInt(),
                        matrix,
                        true
                    )
                    var realWidth: Int = IMAGE_SIZE
                    var realHeight: Int = IMAGE_SIZE
                    if (width > IMAGE_SIZE || hight > IMAGE_SIZE) {
                        val scaleSize: Double
                        if (rotateBitmap.getWidth() > rotateBitmap.getHeight()) {
                            scaleSize =
                                1024.0 / rotateBitmap.getWidth() * rotateBitmap.getHeight()
                            realHeight = Math.round(scaleSize).toString().toInt()
                        } else {
                            scaleSize =
                                1024.0 / rotateBitmap.getHeight() * rotateBitmap.getWidth()
                            realWidth = Math.round(scaleSize).toString().toInt()
                        }
                    } else {
                        realWidth = rotateBitmap.getWidth()
                        realHeight = rotateBitmap.getHeight()
                    }
                    rotateBitmap =
                        Bitmap.createScaledBitmap(rotateBitmap, realWidth, realHeight, true)
                    val byteArray = ByteArrayOutputStream()
                    rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArray)
                    val by = byteArray.toByteArray()
                    val encoding =
                        Base64.encodeToString(by, Base64.DEFAULT)
                    sendImage(encoding)
                    //                    selectImageView.setBackgroundResource(R.drawable.imageview_border);
                } else {
                    val galleryIntent = Intent(Intent.ACTION_PICK)
                    galleryIntent.type = MediaStore.Images.Media.CONTENT_TYPE
                    galleryIntent.data =
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI // images on the SD card.
                    startActivityForResult(
                        galleryIntent,
                        LogisbelleyApplication.FLAG_GALLERY
                    )
                }
            } else {
                if (data.data != null) { //사진
                    try {
                        var selPhoto: Bitmap? = null
                        var imgUri: Uri? = null
                        imgUri = data.data
                        val proj = arrayOf(
                            MediaStore.Images.Media.SIZE,
                            MediaStore.Images.Media.MINI_THUMB_MAGIC
                        )
                        val imageCursor =
                            contentResolver.query(imgUri!!, proj, null, null, null)
                        var lImageSize: Long = 0
                        if (imageCursor != null && imageCursor.moveToFirst()) {
                            val sizeCol =
                                imageCursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                            lImageSize = imageCursor.getLong(sizeCol)
                        }
                        val options = BitmapFactory.Options()
                        val iImageSize = lImageSize.toInt() / 1024
                        val iSampleSize = 1
                        options.inSampleSize = iSampleSize
                        val fb: ParcelFileDescriptor?
                        fb = contentResolver.openFileDescriptor(imgUri, "r")
                        selPhoto = BitmapFactory.decodeFileDescriptor(
                            fb!!.fileDescriptor,
                            null,
                            options
                        )
                        val size = selPhoto.byteCount
                        val rotate: Int = getOrientation(imgUri)
                        val matrix = Matrix()
                        matrix.postRotate(rotate.toFloat())
                        val width = selPhoto.width.toFloat()
                        val hight = selPhoto.height.toFloat()
                        var rotateBitmap = Bitmap.createBitmap(
                            selPhoto,
                            0,
                            0,
                            width.toInt(),
                            hight.toInt(),
                            matrix,
                            true
                        )
                        var realWidth: Int = IMAGE_SIZE
                        var realHeight: Int = IMAGE_SIZE
                        if (width > IMAGE_SIZE || hight > IMAGE_SIZE) {
                            val scaleSize: Double
                            if (rotateBitmap.getWidth() > rotateBitmap.getHeight()) {
                                scaleSize =
                                    1024.0 / rotateBitmap.getWidth() * rotateBitmap.getHeight()
                                realHeight = Math.round(scaleSize).toString().toInt()
                            } else {
                                scaleSize =
                                    1024.0 / rotateBitmap.getHeight() * rotateBitmap.getWidth()
                                realWidth = Math.round(scaleSize).toString().toInt()
                            }
                        } else {
                            realWidth = rotateBitmap.getWidth()
                            realHeight = rotateBitmap.getHeight()
                        }
                        rotateBitmap =
                            Bitmap.createScaledBitmap(rotateBitmap, realWidth, realHeight, true)
                        val byteArray =
                            ByteArrayOutputStream()
                        rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArray)
                        val by = byteArray.toByteArray()
                        val encoding =
                            Base64.encodeToString(by, Base64.NO_WRAP)
                        sendImage(encoding)
                        //                        selectImageView.setBackgroundResource(R.drawable.imageview_border);
                    } catch (e: FileNotFoundException) { // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                } else {
                    val rotateBitmap = data.extras!!["data"] as Bitmap
                    val size: Int = rotateBitmap.getByteCount()
                    val byteArray = ByteArrayOutputStream()
                    rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArray)
                    val by = byteArray.toByteArray()
                    val encoding =
                        Base64.encodeToString(by, Base64.DEFAULT)
                    sendImage(encoding)
                    //                    selectImageView.setBackgroundResource(R.drawable.imageview_border);
                }
            }
        }

    }

    /**
     * 사진 전송
     */
    private fun sendImage(image: String) {
        val url = "javascript:mfRunCameraCallback('$image')"
        webView.loadUrl(url)
    }

    private fun getImageOrientation(path: String?): Int {
        var rotation = 0
        try {
            val exif = ExifInterface(path)
            val rot = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            rotation = if (rot == ExifInterface.ORIENTATION_ROTATE_90) {
                90
            } else if (rot == ExifInterface.ORIENTATION_ROTATE_180) {
                180
            } else if (rot == ExifInterface.ORIENTATION_ROTATE_270) {
                270
            } else {
                0
            }
        } catch (e: Exception) { // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return rotation
    }

    fun getOrientation(selectedImage: Uri?): Int {
        try {
            var orientation = 0
            val projection =
                arrayOf(MediaStore.Images.Media.ORIENTATION)
            val cursor = this.contentResolver.query(
                selectedImage!!,
                projection, null, null, null
            )
            if (cursor != null) {
                val orientationColumnIndex = cursor
                    .getColumnIndex(MediaStore.Images.Media.ORIENTATION)

                if (cursor.moveToFirst()) {
                    orientation = if (cursor.isNull(orientationColumnIndex)) 0 else cursor.getInt(
                        orientationColumnIndex
                    )
                }
                cursor.close()
            }
            return orientation
        } catch (e: Exception) {
            return 0
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (isAppValidityCheckPass()) {
            webviewSetting(this)
            val stepCounterService = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(stepCounterService)
            } else {
                startService(stepCounterService)
            }
        }

    }

    /**
     * back 이벤트
     */
    override fun onBackPressed() {
        val url = "javascript:cfMoveHeader('BACK')"

        if (isErrorPage) {
            // 페이지에 오류가 있을시 네이티브에서 컨트롤
            showOneButtonDialog(
                getString(R.string.alert),
                getString(R.string.network_error3),
                true
            )
        } else {
            val url = "javascript:cfMoveHeader('BACK')"
            webView.post(Runnable() {
                webView!!.loadUrl(url)
            })
        }


    }

    /**
     * 앱 종료시 서비스 종료
     */
    override fun onDestroy() {
        stopService(Intent(this, LocationService::class.java))
        super.onDestroy()
    }
}