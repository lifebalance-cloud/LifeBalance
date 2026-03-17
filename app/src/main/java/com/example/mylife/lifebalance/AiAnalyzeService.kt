package com.example.mylife.lifebalance

import com.example.mylife.lifebalance.data.AiAnalyzeRequest
import com.example.mylife.lifebalance.data.AiAnalyzeResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AiAnalyzeService {

    @POST("analyzeBalance") // <-- путь относительно BASE_URL
    suspend fun analyzeBalance(
        @Body request: AiAnalyzeRequest
    ): AiAnalyzeResponse
}
