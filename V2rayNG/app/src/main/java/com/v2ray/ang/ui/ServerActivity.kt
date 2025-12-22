package com.v2ray.ang.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.DEFAULT_PORT
import com.v2ray.ang.AppConfig.PREF_ALLOW_INSECURE
import com.v2ray.ang.AppConfig.TLS
import com.v2ray.ang.R
import com.v2ray.ang.dto.EConfigType
import com.v2ray.ang.dto.ProfileItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.Utils

class ServerActivity : BaseActivity() {

    private val editGuid by lazy { intent.getStringExtra("guid").orEmpty() }
    private val isRunning by lazy {
        intent.getBooleanExtra("isRunning", false)
                && editGuid.isNotEmpty()
                && editGuid == MmkvManager.getSelectServer()
    }
    private val createConfigType by lazy {
        EConfigType.fromInt(intent.getIntExtra("createConfigType", EConfigType.HYSTERIA2.value))
            ?: EConfigType.HYSTERIA2
    }
    private val subscriptionId by lazy {
        intent.getStringExtra("subscriptionId")
    }

    private val securitys: Array<out String> by lazy {
        resources.getStringArray(R.array.securitys)
    }
    private val shadowsocksSecuritys: Array<out String> by lazy {
        resources.getStringArray(R.array.ss_securitys)
    }
    private val flows: Array<out String> by lazy {
        resources.getStringArray(R.array.flows)
    }
    private val networks: Array<out String> by lazy {
        resources.getStringArray(R.array.networks)
    }
    private val tcpTypes: Array<out String> by lazy {
        resources.getStringArray(R.array.header_type_tcp)
    }
    private val kcpAndQuicTypes: Array<out String> by lazy {
        resources.getStringArray(R.array.header_type_kcp_and_quic)
    }
    private val grpcModes: Array<out String> by lazy {
        resources.getStringArray(R.array.mode_type_grpc)
    }
    private val streamSecuritys: Array<out String> by lazy {
        resources.getStringArray(R.array.streamsecurityxs)
    }
    private val allowinsecures: Array<out String> by lazy {
        resources.getStringArray(R.array.allowinsecures)
    }
    private val uTlsItems: Array<out String> by lazy {
        resources.getStringArray(R.array.streamsecurity_utls)
    }
    private val alpns: Array<out String> by lazy {
        resources.getStringArray(R.array.streamsecurity_alpn)
    }
    private val xhttpMode: Array<out String> by lazy {
        resources.getStringArray(R.array.xhttp_mode)
    }


    // Kotlin synthetics was used, but since it is removed in 1.8. We switch to old manual approach.
    // We don't use AndroidViewBinding because, it is better to share similar logics for different
    // protocols. Use findViewById manually ensures the xml are de-coupled with the activity logic.
    private val et_remarks: EditText by lazy { findViewById(R.id.et_remarks) }
    private val et_address: EditText by lazy { findViewById(R.id.et_address) }
    private val et_port: EditText by lazy { findViewById(R.id.et_port) }
    private val et_id: EditText by lazy { findViewById(R.id.et_id) }

    private val sp_stream_security: Spinner? by lazy { findViewById(R.id.sp_stream_security) }
    private val sp_allow_insecure: Spinner? by lazy { findViewById(R.id.sp_allow_insecure) }
    private val container_allow_insecure: LinearLayout? by lazy { findViewById(R.id.lay_allow_insecure) }
    private val et_sni: EditText? by lazy { findViewById(R.id.et_sni) }
    private val container_sni: LinearLayout? by lazy { findViewById(R.id.lay_sni) }
    private val sp_stream_fingerprint: Spinner? by lazy { findViewById(R.id.sp_stream_fingerprint) } //uTLS

    private val et_obfs_password: EditText? by lazy { findViewById(R.id.et_obfs_password) }
    private val et_port_hop: EditText? by lazy { findViewById(R.id.et_port_hop) }
    private val et_port_hop_interval: EditText? by lazy { findViewById(R.id.et_port_hop_interval) }
    private val et_pinsha256: EditText? by lazy { findViewById(R.id.et_pinsha256) }
    private val et_bandwidth_down: EditText? by lazy { findViewById(R.id.et_bandwidth_down) }
    private val et_bandwidth_up: EditText? by lazy { findViewById(R.id.et_bandwidth_up) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_server)

        val config = MmkvManager.decodeServerConfig(editGuid)
        when (config?.configType ?: createConfigType) {
            EConfigType.CUSTOM -> return
            EConfigType.HYSTERIA2 -> setContentView(R.layout.activity_server_hysteria2)
        }

        sp_stream_security?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val isBlank = streamSecuritys[position].isBlank()
                val isTLS = streamSecuritys[position] == TLS

