package com.example.mylife.lifebalance

import com.example.mylife.lifebalance.data.AnalyzeRequest
import com.example.mylife.lifebalance.data.AnalyzeResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface FirebaseFunctionService {
    @POST("analyzeBalance")
    fun analyzeBalance(@Body request: AnalyzeRequest): Call<AnalyzeResponse>
}

object RetrofitInstance {
    val service: FirebaseFunctionService by lazy {
        Retrofit.Builder()
            .baseUrl("https://us-central1-life-balance-fd152.cloudfunctions.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FirebaseFunctionService::class.java)
    }
}
