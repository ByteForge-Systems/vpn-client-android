package ru.byteforge.xrayvpnclient

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.byteforge.xray_core.XrayVpnKit
import ru.byteforge.xray_core.api.VpnState
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ConnectionViewModel(application: Application) : AndroidViewModel(application)  {
    private val xrayVpnKit = XrayVpnKit.getInstance()

    private val context = getApplication<Application>()

    var isConnected by mutableStateOf(false)
        private set
    
    var isConnecting by mutableStateOf(false)
        private set
    
    var connectionTime by mutableStateOf(0L)
        private set
    
    var downloadSpeed by mutableStateOf(0.0)
        private set
    
    var uploadSpeed by mutableStateOf(0.0)
        private set
    
    var selectedServer by mutableStateOf(
        ServerInfo(
            country = "Finland",
            city = "Helsinki",
            flag = "flag_fl",
            pingMs = 54
        )
    )
        private set
    
    fun toggleConnection() {
        val currentKitState = xrayVpnKit.observeVpnState().value
        if (currentKitState is VpnState.CONNECTED) {
            xrayVpnKit.stopVpn()
        } else if (currentKitState == VpnState.IDLE || currentKitState is VpnState.ERROR) {
            val intent = android.net.VpnService.prepare(getApplication())
            if (intent != null) {
                permissionLauncher.launch(intent)
            } else {
                xrayVpnKit.startVpn()
            }
        }
    }
    
    private fun startTimer() {
        connectionTime = 0
        viewModelScope.launch {
            while (isConnected) {
                delay(1000)
                connectionTime += 1
            }
        }
    }
    
    private fun startSpeedUpdates() {
        viewModelScope.launch {
            while (isConnected) {
                downloadSpeed = 25.0 + Random.nextDouble(-5.0, 10.0)
                uploadSpeed = 15.0 + Random.nextDouble(-3.0, 8.0)
                delay(3000)
            }
        }
    }
    
    fun formatTime(seconds: Long): String {
        val hours = TimeUnit.SECONDS.toHours(seconds)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val secs = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}

data class ServerInfo(
    val country: String,
    val city: String,
    val flag: String,
    val pingMs: Int
)