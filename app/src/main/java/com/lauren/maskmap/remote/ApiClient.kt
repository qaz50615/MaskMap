package com.lauren.MaskMap.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    private val retrofit: Retrofit = createRetrofit()

    private fun createRetrofit() = Retrofit
        .Builder()
        .baseUrl("https://raw.githubusercontent.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T:Any> createService(_class:Class<T>):T {
        return retrofit.create(_class)
    }
}