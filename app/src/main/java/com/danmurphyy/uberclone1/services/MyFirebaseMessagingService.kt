package com.danmurphyy.uberclone1.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.danmurphyy.uberclone1.DriverHomeActivity
import com.danmurphyy.uberclone1.R
import com.danmurphyy.uberclone1.services.model.DriverRequestReceived
import com.danmurphyy.uberclone1.utils.Constants
import com.danmurphyy.uberclone1.utils.Constants.CHANNEL_ID
import com.danmurphyy.uberclone1.utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (FirebaseAuth.getInstance().currentUser != null)
            UserUtils.updateToken(this, token)
        super.onNewToken(token)
    }


    @SuppressLint("ObsoleteSdkInt")
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val data = message.data
        if (data[Constants.NOTI_TITLE].equals(Constants.REQUEST_DRIVER_TITLE)) {
            val driverRequestReceived = DriverRequestReceived()
            driverRequestReceived.key = data[Constants.RIDER_KEY]
            driverRequestReceived.pickupLocation = data[Constants.PICKUP_LOCATION]
            driverRequestReceived.pickupLocationString = data[Constants.PICKUP_LOCATION_STRING]
            driverRequestReceived.destinationLocation = data[Constants.DESTINATION_LOCATION]
            driverRequestReceived.destinationLocationString = data[Constants.DESTINATION_LOCATION_STRING]

            driverRequestReceived.distanceValue = data[Constants.RIDER_DISTANCE_VALUE]!!.toInt()
            driverRequestReceived.distanceText = data[Constants.RIDER_DISTANCE_TEXT]!!.toString()
            driverRequestReceived.durationValue = data[Constants.RIDER_DURATION_VALUE]!!.toInt()
            driverRequestReceived.durationText = data[Constants.RIDER_DURATION_TEXT]!!.toString()
            driverRequestReceived.totalFee = data[Constants.RIDER_TOTAL_FEE]!!.toDouble()



            EventBus.getDefault().postSticky(driverRequestReceived)
        } else {
            if (currentUser != null) {
                val intent = Intent(this, DriverHomeActivity::class.java)
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationID = Random.nextInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel(notificationManager)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
                // Play the custom sound
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(message.data["title"])
                    .setContentText(message.data["message"])
                    .setSmallIcon(R.drawable.ic_stat_ic_notification)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager.notify(notificationID, notification)

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply {
            description = "New Orders"
            enableLights(true)
            lightColor = Color.GREEN
        }
        notificationManager.createNotificationChannel(channel)
    }
}