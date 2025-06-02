package ru.byteforge.xray_core.internal.service // Замени на свой пакет

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.byteforge.xray_core.api.VpnState

internal object CoreVpnServiceConstants {
    const val ACTION_START_VPN = "ru.byteforge.xray_core.ACTION_START_VPN"
    const val ACTION_STOP_VPN = "ru.byteforge.xray_core.ACTION_STOP_VPN"

    const val EXTRA_PROFILE_RAW_CONFIG = "ru.byteforge.xray_core.EXTRA_PROFILE_RAW_CONFIG"
    const val EXTRA_PROFILE_NAME = "ru.byteforge.xray_core.EXTRA_PROFILE_NAME"
    const val EXTRA_PROFILE_OBJECT = "ru.byteforge.xray_core.EXTRA_PROFILE_OBJECT_SERIALIZED"


    const val EXTRA_ASSETS_PATH = "ru.byteforge.xray_core.EXTRA_ASSETS_PATH"
    const val EXTRA_TUN2SOCKS_EXE_PATH = "ru.byteforge.xray_core.EXTRA_TUN2SOCKS_EXE_PATH"
    const val EXTRA_SOCKS_PROXY_ADDRESS = "ru.byteforge.xray_core.EXTRA_SOCKS_PROXY_ADDRESS"

    const val NOTIFICATION_CHANNEL_ID = "XrayVpnKitChannel"
    const val NOTIFICATION_ID = 1

    const val TUN_INTERFACE_ADDRESS = "10.1.10.1"
    const val TUN_INTERFACE_PREFIX_LENGTH = 24
    const val TUN_ROUTE_ADDRESS = "0.0.0.0"
    const val TUN_ROUTE_PREFIX_LENGTH = 0
    const val TUN_MTU = 1500
    const val DEFAULT_DNS_SERVER = "1.1.1.1"

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.IDLE)
    val vpnState = _vpnState.asStateFlow()

    fun updateState(newState: VpnState) {
        _vpnState.value = newState
        Log.d("CoreVpnService", "VPN State Updated: $newState")
    }
}