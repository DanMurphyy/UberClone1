package com.danmurphyy.uberclone1.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.danmurphyy.uberclone1.R
import com.danmurphyy.uberclone1.model.TokenModel
import com.danmurphyy.uberclone1.remote.FCMSendData
import com.danmurphyy.uberclone1.remote.FCMService
import com.danmurphyy.uberclone1.remote.RetrofitFCM
import com.danmurphyy.uberclone1.services.model.NotifyRiderEvent
import com.danmurphyy.uberclone1.utils.Constants.DRIVER_INFO_REFERENCE
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

object UserUtils {
    fun updateUse(context: Context, updateData: HashMap<String, Any>) {
        FirebaseDatabase.getInstance().getReference(DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener {
                Toast.makeText(context, it.message!!, Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Toast.makeText(context, "Update information success", Toast.LENGTH_LONG).show()
            }
    }

    fun updateToken(context: Context, token: String) {
        val tokenModel = TokenModel()
        tokenModel.token = token
        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener { e ->
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener { }
    }

    fun sendDeclineRequest(view: View, activity: Activity?, key: String) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)
        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tokenModel = snapshot.getValue(TokenModel::class.java)
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[Constants.NOTI_TITLE] = Constants.REQUEST_DRIVER_DECLINE
                        notificationData[Constants.NOTI_BODY] = "This is the notification body"
                        notificationData[Constants.DRIVER_KEY] =
                            FirebaseAuth.getInstance().currentUser!!.uid
                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)
                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(
                                        view,
                                        activity!!.getString(R.string.send_request_to_driver_failed),
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view, t!!.message!!, Snackbar.LENGTH_LONG).show()
                                }
                            ))
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(
                            view,
                            activity!!.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    fun sendAcceptRequestToRider(
        view: View?,
        requireContext: Context,
        key: String,
        tripNumberId: String?,
    ) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)

        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tokenModel = snapshot.getValue(TokenModel::class.java)
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[Constants.NOTI_TITLE] = Constants.REQUEST_DRIVER_ACCEPT
                        notificationData[Constants.NOTI_BODY] = "This is the notification body"
                        notificationData[Constants.DRIVER_KEY] =
                            FirebaseAuth.getInstance().currentUser!!.uid
                        notificationData[Constants.TRIP_KEY] = tripNumberId!!
                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)

                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(
                                        view!!,
                                        requireContext.getString(R.string.accept_failed),
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view!!, t!!.message!!, Snackbar.LENGTH_LONG)
                                        .show()
                                }
                            ))
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(
                            view!!,
                            requireContext.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view!!, error.message, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    fun sendNotifyToRider(context: Context, view: View, key: String) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)
        Log.d("DriverArrivedAtPU", "sendNotifyToRiderUtils")

        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tokenModel = snapshot.getValue(TokenModel::class.java)
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[Constants.NOTI_TITLE] =
                            context.getString(R.string.driver_arrived)
                        notificationData[Constants.NOTI_BODY] =
                            context.getString(R.string.your_driver_arrived)
                        notificationData[Constants.DRIVER_KEY] =
                            FirebaseAuth.getInstance().currentUser!!.uid
                        notificationData[Constants.RIDER_KEY] = key
                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)

                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(
                                        view,
                                        context.getString(R.string.accept_failed),
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                } else {
                                    Log.d("DriverArrivedAtPU", "EventBus.getDefault()")

                                    EventBus.getDefault().postSticky(NotifyRiderEvent())
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view, t!!.message!!, Snackbar.LENGTH_LONG)
                                        .show()
                                }
                            ))
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(
                            view,
                            context.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    fun sendDeclineAndRemoveTripRequest(
        view: View,
        activity: FragmentActivity,
        key: String,
        tripNumberId: String?,
    ) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)
        FirebaseDatabase.getInstance().getReference(Constants.TRIPS)
            .child(tripNumberId!!)
            .removeValue()
            .addOnFailureListener { e ->
                Snackbar.make(view, e.message!!, Snackbar.LENGTH_SHORT).show()
            }.addOnSuccessListener {
                //after remove success, we will send notific. to rider

                FirebaseDatabase.getInstance()
                    .getReference(Constants.TOKEN_REFERENCE)
                    .child(key)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val tokenModel = snapshot.getValue(TokenModel::class.java)
                                val notificationData: MutableMap<String, String> = HashMap()
                                notificationData[Constants.NOTI_TITLE] =
                                    Constants.REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP
                                notificationData[Constants.NOTI_BODY] =
                                    "This is the notification body"
                                notificationData[Constants.DRIVER_KEY] =
                                    FirebaseAuth.getInstance().currentUser!!.uid
                                val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)
                                compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ fcmResponse ->
                                        if (fcmResponse.success == 0) {
                                            compositeDisposable.clear()
                                            Snackbar.make(
                                                view,
                                                activity.getString(R.string.send_request_to_driver_failed),
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                        }
                                    },
                                        { t: Throwable? ->
                                            compositeDisposable.clear()
                                            Snackbar.make(
                                                view,
                                                t!!.message!!,
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                        }
                                    ))
                            } else {
                                compositeDisposable.clear()
                                Snackbar.make(
                                    view,
                                    activity.getString(R.string.token_not_found),
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                        }
                    })

            }

    }

    fun sendCompleteTripToRider(view: View, context: Context, key: String, tripNumberId: String) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCM.instance!!.create(FCMService::class.java)

        FirebaseDatabase.getInstance()
            .getReference(Constants.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val tokenModel = snapshot.getValue(TokenModel::class.java)
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[Constants.NOTI_TITLE] =
                            Constants.RIDER_REQUEST_COMPLETE_TRIP
                        notificationData[Constants.NOTI_BODY] = "This is the notification body"
                        notificationData[Constants.TRIP_KEY] = tripNumberId

                        val fcmSendData = FCMSendData(tokenModel!!.token, notificationData)
                        compositeDisposable.add(fcmService.sendNotification(fcmSendData)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ fcmResponse ->
                                if (fcmResponse.success == 0) {
                                    compositeDisposable.clear()
                                    Snackbar.make(
                                        view,
                                        context.getString(R.string.send_request_to_driver_failed),
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                } else {
                                    Snackbar.make(
                                        view,
                                        context.getString(R.string.complete_trip_success),
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            },
                                { t: Throwable? ->
                                    compositeDisposable.clear()
                                    Snackbar.make(view, t!!.message!!, Snackbar.LENGTH_LONG).show()
                                }
                            ))
                    } else {
                        compositeDisposable.clear()
                        Snackbar.make(
                            view,
                            context.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }
            })

    }
}

