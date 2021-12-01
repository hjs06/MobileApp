package com.logisbelley.mobileapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_custom_dialog.view.*

/**
 * BaseActivity
 */
abstract class BaseActivity<B : ViewDataBinding>(@LayoutRes val layoutResId: Int) :
    AppCompatActivity() {
    lateinit var binding: B

    // 권한체크가 필요한 Permission
    private var permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    // 안드로이드 10이상 버전체크
    @RequiresApi(Build.VERSION_CODES.Q)
    private var permissions2 = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layoutResId)
        binding.lifecycleOwner = this
    }

    protected fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * 권한 체크
     */
    fun checkPermissions(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val arr2 = arrayListOf<String>()

            // 29 이상일때 체크 목록
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                for (permission in permissions2) {
                    val permissionCheck = ContextCompat.checkSelfPermission(this, permission)

                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        arr2.add(permission)
                    }
                }

            } else {
                // 29 미만 일때 체크 목록
                for (permission in permissions) {
                    val permissionCheck = ContextCompat.checkSelfPermission(this, permission)

                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        arr2.add(permission)
                    }
                }
            }
            // 권한체크가 모두 되어있을시 true
            return if (arr2.isEmpty()) {
                true
            } else {
                // 미 승인된 권한체크 항목이 있을 경우 false
                requestPermissions(arr2.toTypedArray(), 101)
                false
            }
        } else {
            return true
        }
    }

    /**
     * 위치정보 상태 체크
     */
    private fun checkLocationServicesStatus(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * 앱 유효성 체크
     * 1. 루팅체크
     * 2. 네트워크 체크
     * 3. 위치설정 체크
     * 4. 권한체크
     */
    fun isAppValidityCheckPass(): Boolean {

        //루팅체크
        if (!isRootingCheck()) {
            showOneButtonDialog(getString(R.string.alert), getString(R.string.rooting_error), true)
            return false
        }

        //네트워크 체크
        if (!isNetworkConnection()) {
            showOneButtonDialog(
                getString(R.string.alert),
                getString(R.string.network_error),
                true
            )
            return false
        }

        //GPS onoff 체크
        if (!checkLocationServicesStatus()) {
            showOneButtonDialog(
                getString(R.string.alert),
                getString(R.string.gps_error),
                true
            )
            return false
        }


        return true

    }


    /**
     * 알럿
     */
    fun showOneButtonDialog(title: String, message: String, isAppFinish: Boolean) {
        try {
            val dialog = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_custom_dialog, null)
            view.btn_cancel.visibility = View.GONE
            dialog.setView(view)
            val alertDialog = dialog.create()
            alertDialog.setCancelable(false)
//
//            view.tv_title.text = title
            view.tv_message.text = message

            view.btn_ok.setOnClickListener {
                if (isAppFinish) {
                    AppExit()
                } else {
                    finish()
                }
            }
            alertDialog.show()
        } catch (e: Exception) {

        }
    }

    /**
     * 알럿
     */
    fun showTwoButtonDialog2(title: String, message: String, callback: DialogCallBack) {
        try {
            val dialog = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(
                R.layout.dialog_custom_dialog,
                null
            )
            dialog.setView(view)
            val alertDialog = dialog.create()
            alertDialog.setCancelable(false)

//            view.tv_title.text = title
            view.tv_message.text = message

            view.btn_ok.setOnClickListener {
                callback.onOkButtonClicked()
                alertDialog.dismiss()
            }
            view.btn_cancel.setOnClickListener {
                callback.onCancelButtonClicked()
                alertDialog.dismiss()
            }
            alertDialog.show()
        } catch (e: Exception) {

        }
    }

    /**
     * 알럿
     */
    fun showOneButtonDialog2(title: String, message: String, callback: DialogCallBack) {
        try {
            val dialog = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(
                R.layout.dialog_custom_dialog,
                null
            )
            view.btn_cancel.visibility = View.GONE
            dialog.setView(view)
            val alertDialog = dialog.create()
            alertDialog.setCancelable(false)

//            view.tv_title.text = title
            view.tv_message.text = message

            view.btn_ok.setOnClickListener {
                callback.onOkButtonClicked()
                alertDialog.dismiss()
            }
            alertDialog.show()
        } catch (e: Exception) {

        }
    }

    /**
     * 앱종료
     */
    public fun AppExit() {
        try {
            stopService(Intent(this, LocationService::class.java))
        } catch (e: Exception) {
        }
        moveTaskToBack(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        }
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    /**
     * 알럿
     */
    fun showOneButtonDialog(
        activity: Activity,
        title: String,
        message: String,
        callback: DialogCallBack
    ) {

        val dialog = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(
            R.layout.dialog_custom_dialog,
            null
        )
        dialog.setView(view)
        val alertDialog = dialog.create()
        alertDialog.setCancelable(false)

        view.tv_message.text = message

        view.btn_cancel.setOnClickListener { callback.onCancelButtonClicked() }
        view.btn_ok.setOnClickListener { callback.onOkButtonClicked() }

        alertDialog.show()
    }


    /**
     * 네트워크 연결 여부 체크
     */
    fun isNetworkConnection(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else {
            false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }


    /**
     * 위치 설정 체크
     * true: 위치켜짐
     * false:위치꺼짐
     */
    fun isGpsUsable() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())


        var isCheck = false
        task.addOnSuccessListener { locationSettingsResponse ->
            if (!isIgnoringBatteryOptimizations()) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.data = Uri.parse("package:" + packageName);
                startActivityForResult(intent, 88);
            }
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    isCheck = true
                    exception.startResolutionForResult(this, 99)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }


    }

    /**
     * 배터리 최적화 모드 체크
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = applicationContext.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return powerManager.isIgnoringBatteryOptimizations(name)
        }
        return true
    }

    /**
     * 루팅체크
     */
    private fun isRootingCheck(): Boolean {
        var isRootingFlag = false

        try {
            Runtime.getRuntime().exec("su")
            isRootingFlag = true
        } catch (e: Exception) {
        }

        if (isRootingFlag) {
            //루팅이기때문에 종료
            return false
        }
        return true
    }

}