                when {
                    // Case 1: Null or blank
                    isBlank -> {
                        listOf(
                            container_sni,
                            container_allow_insecure
                        ).forEach { it?.visibility = View.GONE }
                    }

                    // Case 2: TLS value
                    isTLS -> {
                        listOf(
                            container_sni
                        ).forEach { it?.visibility = View.VISIBLE }
                        container_allow_insecure?.visibility = View.VISIBLE
                    }

                    // Case 3: Other reality values
                    else -> {
                        listOf(container_sni).forEach {
                            it?.visibility = View.VISIBLE
                        }
                        container_allow_insecure?.visibility = View.GONE

                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // do nothing
            }
        }
        if (config != null) {
            bindingServer(config)
        } else {
            clearServer()
        }
    }

    /**
     * binding selected server config
     */
    private fun bindingServer(config: ProfileItem): Boolean {

        et_remarks.text = Utils.getEditable(config.remarks)
        et_address.text = Utils.getEditable(config.server.orEmpty())
        et_port.text = Utils.getEditable(config.serverPort ?: DEFAULT_PORT.toString())
        et_id.text = Utils.getEditable(config.password.orEmpty())

        if (config.configType == EConfigType.HYSTERIA2) {
            et_obfs_password?.text = Utils.getEditable(config.obfsPassword)
            et_port_hop?.text = Utils.getEditable(config.portHopping)
            et_port_hop_interval?.text = Utils.getEditable(config.portHoppingInterval)
            et_pinsha256?.text = Utils.getEditable(config.pinSHA256)
            et_bandwidth_down?.text = Utils.getEditable(config.bandwidthDown)
            et_bandwidth_up?.text = Utils.getEditable(config.bandwidthUp)
        }



        if (config.security.isNullOrEmpty()) {
            listOf(
                container_sni,
                container_allow_insecure
            ).forEach { it?.visibility = View.GONE }
        }


        return true
    }

    /**
     * clear or init server config
     */
    private fun clearServer(): Boolean {
        et_remarks.text = null
        et_address.text = null
        et_port.text = Utils.getEditable(DEFAULT_PORT.toString())
        et_id.text = null


        sp_stream_security?.setSelection(0)
        sp_allow_insecure?.setSelection(0)
        et_sni?.text = null

        return true
    }

    /**
     * save server config
     */
    private fun saveServer(): Boolean {
        if (TextUtils.isEmpty(et_remarks.text.toString())) {
            toast(R.string.server_lab_remarks)
            return false
        }
        if (TextUtils.isEmpty(et_address.text.toString())) {
            toast(R.string.server_lab_address)
            return false
        }
        if (createConfigType != EConfigType.HYSTERIA2) {
            if (Utils.parseInt(et_port.text.toString()) <= 0) {
                toast(R.string.server_lab_port)
                return false
            }
        }
        val config =
            MmkvManager.decodeServerConfig(editGuid) ?: ProfileItem.create(createConfigType)
        if (TextUtils.isEmpty(et_id.text.toString())) {
            if (config.configType == EConfigType.HYSTERIA2) {
                toast(R.string.server_lab_id3)
            } else {
                toast(R.string.server_lab_id)
            }
            return false
        }


        saveCommon(config)
        saveTls(config)

        if (config.subscriptionId.isEmpty() && !subscriptionId.isNullOrEmpty()) {
            config.subscriptionId = subscriptionId.orEmpty()
        }
        //Log.i(AppConfig.TAG, JsonUtil.toJsonPretty(config) ?: "")
        MmkvManager.encodeServerConfig(editGuid, config)
        toastSuccess(R.string.toast_success)
        finish()
        return true
    }

    private fun saveCommon(config: ProfileItem) {
        config.remarks = et_remarks.text.toString().trim()
        config.server = et_address.text.toString().trim()
        config.serverPort = et_port.text.toString().trim()
        config.password = et_id.text.toString().trim()

        if (config.configType == EConfigType.HYSTERIA2) {
            config.obfsPassword = et_obfs_password?.text?.toString()
            config.portHopping = et_port_hop?.text?.toString()
            config.portHoppingInterval = et_port_hop_interval?.text?.toString()
            config.pinSHA256 = et_pinsha256?.text?.toString()
            config.bandwidthDown = et_bandwidth_down?.text?.toString()
            config.bandwidthUp = et_bandwidth_up?.text?.toString()
        }
    }


    private fun saveTls(config: ProfileItem) {
        val streamSecurity = sp_stream_security?.selectedItemPosition ?: return
        val sniField = et_sni?.text?.toString()?.trim()
        val allowInsecureField = sp_allow_insecure?.selectedItemPosition
        val utlsIndex = sp_stream_fingerprint?.selectedItemPosition ?: 0


        val allowInsecure =
            if (allowInsecureField == null || allowinsecures[allowInsecureField].isBlank()) {
                MmkvManager.decodeSettingsBool(PREF_ALLOW_INSECURE)
            } else {
                allowinsecures[allowInsecureField].toBoolean()
            }

        config.security = streamSecuritys[streamSecurity]
        config.insecure = allowInsecure
        config.sni = sniField
        config.fingerPrint = uTlsItems[utlsIndex]

    }


    /**
     * delete server config
     */
    private fun deleteServer(): Boolean {
        if (editGuid.isNotEmpty()) {
            if (editGuid != MmkvManager.getSelectServer()) {
                if (MmkvManager.decodeSettingsBool(AppConfig.PREF_CONFIRM_REMOVE) == true) {
                    AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            MmkvManager.removeServer(editGuid)
                            finish()
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            // do nothing
                        }
                        .show()
                } else {
                    MmkvManager.removeServer(editGuid)
                    finish()
                }
            } else {
                application.toast(R.string.toast_action_not_allowed)
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        val delButton = menu.findItem(R.id.del_config)
        val saveButton = menu.findItem(R.id.save_config)

        if (editGuid.isNotEmpty()) {
            if (isRunning) {
                delButton?.isVisible = false
                saveButton?.isVisible = false
            }
        } else {
            delButton?.isVisible = false
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.del_config -> {
            deleteServer()
            true
        }

        R.id.save_config -> {
            saveServer()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}
