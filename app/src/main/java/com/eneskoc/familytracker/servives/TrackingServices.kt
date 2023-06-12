package com.eneskoc.familytracker.servives

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.eneskoc.familytracker.other.Constants.ACTION_PAUSE_SERVICE
import com.eneskoc.familytracker.other.Constants.ACTION_SHOW_HOME_FRAGMENT
import com.eneskoc.familytracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.eneskoc.familytracker.other.Constants.ACTION_STOP_SERVICE
import com.eneskoc.familytracker.other.Constants.LOCATION_UPDATE_INTERVAL
import com.eneskoc.familytracker.other.Constants.NOTIFICATION_CHANNEL_ID
import com.eneskoc.familytracker.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.eneskoc.familytracker.other.Constants.NOTIFICATION_ID
import com.eneskoc.familytracker.other.TrackingUtil
import com.eneskoc.familytracker.ui.MainActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

class TrackingServices : LifecycleService() {

    var isFirstTrack=true
    var serviceKilled=false

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var batteryManager: BatteryManager

    companion object{
        val isTracking = MutableLiveData<Boolean>()
        var location =  MutableLiveData<LatLng>()
        var batteryLevel = MutableLiveData<Int>()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        isTracking.observe(this, Observer {
            updateLocationTracking(it)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE ->{
                    if (isFirstTrack){
                        startForegroundService()
                        isFirstTrack=false
                    }else {
                        println("Resuming services...")
                    }
                    println("Started or resumed service")
                }
                ACTION_PAUSE_SERVICE ->{
                    println("Pause service")
                    pauseService()
                    isFirstTrack=true
                }
                ACTION_STOP_SERVICE ->{
                    println("Stop service")
                    killService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun killService(){
        serviceKilled=true
        isFirstTrack=true
        pauseService()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private  fun pauseService(){
        isTracking.postValue(false)
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking:Boolean){
        if(isTracking){
            if(TrackingUtil.hasLocationPermission(this)){
                val request= LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,LOCATION_UPDATE_INTERVAL).apply {
                    setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                    setWaitForAccurateLocation(true)
                }.build()

                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper())
            }
        }else{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }


    val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (isTracking.value!!){
                result.lastLocation?.let { lastLocation ->
                    val updatedLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    location.value = updatedLocation
                }
                batteryLevel.value = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            }
        }
    }

    private fun startForegroundService(){
        isTracking.postValue(true)

        val notificationManager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true) //Block swipe
            .setSmallIcon(com.google.android.material.R.drawable.mtrl_checkbox_button_icon_indeterminate_checked)
            .setContentTitle("Family Tracker")
            .setContentText("Your family follow you and know you're safe")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID,notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent()= PendingIntent.getActivity(
        this,
        0,
        Intent(this,MainActivity::class.java).also {
            it.action = ACTION_SHOW_HOME_FRAGMENT
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
            )
        notificationManager.createNotificationChannel(channel)
    }

}