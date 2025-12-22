package com.v2ray.ang.dto

import com.v2ray.ang.AppConfig.TAG_BLOCKED
import com.v2ray.ang.AppConfig.TAG_DIRECT
import com.v2ray.ang.AppConfig.TAG_PROXY

data class ServerConfig(
    val configVersion: Int = 3,
    val configType: EConfigType,
    var subscriptionId: String = "",
    val addedTime: Long = System.currentTimeMillis(),
    var remarks: String = "",
    val outboundBean: V2rayConfig.OutboundBean? = null,
    var fullConfig: V2rayConfig? = null
) {
    companion object {
        fun create(configType: EConfigType): ServerConfig {
            when (configType) {


                EConfigType.CUSTOM ->
                    return ServerConfig(configType = configType)

                EConfigType.HYSTERIA2 ->
                    return ServerConfig(
                        configType = configType,
                        outboundBean = V2rayConfig.OutboundBean(
                            protocol = configType.name.lowercase(),
                            settings = V2rayConfig.OutboundBean.OutSettingsBean(
                                servers = listOf(V2rayConfig.OutboundBean.OutSettingsBean.ServersBean())
                            ),
                            streamSettings = V2rayConfig.OutboundBean.StreamSettingsBean()
                        )
                    )


            }
        }
    }

    fun getProxyOutbound(): V2rayConfig.OutboundBean? {
        if (configType != EConfigType.CUSTOM) {
            return outboundBean
        }
        return fullConfig?.getProxyOutbound()
    }

    fun getAllOutboundTags(): MutableList<String> {
        if (configType != EConfigType.CUSTOM) {
            return mutableListOf(TAG_PROXY, TAG_DIRECT, TAG_BLOCKED)
        }
        fullConfig?.let { config ->
            return config.outbounds.map { it.tag }.toMutableList()
        }
        return mutableListOf()
    }
}
