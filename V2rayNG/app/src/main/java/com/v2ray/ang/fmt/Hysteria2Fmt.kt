package com.v2ray.ang.fmt

import com.v2ray.ang.AppConfig.LOOPBACK
import com.v2ray.ang.dto.Hysteria2Bean
import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.util.Utils

object Hysteria2Fmt : FmtBase() {


    /**
     * Converts a ProfileItem object to a Hysteria2Bean object.
     *
     * @param config the ProfileItem object to convert
     * @param socksPort the port number for the socks5 proxy
     * @return the converted Hysteria2Bean object, or null if conversion fails
     */
    fun toNativeConfig(config: ProfileItem, socksPort: Int): Hysteria2Bean {

        val obfs = if (config.obfsPassword.isNullOrEmpty()) null else
            Hysteria2Bean.ObfsBean(
                type = "salamander",
                salamander = Hysteria2Bean.ObfsBean.SalamanderBean(
                    password = config.obfsPassword
                )
            )

        val transport = if (config.portHopping.isNullOrEmpty()) null else
            Hysteria2Bean.TransportBean(
                type = "udp",
                udp = Hysteria2Bean.TransportBean.TransportUdpBean(
                    hopInterval = (config.portHoppingInterval ?: "30") + "s"
                )
            )

        val bandwidth =
            if (config.bandwidthDown.isNullOrEmpty() || config.bandwidthUp.isNullOrEmpty()) null else
                Hysteria2Bean.BandwidthBean(
                    down = config.bandwidthDown,
                    up = config.bandwidthUp,
                )

        val server =
            if (config.portHopping.isNullOrEmpty())
                config.getServerAddressAndPort()
            else
                Utils.getIpv6Address(config.server) + ":" + config.portHopping

        val bean = Hysteria2Bean(
            server = server,
            auth = config.password,
            obfs = obfs,
            transport = transport,
            bandwidth = bandwidth,
            socks5 = Hysteria2Bean.Socks5Bean(
                listen = "$LOOPBACK:${socksPort}",
            ),
            http = Hysteria2Bean.Socks5Bean(
                listen = "$LOOPBACK:${socksPort}",
            ),
            tls = Hysteria2Bean.TlsBean(
                sni = config.sni ?: config.server,
                insecure = config.insecure,
                pinSHA256 = if (config.pinSHA256.isNullOrEmpty()) null else config.pinSHA256
            )
        )
        return bean
    }


}