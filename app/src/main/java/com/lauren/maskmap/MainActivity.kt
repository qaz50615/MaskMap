package com.lauren.maskmap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lauren.MaskMap.MaskDataGson
import com.lauren.maskmap.remote.ApiClient
import com.lauren.maskmap.remote.ApiResult
import com.lauren.maskmap.remote.ApiService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.include_infodialog.*
import kotlinx.android.synthetic.main.include_typechoose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), LocationListener {

    private var mMap: GoogleMap? = null
    private var dataList: MaskDataGson? = null
    private var mLatitude = 24.000000
    private var mLongitude = 121.000000
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

    @SuppressLint("MissingPermission")
    private fun initView() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{
            mMap = it
            mMap?.uiSettings?.isMyLocationButtonEnabled = true
            mMap?.uiSettings?.isMapToolbarEnabled = false
            mMap?.uiSettings?.isZoomControlsEnabled = true
                val lastKnownLocation = locationManager.getLastKnownLocation(findBestProvider())
                mMap?.isMyLocationEnabled = true
                val lat = lastKnownLocation.latitude
                val long = lastKnownLocation.longitude
                mMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(lat, long), 16f)
                )
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
        if (location != null) {
            mLatitude = location.latitude
            mLongitude = location.longitude
            val myLocation = LatLng(mLatitude, mLongitude)
            Log.i("location", "$mLatitude$mLongitude")
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f))
        }
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun findBestProvider(): String? {
        return locationManager.getBestProvider(Criteria(), true) // 選擇精準度最高的提供者
    }

    private fun isGpsEnable(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun startGPS() {
        if (findBestProvider() != null) {
            val myLocation = locationManager.getLastKnownLocation(findBestProvider())
            updateLocation(myLocation)
            locationManager.requestLocationUpdates(findBestProvider(), 0, 20f, this)
        }
    }

//    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
//    private fun setCriteria(): Criteria {
//        val criteria = Criteria()                    //資訊提供者選取標準
//        criteria.accuracy = Criteria.ACCURACY_FINE  //设置定位精准度
//        criteria.isAltitudeRequired = false          //是否要求海拔
//        criteria.isBearingRequired = true           //是否要求方向
//        criteria.isCostAllowed = false               //是否要求收费
//        criteria.isSpeedRequired = true             //是否要求速度
//        criteria.powerRequirement = Criteria.POWER_LOW      //设置相对省电
//        criteria.bearingAccuracy = Criteria.ACCURACY_HIGH   //设置方向精确度
//        criteria.speedAccuracy = Criteria.ACCURACY_HIGH     //设置速度精确度
//        criteria.horizontalAccuracy = Criteria.ACCURACY_HIGH//设置水平方向精确度
//        criteria.verticalAccuracy = Criteria.ACCURACY_HIGH  //设置垂直方向精确
//        return criteria
//    }

    private fun locationProvider() {
        if (isGpsEnable()) {
            startGPS()
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
            progressBar.visibility = View.VISIBLE
            when (fetchData()) {
                is ApiResult.Success -> {
                    dataList = (fetchData() as ApiResult.Success).data
                    showAllMarker()
                    include_typechoose.visibility = View.VISIBLE
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