package ru.byteforge.xray_core.internal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.byteforge.xray_core.api.Profile
import ru.byteforge.xray_core.api.VpnState
import ru.byteforge.xray_core.internal.core.Tun2SocksProxy
import ru.byteforge.xray_core.internal.core.Tun2SocksProxyImpl
import ru.byteforge.xray_core.internal.core.XrayCoreProxy
import ru.byteforge.xray_core.internal.core.XrayCoreProxyImpl

internal class CoreVpnService : VpnService() {

    companion object {
        private const val TAG = "CORE_VPN_SERVICE"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var tunInterface: ParcelFileDescriptor? = null
    private var currentProfile: Profile? = null
    private var connectionStartTime: Long = 0L

    private lateinit var xrayCoreProxy: XrayCoreProxy
    private lateinit var tun2SocksProxy: Tun2SocksProxy

    private var assetsPath: String? = null
    private var tun2socksExecutablePath: String? = null
    private var socksProxyAddress: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        xrayCoreProxy = XrayCoreProxyImpl()
        tun2SocksProxy = Tun2SocksProxyImpl()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, action: ${intent?.action}")
        when (intent?.action) {
            CoreVpnServiceConstants.ACTION_START_VPN -> {
                val profileJson = intent.getStringExtra(CoreVpnServiceConstants.EXTRA_PROFILE_OBJECT)
                val profileName = intent.getStringExtra(CoreVpnServiceConstants.EXTRA_PROFILE_NAME) ?: "Unknown Profile"
                val rawConfig = intent.getStringExtra(CoreVpnServiceConstants.EXTRA_PROFILE_RAW_CONFIG)

                assetsPath = intent.getStringExtra(CoreVpnServiceConstants.EXTRA_ASSETS_PATH)
                tun2socksExecutablePath = intent.getStringExtra(CoreVpnServiceConstants.EXTRA_TUN2SOCKS_EXE_PATH)
                socksProxyAddress = intent.getStringExtra(CoreVpnServiceConstants.EXTRA_SOCKS_PROXY_ADDRESS)

                if (profileJson != null) {
                    currentProfile = Gson().fromJson(profileJson, Profile::class.java)
                } else if (rawConfig != null && currentProfile == null) {
                    currentProfile = Profile(id = "temp_id_${System.currentTimeMillis()}", name = profileName, rawConfig = rawConfig)
                }
                assetsPath = applicationContext.filesDir.absolutePath

                tun2socksExecutablePath = "${applicationContext.filesDir.absolutePath}/tun2socks"
                socksProxyAddress = "127.0.0.1:10808"

                currentProfile = Profile(
                    id = "1233213",
                    name = "hardcode",
                    rawConfig = """
                                    {
  "dns": {
    "hosts": {
      "domain:googleapis.cn": "googleapis.com",
      "dns.alidns.com": [
        "223.5.5.5",
        "223.6.6.6",
        "2400:3200::1",
        "2400:3200:baba::1"
      ],
      "one.one.one.one": [
        "1.1.1.1",
        "1.0.0.1",
        "2606:4700:4700::1111",
        "2606:4700:4700::1001"
      ],
      "dot.pub": [
        "1.12.12.12",
        "120.53.53.53"
      ],
      "dns.google": [
        "8.8.8.8",
        "8.8.4.4",
        "2001:4860:4860::8888",
        "2001:4860:4860::8844"
      ],
      "dns.quad9.net": [
        "9.9.9.9",
        "149.112.112.112",
        "2620:fe::fe",
        "2620:fe::9"
      ],
      "common.dot.dns.yandex.net": [
        "77.88.8.8",
        "77.88.8.1",
        "2a02:6b8::feed:0ff",
        "2a02:6b8:0:1::feed:0ff"
      ]
    },
    "servers": [
      "1.1.1.1",
      {
        "address": "1.1.1.1",
        "domains": [
          "domain:googleapis.cn",
          "domain:gstatic.com"
        ]
      },
      {
        "address": "223.5.5.5",
        "domains": [
          "domain:alidns.com",
          "domain:doh.pub",
          "domain:dot.pub",
          "domain:360.cn",
          "domain:onedns.net",
          "geosite:cn"
        ],
        "expectIPs": [
          "geoip:cn"
        ],
        "skipFallback": true
      }
    ]
  },
  "inbounds": [
    {
      "listen": "127.0.0.1",
      "port": 10808,
      "protocol": "socks",
      "settings": {
        "auth": "noauth",
        "udp": true,
        "userLevel": 8
      },
      "sniffing": {
        "destOverride": [
          "http",
          "tls"
        ],
        "enabled": true,
        "routeOnly": false
      },
      "tag": "socks"
    }
  ],
  "log": {
    "loglevel": "warning"
  },
  "outbounds": [
    {
      "mux": {
        "concurrency": -1,
        "enabled": false
      },
      "protocol": "vless",
      "settings": {
        "vnext": [
          {
            "address": "164.215.97.44",
            "port": 443,
            "users": [
              {
                "encryption": "none",
                "flow": "xtls-rprx-vision-udp443",
                "id": "goyda",
                "level": 8
              }
            ]
          }
        ]
      },
      "streamSettings": {
        "network": "tcp",
        "realitySettings": {
          "allowInsecure": false,
          "fingerprint": "chrome",
          "publicKey": "JtrfBphftmhxOE2r4g6jiWob0B3jfxx5mITJALd8-k8",
          "serverName": "www.cloudflare.com",
          "show": false
        },
        "security": "reality",
        "tcpSettings": {
          "header": {
            "type": "none"
          }
        }
      },
      "tag": "proxy"
    },
    {
      "protocol": "freedom",
      "settings": {
        "domainStrategy": "UseIP"
      },
      "tag": "direct"
    },
    {
      "protocol": "blackhole",
      "settings": {
        "response": {
          "type": "http"
        }
      },
      "tag": "block"
    }
  ],
  "remarks": "XrayVPN",
  "routing": {
      }
  }
                    """.trimIndent()
                )

                if (currentProfile?.rawConfig == null || assetsPath == null || tun2socksExecutablePath == null || socksProxyAddress == null) {
                    Log.e(TAG, "Missing required parameters to start VPN.")
                    Log.d(TAG, "$currentProfile \n $assetsPath \n $tun2socksExecutablePath \n $socksProxyAddress")
                    CoreVpnServiceConstants.updateState(VpnState.ERROR("Missing start parameters", null))
                    stopSelf()
                    return START_NOT_STICKY
                }

                serviceScope.launch {
                    startVpnConnection(currentProfile!!.rawConfig, currentProfile!!.name)
                }
            }
            CoreVpnServiceConstants.ACTION_STOP_VPN -> {
                serviceScope.launch {
                    stopVpnConnection()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startVpnConnection(rawConfig: String, profileName: String) {
        if (CoreVpnServiceConstants.vpnState.value is VpnState.CONNECTED || CoreVpnServiceConstants.vpnState.value is VpnState.CONNECTING) {
            Log.w(TAG, "VPN is already connected or connecting.")
            return
        }

        CoreVpnServiceConstants.updateState(VpnState.CONNECTING)
        connectionStartTime = System.currentTimeMillis()

        Log.d(TAG, "Starting Xray core...")
        val xrayRunResult = xrayCoreProxy.runXray(rawConfig)
        if (xrayRunResult != 0) {
            Log.e(TAG, "Failed to start Xray core, result: $xrayRunResult")
            CoreVpnServiceConstants.updateState(VpnState.ERROR("Failed to start Xray core (code: $xrayRunResult)", null))
            return
        }
        Log.i(TAG, "Xray core started successfully.")

        Log.d(TAG, "Establishing TUN interface...")
        try {
            val builder = Builder()
            builder.setSession(profileName)
            builder.setMtu(CoreVpnServiceConstants.TUN_MTU)
            builder.addAddress(CoreVpnServiceConstants.TUN_INTERFACE_ADDRESS, CoreVpnServiceConstants.TUN_INTERFACE_PREFIX_LENGTH)
            builder.addRoute(CoreVpnServiceConstants.TUN_ROUTE_ADDRESS, CoreVpnServiceConstants.TUN_ROUTE_PREFIX_LENGTH)
            builder.addDnsServer(CoreVpnServiceConstants.DEFAULT_DNS_SERVER)


            tunInterface = builder.establish()
            if (tunInterface == null) {
                Log.e(TAG, "Failed to establish TUN interface. VPN permission might be missing.")
                CoreVpnServiceConstants.updateState(VpnState.ERROR("Failed to establish TUN (permission?)", null))
                xrayCoreProxy.stopXray()
                return
            }
            Log.i(TAG, "TUN interface established.")

            Log.d(TAG, "Starting tun2socks...")
            val tun2socksStarted = tun2SocksProxy.startTun2Socks(
                tunFd = tunInterface!!,
                socksProxyAddress = socksProxyAddress!!,
                tunAddress = CoreVpnServiceConstants.TUN_INTERFACE_ADDRESS,
                tunPrefixLength = CoreVpnServiceConstants.TUN_INTERFACE_PREFIX_LENGTH,
                tunDns = CoreVpnServiceConstants.DEFAULT_DNS_SERVER,
                tunMtu = CoreVpnServiceConstants.TUN_MTU,
                tun2socksExecutablePath = tun2socksExecutablePath!!
            )

            if (!tun2socksStarted || !tun2SocksProxy.isRunning()) {
                Log.e(TAG, "Failed to start tun2socks.")
                CoreVpnServiceConstants.updateState(VpnState.ERROR("Failed to start tun2socks", null))
                tunInterface?.close()
                tunInterface = null
                xrayCoreProxy.stopXray()
                return
            }
            Log.i(TAG, "tun2socks started successfully.")

            if (currentProfile != null) {
                CoreVpnServiceConstants.updateState(VpnState.CONNECTED(currentProfile!!, connectionStartTime))
            } else {
                CoreVpnServiceConstants.updateState(VpnState.ERROR("Internal: Profile missing for CONNECTED state", null))
            }
            startForeground(CoreVpnServiceConstants.NOTIFICATION_ID, createNotification("Connected to $profileName"))
            Log.i(TAG, "VPN connection established successfully with profile: $profileName")

        } catch (e: Exception) {
            Log.e(TAG, "Exception during VPN start", e)
            CoreVpnServiceConstants.updateState(VpnState.ERROR("Exception: ${e.message}", e))
            tunInterface?.close()
            tunInterface = null
            tun2SocksProxy.stopTun2Socks()
            xrayCoreProxy.stopXray()
        }
    }

    private suspend fun stopVpnConnection() {
        Log.d(TAG, "Stopping VPN connection...")
        val currentState = CoreVpnServiceConstants.vpnState.value
        if (currentState is VpnState.IDLE || currentState is VpnState.STOPPING) {
            Log.w(TAG, "VPN is already stopped or stopping.")
            return
        }
        CoreVpnServiceConstants.updateState(VpnState.STOPPING)

        stopForeground(true)

        Log.d(TAG, "Stopping tun2socks...")
        tun2SocksProxy.stopTun2Socks()
        Log.i(TAG, "tun2socks stopped.")

        Log.d(TAG, "Closing TUN interface...")
        try {
            tunInterface?.close()
            tunInterface = null
            Log.i(TAG, "TUN interface closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TUN interface", e)
        }

        Log.d(TAG, "Stopping Xray core...")
        xrayCoreProxy.stopXray()
        Log.i(TAG, "Xray core stopped.")

        CoreVpnServiceConstants.updateState(VpnState.IDLE)
        currentProfile = null
        Log.i(TAG, "VPN connection stopped successfully.")
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Xray VPN Service"
            val descriptionText = "Notification channel for Xray VPN service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CoreVpnServiceConstants.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val stopIntent = Intent(this, CoreVpnService::class.java).apply {
            action = CoreVpnServiceConstants.ACTION_STOP_VPN
        }
        val stopPendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CoreVpnServiceConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("XrayVpnKit")
            .setContentText(contentText)
            .setSmallIcon(androidx.constraintlayout.widget.R.drawable.abc_ic_star_black_48dp)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "Stop VPN", stopPendingIntent)
            .build()
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN permission revoked by user or system.")
        serviceScope.launch {
            if (xrayCoreProxy.isCoreRunning() || tun2SocksProxy.isRunning() || tunInterface != null) {
                CoreVpnServiceConstants.updateState(VpnState.ERROR("VPN permission revoked", null))
            }
            stopVpnConnection()
        }
        super.onRevoke()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        // Убедимся, что все остановлено, если сервис уничтожается неожиданно
        // (хотя stopVpnConnection должен вызываться и приводить к stopSelf)
        if (CoreVpnServiceConstants.vpnState.value !is VpnState.IDLE) {
            Log.w(TAG, "Service destroyed while VPN was not IDLE. Forcing cleanup.")
            // Запускаем в GlobalScope, так как serviceScope может быть уже отменен
            GlobalScope.launch(Dispatchers.IO) {
                stopVpnConnection()
            }
        }
        serviceJob.cancel()
        super.onDestroy()
    }

}