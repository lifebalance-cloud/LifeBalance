package com.example.mylife.lifebalance.premium

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Управление премиум-статусом. Пока всегда false.
 * Позже подключить проверку покупки/подписки через Google Play Billing.
 */
object PremiumManager {
    private val _isPremium = MutableStateFlow(true) //false когда подключите проверку покупки
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun setPremium(value: Boolean) {
        _isPremium.value = value
    }
}
