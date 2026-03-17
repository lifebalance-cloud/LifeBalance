package com.example.mylife.lifebalance.data

data class AiAnalyzeRequest(
    val text: String,
    val language: String,
    val maxTokens: Int? = null
)