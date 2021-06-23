package com.lauren.maskmap.remote

import com.lauren.MaskMap.MaskDataGson
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("kiang/pharmacies/master/json/points.json")
    suspend fun getData (): MaskDataGson
}