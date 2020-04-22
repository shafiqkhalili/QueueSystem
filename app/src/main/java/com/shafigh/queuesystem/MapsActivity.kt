package com.shafigh.queuesystem

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap

    ///Users/shafighkhalili/Library/Android/sdk/sources/android-29/java/security/KeyStore.java
    //private val TAG = MapsActivity::class.java.simpleName

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var locationCallback: LocationCallback

    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    private val REQUEST_LOCATION_PERMISSION = 1
    private val PLACE_PICKER_REQUEST = 3
    private val TAG = "MapActivity"
    private val DEFAULT_ZOOM = 15f
    private val mLocationPermissionsGranted = false


    //Widgets
    private lateinit var mSearchText: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mSearchText = findViewById(R.id.input_search)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val latitude = 59.187651
        val longitude = 17.913943

        val homeLatLng = LatLng(latitude, longitude)
        val zoomLevel = 15f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))

        //mMap.addMarker(MarkerOptions().position(homeLatLng))

        //setMapLongClick(mMap)
        setPoiClick(mMap)
        setMapStyle(mMap)
        enableMyLocation()
        init()
        setPadding()
        initAutocomplete()
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)
    }

    //Autocomplete test
    private fun initAutocomplete() {

        // Initialize the AutocompleteSupportFragment.
        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment: AutocompleteSupportFragment? =
            supportFragmentManager.findFragmentById(R.id.input_search) as AutocompleteSupportFragment?

        // Specify the types of place data to return.

        // Specify the types of place data to return.
        autocompleteFragment?.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))

        // Set up a PlaceSelectionListener to handle the response.

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // TODO: Get info about the selected place.
                Log.i(
                    TAG,
                    "Place: " + place.name.toString() + ", " + place.id
                )
            }

            override fun onError(p0: Status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: $p0")
            }
        })


    }

    //Add listener to Enter button
    private fun init() {
        //Enable Enter keyboard key
        hideKeyboard()
        mSearchText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event.action == KeyEvent.ACTION_DOWN ||
                event.action == KeyEvent.KEYCODE_ENTER
            ) {
                //Execute out method for searching
                geoLocate()
            }
            return@setOnEditorActionListener false
        }
    }

    //Menu options
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                /*.icon(BitmapDescriptorFactory.fromBitmap(
                    BitmapFactory.decodeResource(resources,R.drawable.ic_person_pin_circle_black_24dp)))*/
            )
            poiMarker.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed!")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }


    //Search place by string
    private fun geoLocate() {
        Log.d(TAG, "geoLocate: geolocating")
        val searchString = mSearchText.text.toString()
        val geocoder = Geocoder(this@MapsActivity)
        var list: List<Address> =
            ArrayList()
        try {
            list = geocoder.getFromLocationName(searchString, 1)
        } catch (e: IOException) {
            Log.e(
                TAG,
                "geoLocate: IOException: " + e.message
            )
        }
        if (list.isNotEmpty()) {
            val address = list[0]
            Log.d(TAG, "geoLocate: found a location: $address")
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            moveCamera(
                LatLng(address.latitude, address.longitude), DEFAULT_ZOOM, address.getAddressLine(0)
            )
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float, title: String) {
        Log.d(
            TAG,
            "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        if (title != "My Location") {
            val options = MarkerOptions()
                .position(latLng)
                .title(title)
            mMap.addMarker(options)
        }
        hideKeyboard()
    }

    //Hide keyboard
    private fun AppCompatActivity.hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        // else {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        // }
    }

    private fun setPadding() {
        mMap.setPadding(0, 200, 0, 0)
    }

//Auto complete test
/*
    private fun loadPlacePicker() {
        val builder = PlacePicker.IntentBuilder()

        try {
            startActivityForResult(builder.build(this@MapsActivity), PLACE_PICKER_REQUEST)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

    // 1
    private fun startLocationUpdates() {
        //1
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        //2
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null
)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                val place = PlacePicker.getPlace(this, data)
                var addressText = place.name.toString()
                addressText += "\n" + place.address.toString()

                //placeMarkerOnMap(place.latLng)
            }
        }
    }

    // 2
    override fun onPause() {
        super.onPause()
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    // 3
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }
*/
    override fun onMarkerClick(p0: Marker?) = false

}

