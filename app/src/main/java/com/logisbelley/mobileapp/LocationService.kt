package com.logisbelley.mobileapp

import android.app.Service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

/**
 * 위치 정보 수집 서비스
 */
class LocationService : Service() {
    private var compositeDisposable = CompositeDisposable()
    private var disposable: Disposable? = null
    private val localBinder = LocalBinder()
    private var mLocation: Location? = null
    private var context: Context = this
    private var UPDATE_INTERVAL: Long = 1000//1분주기
    private var FASTEST_INTERVAL: Long = UPDATE_INTERVAL / 2 //1분주기
    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var builder: NotificationCompat.Builder? = null

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    var locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            if (locationResult!!.lastLocation != null) {
                mLocation = locationResult!!.lastLocation
                val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                val date = simpleDateFormat.format(mLocation!!.time)

                if (mLocation != null) {
                    LogisbelleyApplication.TIME = date
                    LogisbelleyApplication.curLocation = mLocation!!
                }
//                    Toast.makeText(context,"kimmi"+mLocation?.latitude,Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        try {
            if (compositeDisposable != null) {
                if(compositeDisposable.isDisposed) {
                    compositeDisposable.dispose()
                }
            }
            disposable!!.dispose()
            stopSelf()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().log(e.toString())
        }
        super.onDestroy()
    }


    override fun onCreate() {
        super.onCreate()

        var cnt = 0
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
        disposable = Observable
            .interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {

                if (!MySharedPreferences(this).gpsParam.equals("")) {
                    cnt++
                    if (cnt.toString() == MySharedPreferences(this).gpsCycle) {
                        cnt = 0

                        if (checkLocationServicesStatus()) {
                            if (mLocation != null) {
                                //수신확인
                                val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                                val date = simpleDateFormat.format(mLocation!!.time)
                                LogisbelleyApplication.curLocation = mLocation
                                insert(mLocation!!)
                            }
                        } else {
                            Toast.makeText(this, "위치설정이 꺼져있습니다.", Toast.LENGTH_SHORT).show()

                        }
                    }
                } else {
                    cnt = 0
                }

            }
    }

    override fun onBind(p0: Intent?): IBinder {
        return localBinder
    }

    @SuppressLint("CheckResult")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
        return START_NOT_STICKY
    }

    private fun insert(curLocation: Location) {

        val param = MySharedPreferences(this).gpsParam
        val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
        val date = simpleDateFormat.format(curLocation.time)

        val longitude = curLocation.longitude.toString()
        val latitude = curLocation.latitude.toString()

        send(
            param!!, latitude, longitude, date
        )


    }

    private fun sendLocation(hashMap: HashMap<String, String>) {
        compositeDisposable = CompositeDisposable()

        compositeDisposable.add(
            InterfaceApi.sendLocation(this, hashMap)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe({ response: LocationResponseModel ->
                }, { error: Throwable ->
                    error.printStackTrace()
                })
        )

    }

    private fun send(
        param: String, latt: String, lngt: String, gpsDt: String
    ) {
        val hashMap = HashMap<String, String>()
        hashMap[KeyInfo.KEY_PARAM] = param
        hashMap[KeyInfo.KEY_LATT] = latt
        hashMap[KeyInfo.KEY_LNGT] = lngt
        hashMap[KeyInfo.KEY_GPS_DT] = gpsDt
        sendLocation(hashMap)
    }


    private fun startLocationUpdates() {

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = FASTEST_INTERVAL
        locationRequest.interval = UPDATE_INTERVAL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasFineLocationPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            val hasCoarseLocationPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

            if (hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED
                || hasFineLocationPermission != PackageManager.PERMISSION_GRANTED
            ) {
                //퍼미션없음
                return
            }
        }

        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        builder = if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "location_service"
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "위치수집중",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
            NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }

        val drawable = resources.getDrawable(R.mipmap.main_icon)
        builder!!.setSmallIcon(R.mipmap.main_icon) //                    .setContent(remoteViews)c
            .setColor(Notification.COLOR_DEFAULT)
            .setContentTitle("위치수집중")
            .setContentText("로지스밸리 배송기사 앱 사용을 위한 위치 수집 중입니다.")
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setContentIntent(pendingIntent)

        startForeground(1, builder!!.build())
        fusedLocationClient!!.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    /**
     * 위치 상태 체크
     */
    private fun checkLocationServicesStatus(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
}