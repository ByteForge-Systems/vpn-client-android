package ru.byteforge.xray_core.api

sealed class VpnState {
    data object IDLE: VpnState()
    data object CONNECTING : VpnState()
    data class CONNECTED(val profile: Profile, val startTimeMs: Long): VpnState()
    data object STOPPING: VpnState()
    data class ERROR(val message: String, val cause: Throwable? = null): VpnState()
}