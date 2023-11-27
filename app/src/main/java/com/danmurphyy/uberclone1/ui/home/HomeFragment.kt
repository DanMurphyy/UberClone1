package com.danmurphyy.uberclone1.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.danmurphyy.uberclone1.DriverHomeActivity
import com.danmurphyy.uberclone1.R
import com.danmurphyy.uberclone1.databinding.FragmentHomeBinding
import com.danmurphyy.uberclone1.model.RiderModel
import com.danmurphyy.uberclone1.model.TripPlanModel
import com.danmurphyy.uberclone1.remote.IGoogleAPI
import com.danmurphyy.uberclone1.remote.RetrofitInstance
import com.danmurphyy.uberclone1.services.model.DriverRequestReceived
import com.danmurphyy.uberclone1.services.model.NotifyRiderEvent
import com.danmurphyy.uberclone1.utils.Constants
import com.danmurphyy.uberclone1.utils.Constants.DRIVERS_LOCATION_REFERENCES
import com.danmurphyy.uberclone1.utils.LocationUtils
import com.danmurphyy.uberclone1.utils.UserUtils
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kusu.loadingbutton.LoadingButton
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap
    private lateinit var _mapFragment: SupportMapFragment

    private var cityName: String = ""

    //Views
    private lateinit var chipDecline: Chip
    private lateinit var layoutAccept: CardView
    private lateinit var circularProgressBar: CircularProgressBar
    private lateinit var txtEstimateTime: TextView
    private lateinit var txtEstimateDistance: TextView
    private lateinit var rootLayout: FrameLayout

    private lateinit var txtRating: TextView
    private lateinit var txt_type_uber: TextView
    private lateinit var img_round: ImageView
    private lateinit var layout_start_uber: CardView
    private lateinit var txt_rider_name: TextView
    private lateinit var txt_start_uber_estimate_distance: TextView
    private lateinit var txt_start_uber_estimate_time: TextView
    private lateinit var img_phone_call: ImageView
    private lateinit var btn_start_uber: LoadingButton
    private lateinit var btn_complete_trip: LoadingButton

    private lateinit var layout_notify_rider: LinearLayout
    private lateinit var txt_notify_rider: TextView
    private lateinit var progress_notify: ProgressBar

    private var pickupGeoFire: GeoFire? = null
    private var pickGeoQuery: GeoQuery? = null

    private var destinationGeoFire: GeoFire? = null
    private var destinationGeoQuery: GeoQuery? = null

    private val pickupGeoQueryListener = object : GeoQueryEventListener {
        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            btn_start_uber.isEnabled = true
            UserUtils.sendNotifyToRider(requireContext(), rootLayout, key!!)
            if (pickGeoQuery != null) {
                // Remove GeoQuery listeners after the driver arrives
                pickGeoQuery!!.removeAllListeners()
            }
        }

        override fun onKeyExited(key: String?) {
            btn_start_uber.isEnabled = false
        }

        override fun onKeyMoved(key: String?, location: GeoLocation?) {

        }

        override fun onGeoQueryReady() {
        }

        override fun onGeoQueryError(error: DatabaseError?) {
        }
    }

    private val destinationGeoQueryListener = object : GeoQueryEventListener {
        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            Toast.makeText(requireContext(), "Destination Entered", Toast.LENGTH_SHORT).show()
            btn_complete_trip.isEnabled = true
            Log.d("DriverArrivedAtPU", "onKeyEntered")
            if (destinationGeoQuery != null) {
                // Remove GeoQuery listeners after the driver arrives
                destinationGeoQuery!!.removeAllListeners()
            }
        }

        override fun onKeyExited(key: String?) {
        }

        override fun onKeyMoved(key: String?, location: GeoLocation?) {
        }

        override fun onGeoQueryReady() {
        }

        override fun onGeoQueryError(error: DatabaseError?) {
        }

    }

    private var isTripStart = false
    private var onlineSystemAlreadyRegistered = false

    private var waiting_timer: CountDownTimer? = null

    private var tripNumberId: String? = ""

    private var driverRequestReceived: DriverRequestReceived? = null
    private var notifyRiderEvent: NotifyRiderEvent? = null
    private var countDownEvent: Disposable? = null

    //Routes
    private val compositeDisposable = CompositeDisposable()
    private lateinit var googleAPI: IGoogleAPI
    private var blackPolyLine: Polyline? = null
    private var greyPolyLine: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolyLineOptions: PolylineOptions? = null
    private var polylineList: MutableList<LatLng>? = null

    //location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //online system
    private lateinit var onlineRef: DatabaseReference
    private var currentUserRef: DatabaseReference? = null
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire

    //delete the data if the user id disconnected
    private val onlineValueEventListener = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(_mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && currentUserRef != null) {
                currentUserRef!!.onDisconnect().removeValue()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        if (!onlineSystemAlreadyRegistered) {
            onlineRef.addValueEventListener(onlineValueEventListener)
            onlineSystemAlreadyRegistered = true
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        initViews(root)
        init()

        _mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        _mapFragment.getMapAsync(this)

        return root
    }

    private fun init() {
        googleAPI = RetrofitInstance.instance!!.create(IGoogleAPI::class.java)

        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).apply {
            setWaitForAccurateLocation(false)
            setMinUpdateIntervalMillis(15000)//15sec
            setMaxUpdateDelayMillis(10000) //10sec
            setMinUpdateDistanceMeters(50f) //50m
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                //set the location on map
                val newPos = LatLng(
                    locationResult.lastLocation!!.latitude, locationResult.lastLocation!!.longitude
                )
                if (pickupGeoFire != null) {
                    pickGeoQuery = pickupGeoFire!!.queryAtLocation(
                        GeoLocation(
                            locationResult.lastLocation!!.latitude,
                            locationResult.lastLocation!!.longitude
                        ), Constants.MIN_RANGE_PICKUP_IN_KM
                    )
                    pickGeoQuery!!.addGeoQueryEventListener(pickupGeoQueryListener)
                }

                if (destinationGeoFire != null) {
                    destinationGeoQuery = destinationGeoFire!!.queryAtLocation(
                        GeoLocation(
                            locationResult.lastLocation!!.latitude,
                            locationResult.lastLocation!!.longitude
                        ), Constants.MIN_RANGE_PICKUP_IN_KM
                    )
                    destinationGeoQuery!!.addGeoQueryEventListener(destinationGeoQueryListener)
                    Log.d("DriverArrivedAtPU", "onLocationResult")
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                if (!isTripStart) {

                    makeDriverOnline(locationResult.lastLocation!!)

                } else {
                    if (!TextUtils.isEmpty(tripNumberId)) {
                        //update location
                        val update_date = HashMap<String, Any>()
                        update_date["currentLat"] = locationResult.lastLocation!!.latitude
                        update_date["currentLng"] = locationResult.lastLocation!!.longitude

                        FirebaseDatabase.getInstance().getReference(Constants.TRIPS)
                            .child(tripNumberId!!)
                            .updateChildren(update_date)
                            .addOnFailureListener { e ->
                                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT)
                                    .show()
                            }
                            .addOnSuccessListener { }
                    }
                }
            }
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper()
            )
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 12
            )
        }

    }

    private fun makeDriverOnline(location: Location) {
        val saveCityName = cityName// first, save old city name to variable
        Log.d("LocationUpdate", "Old City: $saveCityName, New City: $cityName")
        cityName = LocationUtils.getAddressFromLocation(requireContext(), location)

        if (cityName != saveCityName) //if old city name and new city name are not same
        {
            if (currentUserRef != null) {
                Log.d(
                    "LocationUpdate",
                    "Before Remove: $currentUserRef"
                ) // Remove old location to prevent 1 driver having 2 locations in 2 areas
                currentUserRef!!.removeValue()
                    .addOnFailureListener { e ->
                        Snackbar.make(_mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG)
                            .show()
                    }
                    .addOnSuccessListener {
                        Log.d("LocationUpdate", "After Remove: $currentUserRef")
                        // Update driver location after removing old location
                        updateDriverLocation(location)
                    }
            } else {
                // currentUserRef is null, update driver location without deleting old position
                updateDriverLocation(location)
            }
        } else {
            // cityName has not changed, update driver location without deleting old position
            updateDriverLocation(location)
        }
    }

    private fun updateDriverLocation(location: Location) {
        if (!TextUtils.isEmpty(cityName)) // city name not empty
        {
            driversLocationRef =
                FirebaseDatabase.getInstance().getReference(DRIVERS_LOCATION_REFERENCES)
                    .child(cityName)
            currentUserRef =
                driversLocationRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
            geoFire = GeoFire(driversLocationRef)
            geoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid, GeoLocation(
                    location.latitude,
                    location.longitude
                )
            ) { _: String?, error: DatabaseError? ->
                if (error != null) {
                    Snackbar.make(
                        _mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            registerOnlineSystem()
        } else {
            Snackbar.make(
                _mapFragment.requireView(),
                getString(R.string.service_unavailable),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //check the permission
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //if we don't have permission
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 12
            )
            return
        }
        //when we have permission
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //Enable button first
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            //when i click on button
            mMap.setOnMyLocationButtonClickListener {
                Toast.makeText(context, "clicked", Toast.LENGTH_LONG).show()
                //get last location
                fusedLocationProviderClient.lastLocation.addOnFailureListener {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                    } else {
                        // Handle the case where the location is null
                        Toast.makeText(context, "Location is null", Toast.LENGTH_LONG).show()
                    }
                }

                true
            }
            //layout
            val locationButton = _mapFragment.view?.findViewById<View>("1".toInt())?.parent as View
            locationButton.findViewById<View>("2".toInt())
            val params = locationButton.layoutParams as? LinearLayout.LayoutParams
            params?.gravity = Gravity.BOTTOM
            params?.gravity = Gravity.START
            params?.bottomMargin = 50
        }
        //try to set map style
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.uber_maps_style
                )
            )
            if (!success) {
                Log.e("ErrorMap", "Map Style is not parsing")
            }
        } catch (e: Exception) {
            Log.e("ErrorMap", e.message!!)
        }
        Snackbar.make(
            _mapFragment.requireView(), "You 're online!", Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun initViews(view: View?) {
        chipDecline = view?.findViewById(R.id.chip_decline) as Chip
        layoutAccept = view.findViewById(R.id.layout_accept) as CardView
        circularProgressBar = view.findViewById(R.id.circul_progress_bar) as CircularProgressBar
        txtEstimateTime = view.findViewById(R.id.text_estimate_time)
        txtEstimateDistance = view.findViewById(R.id.text_estimate_distance)
        rootLayout = view.findViewById(R.id.root_layout)

        txtRating = binding.textRating
        txt_type_uber = binding.textTypeUber
        img_round = binding.imgRound
        layout_start_uber = binding.layoutStartUber
        txt_rider_name = binding.txtRiderName
        txt_start_uber_estimate_distance = binding.txtStartUberEstimateDistance
        txt_start_uber_estimate_time = binding.textEstimateTime
        img_phone_call = binding.imgPhoneCall
        btn_start_uber = binding.btnStartUber
        btn_complete_trip = binding.btnCompleteTrip

        layout_notify_rider = binding.layoutNotifyRider
        txt_notify_rider = binding.txtNotifyRider
        progress_notify = binding.progressNotify

        chipDecline.setOnClickListener {
            if (driverRequestReceived != null) {
                if (TextUtils.isEmpty(tripNumberId)) {
                    if (countDownEvent != null) {
                        countDownEvent!!.dispose()
                    }
                    chipDecline.visibility = View.GONE
                    layoutAccept.visibility = View.GONE
                    mMap.clear()
                    circularProgressBar.progress = 0f
                    UserUtils.sendDeclineRequest(
                        rootLayout,
                        activity,
                        driverRequestReceived!!.key!!
                    )
                    driverRequestReceived = null
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Snackbar.make(
                            _mapFragment.requireView(),
                            getString(R.string.permission_required),
                            Snackbar.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }
                    fusedLocationProviderClient.lastLocation
                        .addOnFailureListener { e ->
                            Snackbar.make(
                                _mapFragment.requireView(),
                                e.message!!,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        .addOnSuccessListener { location ->
                            chipDecline.visibility = View.GONE
                            layoutAccept.visibility = View.GONE
                            layout_start_uber.visibility = View.GONE
                            mMap.clear()
                            UserUtils.sendDeclineAndRemoveTripRequest(
                                rootLayout,
                                requireActivity(),
                                driverRequestReceived!!.key!!,
                                tripNumberId
                            )
                            tripNumberId = ""
                            driverRequestReceived = null
                            makeDriverOnline(location)
                        }
                }
            }
        }
        btn_start_uber.setOnClickListener {
            if (blackPolyLine != null) blackPolyLine!!.remove()
            if (greyPolyLine != null) greyPolyLine!!.remove()
            //Cancel waiting time
            if (waiting_timer != null) waiting_timer!!.cancel()
            layout_notify_rider.visibility = View.GONE
            if (driverRequestReceived != null) {
                val destinationLatLng = LatLng(
                    driverRequestReceived!!.destinationLocation!!.split(",")[0].toDouble(),
                    driverRequestReceived!!.destinationLocation!!.split(",")[1].toDouble()
                )
                mMap.addMarker(
                    MarkerOptions().position(destinationLatLng)
                        .title(driverRequestReceived!!.destinationLocationString)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                )

                drawPathFromCurrentLocation(driverRequestReceived!!.destinationLocation)
            }
            btn_start_uber.visibility = View.GONE
            chipDecline.visibility = View.GONE
            btn_complete_trip.visibility = View.VISIBLE
        }
        btn_complete_trip.setOnClickListener {
            //first, update trip set done to true
            val updateTrip = HashMap<String, Any>()
            updateTrip["done"] = true
            FirebaseDatabase.getInstance()
                .getReference(Constants.TRIPS)
                .child(tripNumberId!!)
                .updateChildren(updateTrip)
                .addOnFailureListener { e ->
                    Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
                }
                .addOnSuccessListener {
                    fusedLocationProviderClient.lastLocation
                        .addOnFailureListener { e ->
                            Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
                        }
                        .addOnSuccessListener { location ->
                            UserUtils.sendCompleteTripToRider(
                                _mapFragment.requireView(),
                                requireContext(),
                                driverRequestReceived!!.key!!,
                                tripNumberId!!
                            )
                            mMap.clear()
                            tripNumberId = ""
                            isTripStart = false
                            chipDecline.visibility = View.GONE
                            layoutAccept.visibility = View.GONE
                            circularProgressBar.progress = 0f
                            layout_start_uber.visibility = View.GONE
                            layout_start_uber.visibility = View.GONE
                            progress_notify.progress = 0
                            btn_complete_trip.isEnabled = false
                            btn_complete_trip.visibility = View.GONE
                            btn_start_uber.isEnabled = false
                            btn_start_uber.visibility = View.VISIBLE
                            destinationGeoFire = null
                            pickupGeoFire = null

                            driverRequestReceived = null
                            makeDriverOnline(location)
                        }
                }
        }
    }

    private fun drawPathFromCurrentLocation(destinationLocation: String?) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(requireView(), "You need permission!", Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location ->
                //Copy code from Rider app
                compositeDisposable.add(googleAPI.getDirections(
                    "driving",
                    "less_driving",
                    StringBuilder()
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString(),
                    destinationLocation,
                    getString(R.string.google_api)
                ).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { returnResult ->
                            Log.d("API_RETURN", returnResult)
                            try {

                                val jsonObject = JSONObject(returnResult)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                for (i in 0 until jsonArray.length()) {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyLine = poly.getString("points")
                                    polylineList = Constants.decodePoly(polyLine)
                                }

                                polylineOptions = PolylineOptions()
                                polylineOptions!!.color(Color.GRAY)
                                polylineOptions!!.width(12f)
                                polylineOptions!!.startCap(SquareCap())
                                polylineOptions!!.jointType(JointType.ROUND)
                                polylineOptions!!.addAll(polylineList!!)
                                greyPolyLine = mMap.addPolyline(polylineOptions!!)

                                blackPolyLineOptions = PolylineOptions()
                                blackPolyLineOptions!!.color(Color.BLACK)
                                blackPolyLineOptions!!.width(5f)
                                blackPolyLineOptions!!.startCap(SquareCap())
                                blackPolyLineOptions!!.jointType(JointType.ROUND)
                                blackPolyLineOptions!!.addAll(polylineList!!)
                                blackPolyLine = mMap.addPolyline(blackPolyLineOptions!!)


                                val origin = LatLng(location.latitude, location.longitude)
                                val destination = LatLng(
                                    destinationLocation!!.split(",")[0].toDouble(),
                                    destinationLocation.split(",")[1].toDouble()
                                )

                                val latLngBound =
                                    LatLngBounds.Builder()
                                        .include(origin)
                                        .include(destination)
                                        .build()

                                mMap.moveCamera(
                                    CameraUpdateFactory.newLatLngBounds(latLngBound, 160)
                                )
                                mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom - 1))
                                createGeoFireDestinationLocation(
                                    driverRequestReceived!!.key,
                                    destination
                                )

                            } catch (e: Exception) {
                                val errorMessage = e.message ?: "Unknown error"
                                Snackbar.make(
                                    _mapFragment.requireView(),
                                    errorMessage,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        },
                        { error ->
                            // Handle the error case
                            Log.e("API_ERROR", error.message, error)
                            // Display an error message or take appropriate action
                        }
                    )

                )
            }
    }

    private fun createGeoFireDestinationLocation(key: String?, destination: LatLng) {
        val ref =
            FirebaseDatabase.getInstance().getReference(Constants.TRIP_DESTINATION_LOCATION_REF)
        destinationGeoFire = GeoFire(ref)
        destinationGeoFire!!.setLocation(
            key!!,
            GeoLocation(destination.latitude, destination.longitude)
        ) { _, _ ->

        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDriverRequestReceived(event: DriverRequestReceived) {
        driverRequestReceived = event
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(requireView(), "You need permission!", Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener { location ->
                //Copy code from Rider app
                compositeDisposable.add(googleAPI.getDirections(
                    "driving",
                    "less_driving",
                    StringBuilder()
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .toString(),
                    event.pickupLocation,
                    getString(R.string.google_api)
                ).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { returnResult ->
                            Log.d("API_RETURN", returnResult)
                            try {

                                val jsonObject = JSONObject(returnResult)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                for (i in 0 until jsonArray.length()) {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyLine = poly.getString("points")
                                    polylineList = Constants.decodePoly(polyLine)
                                }

                                polylineOptions = PolylineOptions()
                                polylineOptions!!.color(Color.GRAY)
                                polylineOptions!!.width(12f)
                                polylineOptions!!.startCap(SquareCap())
                                polylineOptions!!.jointType(JointType.ROUND)
                                polylineOptions!!.addAll(polylineList!!)
                                greyPolyLine = mMap.addPolyline(polylineOptions!!)

                                blackPolyLineOptions = PolylineOptions()
                                blackPolyLineOptions!!.color(Color.BLACK)
                                blackPolyLineOptions!!.width(5f)
                                blackPolyLineOptions!!.startCap(SquareCap())
                                blackPolyLineOptions!!.jointType(JointType.ROUND)
                                blackPolyLineOptions!!.addAll(polylineList!!)
                                blackPolyLine = mMap.addPolyline(blackPolyLineOptions!!)

                                //Animator
                                val valueAnimator = ValueAnimator.ofInt(0, 100)
                                valueAnimator.duration = 1100
                                valueAnimator.repeatCount = ValueAnimator.INFINITE
                                valueAnimator.interpolator = LinearInterpolator()
                                valueAnimator.addUpdateListener { _ ->
                                    val points = greyPolyLine!!.points
                                    val percentValue =
                                        valueAnimator.animatedValue.toString().toInt()
                                    val size = points.size
                                    val newPoints = (size * (percentValue / 100f)).toInt()
                                    val p = points.subList(0, newPoints)
                                    blackPolyLine!!.points = p
                                }
                                valueAnimator.start()


                                val origin = LatLng(location.latitude, location.longitude)
                                val destination = LatLng(
                                    event.pickupLocation!!.split(",")[0].toDouble(),
                                    event.pickupLocation!!.split(",")[1].toDouble()
                                )

                                val latLngBound =
                                    LatLngBounds.Builder()
                                        .include(origin)
                                        .include(destination)
                                        .build()

                                val objects = jsonArray.getJSONObject(0)
                                val legs = objects.getJSONArray("legs")
                                val legsObject = legs.getJSONObject(0)

                                val time = legsObject.getJSONObject("duration")
                                val duration = time.getString("text")

                                val distanceEstimate = legsObject.getJSONObject("distance")
                                val distance = distanceEstimate.getString("text")

                                txtEstimateTime.text = duration
                                txtEstimateDistance.text = distance

                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(destination)
                                        .icon(BitmapDescriptorFactory.defaultMarker())
                                        .title("Pickup Location")
                                )

                                mMap.moveCamera(
                                    CameraUpdateFactory.newLatLngBounds(
                                        latLngBound,
                                        160
                                    )
                                )
                                mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom - 1))
                                createGeoFirePickupLocation(event.key, destination)

                                //Display layout
                                chipDecline.visibility = View.VISIBLE
                                layoutAccept.visibility = View.VISIBLE

                                // Countdown
                                countDownEvent = Observable.interval(100, TimeUnit.MILLISECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .takeUntil {
                                        // Adjust the condition based on your requirements
                                        it >= 100
                                    }
                                    .doOnNext { _ ->
                                        // Update the progress based on the count
                                        circularProgressBar.progress += 1f
                                    }
                                    .takeUntil { aLong -> aLong == "100".toLong() }
                                    .doOnComplete {
                                        createTripPlan(event, duration, distance)
                                    }
                                    .subscribe(
                                        { _ -> },
                                        { error ->
                                            Log.d("reactivex", error.message!!)
                                            // Handle the error here, e.g., show an error message
                                            Toast.makeText(
                                                requireContext(),
                                                "Error: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )


                            } catch (e: Exception) {
                                val errorMessage = e.message ?: "Unknown error"
                                Snackbar.make(
                                    _mapFragment.requireView(),
                                    errorMessage,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        },
                        { error ->
                            // Handle the error case
                            Log.e("API_ERROR", error.message, error)
                            // Display an error message or take appropriate action
                        }
                    )

                )
            }
    }

    private fun createGeoFirePickupLocation(key: String?, destination: LatLng) {
        val ref = FirebaseDatabase.getInstance()
            .getReference(Constants.TRIP_PICKUP_REF)
        pickupGeoFire = GeoFire(ref)
        pickupGeoFire!!.setLocation(
            key, GeoLocation(destination.latitude, destination.longitude)
        ) { key1, error ->
            if (error != null)
                Snackbar.make(rootLayout, error.message, Snackbar.LENGTH_LONG).show()
            else
                Log.d("EDMTDev", key1 + "was create success")
//            // Create GeoQuery for pickup location
//            pickGeoQuery = pickupGeoFire?.queryAtLocation(
//                GeoLocation(destination.latitude, destination.longitude),
//                Constants.MIN_RANGE_PICKUP_IN_KM
//            )
//            pickGeoQuery?.addGeoQueryEventListener(this)
        }

    }

    private fun createTripPlan(event: DriverRequestReceived, duration: String, distance: String) {
        setLayoutProcess(true)
        //Sync server time with device
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val timeOffset = snapshot.getValue(Long::class.java)
                    val estimateTimeInMs = System.currentTimeMillis() + timeOffset!!
                    val timeText = SimpleDateFormat("dd/MM/yyy HH:mm aa")
                        .format(estimateTimeInMs)

                    //Load rider Information
                    FirebaseDatabase.getInstance()
                        .getReference(Constants.RIDER_INFO)
                        .child(event.key!!)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val riderModel = snapshot.getValue(RiderModel::class.java)
                                    //get location

                                    if (ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        Snackbar.make(
                                            _mapFragment.requireView(),
                                            requireContext().getString(R.string.permission_required),
                                            Snackbar.LENGTH_LONG
                                        ).show()

                                        return
                                    }
                                    fusedLocationProviderClient.lastLocation
                                        .addOnFailureListener { e ->
                                            Snackbar.make(
                                                _mapFragment.requireView(),
                                                e.message!!,
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                        }
                                        .addOnSuccessListener { location ->
                                            //create Trip Planner
                                            val tripPlanModel = TripPlanModel()
                                            tripPlanModel.driver =
                                                FirebaseAuth.getInstance().currentUser!!.uid
                                            tripPlanModel.rider = event.key
                                            tripPlanModel.driverInfoModel = Constants.currentDriver
                                            tripPlanModel.riderModel = riderModel
                                            tripPlanModel.origin = event.pickupLocation
                                            tripPlanModel.originString = event.pickupLocationString
                                            tripPlanModel.destination = event.destinationLocation
                                            tripPlanModel.destinationString =
                                                event.destinationLocationString
                                            tripPlanModel.distancePickUp = distance
                                            tripPlanModel.durationPickUp = duration
                                            tripPlanModel.currentLat = location.latitude
                                            tripPlanModel.currentLng = location.longitude

                                            //new Info
                                            tripPlanModel.timeText = timeText
                                            tripPlanModel.distanceText = event.distanceText!!
                                            tripPlanModel.durationText = event.durationText!!
                                            tripPlanModel.durationValue = event.durationValue
                                            tripPlanModel.distanceValue = event.distanceValue
                                            tripPlanModel.totalFee = event.totalFee

                                            tripNumberId =
                                                Constants.createUniqueTripIdNumber(timeOffset)

                                            //Submit
                                            FirebaseDatabase.getInstance()
                                                .getReference(Constants.TRIPS)
                                                .child(tripNumberId!!)
                                                .setValue(tripPlanModel)
                                                .addOnFailureListener { e ->
                                                    Snackbar.make(
                                                        _mapFragment.requireView(),
                                                        e.message!!,
                                                        Snackbar.LENGTH_LONG
                                                    ).show()
                                                }
                                                .addOnSuccessListener {
                                                    txt_rider_name.text = riderModel!!.firstName
                                                    txtEstimateDistance.text = distance
                                                    txtEstimateTime.text = duration

                                                    setOfflineModeForDriver(
                                                        event,
                                                        duration,
                                                        distance
                                                    )
                                                }
                                        }


                                } else {
                                    Snackbar.make(
                                        _mapFragment.requireView(),
                                        requireContext().getString(R.string.rider_not_found) + " " + event.key,
                                        Snackbar.LENGTH_LONG
                                    ).show()

                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(
                                    _mapFragment.requireView(),
                                    error.message,
                                    Snackbar.LENGTH_LONG
                                ).show()

                            }

                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(_mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG)
                        .show()
                }

            })
    }

    private fun setOfflineModeForDriver(
        event: DriverRequestReceived,
        duration: String,
        distance: String,
    ) {

        UserUtils.sendAcceptRequestToRider(
            _mapFragment.view,
            requireContext(),
            event.key!!,
            tripNumberId
        )

        //Go to offline
        if (currentUserRef != null) currentUserRef!!.removeValue()

        setLayoutProcess(false)
        layoutAccept.visibility = View.GONE
        layout_start_uber.visibility = View.VISIBLE

        isTripStart = true
    }

    private fun setLayoutProcess(process: Boolean) {
        val color: Int
        if (process) {
            color = ContextCompat.getColor(requireContext(), R.color.gray)
            circularProgressBar.indeterminateMode = true
            binding.textRating.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.baseline_star_24,
                0
            )
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.white)
            circularProgressBar.indeterminateMode = false
            circularProgressBar.progress = 0F
            binding.textRating.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_star, 0)
        }

        txtEstimateTime.setTextColor(color)
        txtEstimateDistance.setTextColor(color)
        txtRating.setTextColor(color)
        txt_type_uber.setTextColor(color)
        ImageViewCompat.setImageTintList(img_round, ColorStateList.valueOf(color))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)

        compositeDisposable.clear()
        onlineSystemAlreadyRegistered = false

        if (EventBus.getDefault().hasSubscriberForEvent(DriverHomeActivity::class.java))
            EventBus.getDefault().removeStickyEvent(DriverHomeActivity::class.java)
        if (EventBus.getDefault().hasSubscriberForEvent(NotifyRiderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(NotifyRiderEvent::class.java)
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onNotifyRider(event: NotifyRiderEvent) {
        Log.d("DriverArrivedAtPU", "onNotifyRiderHome")
        layout_notify_rider.visibility = View.VISIBLE
        progress_notify.max = Constants.WAIT_TIME_IN_MIN * 60
        val countDownTimer = object : CountDownTimer((progress_notify.max * 1000).toLong(), 1000) {
            override fun onTick(l: Long) {
                progress_notify.progress += 1
                txt_notify_rider.text = String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(l) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(l)
                    ),
                    TimeUnit.MILLISECONDS.toSeconds(l) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(l)
                    )
                )
            }

            override fun onFinish() {
                Snackbar.make(rootLayout, getText(R.string.time_over), Snackbar.LENGTH_LONG).show()
            }
        }
            .start()
    }
}