package com.danmurphyy.uberclone1.utils

import com.danmurphyy.uberclone1.model.DriverInfoModel
import com.google.android.gms.maps.model.LatLng
import kotlin.random.Random

object Constants {

    const val RIDER_TOTAL_FEE = "TotalFeeRider"
    const val RIDER_DISTANCE_TEXT = "DistanceRider"
    const val RIDER_DURATION_TEXT = "DurationRider"
    const val RIDER_DISTANCE_VALUE = "DistanceRiderValue"
    const val RIDER_DURATION_VALUE = "DurationRiderValue"
    const val RIDER_REQUEST_COMPLETE_TRIP: String = "RequestCompleteTripToRider"
    const val REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP: String = "DeclineAndRemoveTrip"
    const val TRIP_DESTINATION_LOCATION_REF: String = "TripDestinationLocation"
    const val WAIT_TIME_IN_MIN: Int = 1
    const val MIN_RANGE_PICKUP_IN_KM: Double = 0.05 //50m
    const val TRIP_PICKUP_REF: String = "TripPickupLocation"
    const val TRIP_KEY: String = "TripKey"
    const val REQUEST_DRIVER_ACCEPT: String = "Accept"
    const val RIDER_INFO: String = "Riders"
    const val TRIPS: String = "Trips"
    const val DESTINATION_LOCATION: String = "DestinationLocation"
    const val DESTINATION_LOCATION_STRING: String = "DestinationLocationString"
    const val PICKUP_LOCATION_STRING: String = "PickupLocationString"
    const val DRIVER_KEY: String = "DriverKey"
    const val REQUEST_DRIVER_DECLINE = "Decline"
    const val RIDER_KEY: String = "RiderKey"
    const val PICKUP_LOCATION: String = "PickIupLocation"
    const val REQUEST_DRIVER_TITLE: String = "RequestDriver"
    const val NOTI_BODY: String = "body"
    const val NOTI_TITLE: String = "title"
    const val TOKEN_REFERENCE: String = "Token"
    const val DRIVER_INFO_REFERENCE = "DriverInfo"
    const val DRIVERS_LOCATION_REFERENCES: String = "DriversLocation"
    const val CHANNEL_ID = "myChannel"
    var currentDriver: DriverInfoModel? = null

    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentDriver!!.firstName)
            .append(" ")
            .append(currentDriver!!.lastName)
            .toString()
    }

    //DECODE POLY
    fun decodePoly(encoded: String): MutableList<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    fun createUniqueTripIdNumber(timeOffset: Long?): String {
        val rd = Random
        val current = System.currentTimeMillis() + timeOffset!!
        var unique = current + rd.nextLong()
        if (unique < 0) unique *= -1
        return unique.toString()
    }
}