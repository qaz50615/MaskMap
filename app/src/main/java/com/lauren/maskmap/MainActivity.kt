package com.lauren.MaskMap

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.lauren.MaskMap.remote.ApiClient
import com.lauren.MaskMap.remote.ApiResult
import com.lauren.MaskMap.remote.ApiService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.include_infodialog.*
import kotlinx.android.synthetic.main.include_typechoose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), LocationListener {

    private var mMap: GoogleMap? = null
    private var dataList: MaskDataGson? = null
    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setPermission()
        initView()
        initData()

    }

    private fun initView() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            mMap = it
            mMap?.isMyLocationEnabled = true
            mMap?.uiSettings?.isMapToolbarEnabled = false
            mMap?.uiSettings?.isZoomControlsEnabled = true
//            updateLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
        }

        dialogClose.setOnClickListener {
            include_infoDialog.visibility = View.GONE
            include_typechoose.visibility = View.VISIBLE
        }
        all_marker.setOnClickListener {
            showAllMarker()
        }
        adult_marker.setOnClickListener {
            showAdultMarker()
        }
        child_marker.setOnClickListener {
            showChildMarker()
        }
        swipe.setOnRefreshListener {
            initData()
        }
    }

    private fun showAllMarker() {
        mMap?.clear()
        if (!dataList?.features.isNullOrEmpty()) {
            for (i in dataList?.features!!.indices) {
                var lat =
                    dataList?.features?.getOrNull(i)?.geometry?.coordinates?.getOrNull(
                        1
                    ) ?: 0.0
                var long =
                    dataList?.features?.getOrNull(i)?.geometry?.coordinates?.getOrNull(
                        0
                    ) ?: 0.0
                var latLng = LatLng(lat, long)
                val marker = mMap?.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//                                .icon(BitmapDescriptorFactory.fromBitmap(bitmapList[0]))
                )
                marker?.tag = i
                mMap?.setOnMarkerClickListener(MarkerClickListener())
            }
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun showAdultMarker() {
        mMap?.clear()
        if (!dataList?.features.isNullOrEmpty()) {
            for (i in dataList?.features!!.indices) {
                if (dataList?.features?.getOrNull(i)?.properties?.mask_adult != "0") {
                    var lat =
                        dataList?.features?.getOrNull(i)?.geometry?.coordinates?.getOrNull(
                            1
                        ) ?: 0.0
                    var long =
                        dataList?.features?.getOrNull(i)?.geometry?.coordinates?.getOrNull(
                            0
                        ) ?: 0.0
                    var latLng = LatLng(lat, long)
                    val marker = mMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
//                                    .icon(BitmapDescriptorFactory.fromBitmap(bitmapList[1]))
                    )
                    marker?.tag = i
                    mMap?.setOnMarkerClickListener(MarkerClickListener())
                }
            }
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun showChildMarker() {
        mMap?.clear()
        if (!dataList?.features.isNullOrEmpty()) {
            for (i in dataList?.features!!.indices) {
                if (dataList?.features?.getOrNull(i)?.properties?.mask_child != "0") {
                    var lat =
                        dataList?.features?.getOrNull(i)?.geometry?.coordinates?.getOrNull(
                            1
                        ) ?: 0.0
                    var long =
                        dataList?.features?.getOrNull(i)?.geometry?.coordinates?.getOrNull(
                            0
                        ) ?: 0.0
                    var latLng = LatLng(lat, long)
                    val marker = mMap?.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
//                                    .icon(BitmapDescriptorFactory.fromBitmap(bitmapList[2]))
                    )
                    marker?.tag = i
                    mMap?.setOnMarkerClickListener(MarkerClickListener())
                }
            }
            progressBar.visibility = View.INVISIBLE
        }
    }

    private fun updateLocation(location: Location?) {
        val myLocation = LatLng(location?.latitude?:23.9193026, location?.longitude?:120.6736842)
        Log.i("location", "${location?.latitude?:23.9193026}${location?.longitude?:120.6736842}")
        mMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                myLocation, 15f
            )
        )
    }

    private fun isGpsEnable(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun locationProvider() {
        if (isGpsEnable()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,0f, this)
        } else {
            Toast.makeText(this, "請開啟高精確度定位模式", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkSelfPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun setPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission()) {
                locationProvider()
                //權限沒問題則取定位系統
            } else {
                val permissions =
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                    )
                requestPermissions(permissions, 0)
                //跳視窗請求授權
            }
        } else {
            locationProvider()
        }
    }

    override fun onLocationChanged(location: Location?) {
        updateLocation(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    inner class MarkerClickListener: GoogleMap.OnMarkerClickListener {
        override fun onMarkerClick(it: Marker?): Boolean {
            var position = it?.tag as Int
            include_infoDialog.visibility = View.VISIBLE
            include_typechoose.visibility = View.INVISIBLE

            name.text =
                dataList?.features?.getOrNull(position)?.properties?.name
                    ?: ""
            address.text =
                dataList?.features?.getOrNull(position)?.properties?.address
                    ?: ""
            phone.text =
                dataList?.features?.getOrNull(position)?.properties?.phone
                    ?: ""
            adultMaskQtys.text =
                dataList?.features?.getOrNull(position)?.properties?.mask_adult
                    ?: ""
            childMaskQtys.text =
                dataList?.features?.getOrNull(position)?.properties?.mask_child
                    ?: ""
            val customNote = dataList?.features?.getOrNull(position)?.properties?.custom_note
                ?:""
            val mNote = dataList?.features?.getOrNull(position)?.properties?.note?:""
            if (customNote.isNullOrEmpty()) {
                if (mNote.isNotEmpty()) {
                    note.text = "藥局備註:$mNote"
                }
            } else {
                note.text = "藥局備註:$customNote"
            }
            return false
        }
    }

    private suspend fun fetchData():ApiResult<MaskDataGson> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = ApiClient().createService(ApiService::class.java).getData()
            ApiResult.Success(result)
        } catch (e:Exception) {
            ApiResult.Error(e)
        }
    }

    private fun initData () {
        CoroutineScope(Dispatchers.Main).launch {
            when (fetchData()) {
                is ApiResult.Success -> {
                    dataList = (fetchData() as ApiResult.Success).data
                    showAllMarker()
                    include_typechoose.visibility = View.VISIBLE
                    if(swipe.isRefreshing) swipe.isRefreshing = false
                }
                is ApiResult.Error -> {
                    progressBar.visibility = View.INVISIBLE
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("錯誤訊息")
                        .setMessage("擷取資料失敗，請稍後再試！")
                        .setCancelable(false)
                        .setNegativeButton("確定") { d, w ->
                            finish()
                        }
                        .show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == 0 ) {
            locationProvider()
        }
    }

//    private fun getData() {
//
//        val request = Request.Builder()
//            .url("https://raw.githubusercontent.com/kiang/pharmacies/master/json/points.json")
//            .get()
//            .build()
//
//        val call : Call = getDataClient.newCall(request)
//
//        call.enqueue(object : Callback {
//
//            override fun onFailure(call: Call, e: IOException) {
//                runOnUiThread {
//                    progressBar.visibility = View.INVISIBLE
//                    Log.d("getDataApiFailed", e.toString())
//                    AlertDialog.Builder(this@MainActivity)
//                        .setTitle("錯誤訊息")
//                        .setMessage("系統或網路出現問題，請稍後再試！")
//                        .setCancelable(false)
//                        .setNegativeButton("確定") { d, w ->
//                            d.cancel()
//                            finish()
//                        }
//                        .show()
//                }
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val result = response.body()!!.string()
//                Log.d("getDataApi",result.toString())
//                try {
//                    val listType = object : TypeToken<MaskDataGson>() {}.type
//                    dataList = Gson().fromJson(result.toString(), listType)
//                    runOnUiThread {
//                        if (dataList != null) {
//                            showMarker("all")
//                            include_typechoose.visibility = View.VISIBLE
//                            update_btn.visibility = View.VISIBLE
//                        } else {
//                            progressBar.visibility = View.INVISIBLE
//                            AlertDialog.Builder(this@MainActivity)
//                                .setTitle("錯誤訊息")
//                                .setMessage("擷取資料失敗，請稍後再試！")
//                                .setCancelable(false)
//                                .setNegativeButton("確定") {d,w ->
//                                    finish()
//                                }
//                                .show()
//                        }
//                    }
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//            }
//        })
//    }
}