package com.v2ray.ang.dto

import com.v2ray.ang.AppConfig


enum class EConfigType(val value: Int, val protocolScheme: String) {
    CUSTOM(2, AppConfig.CUSTOM),
    HYSTERIA2(9, AppConfig.HYSTERIA2);

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value }
    }
}
