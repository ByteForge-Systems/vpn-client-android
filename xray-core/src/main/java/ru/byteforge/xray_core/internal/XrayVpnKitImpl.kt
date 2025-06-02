package ru.byteforge.xray_core.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.byteforge.xray_core.XrayVpnKit
import ru.byteforge.xray_core.api.Profile
import ru.byteforge.xray_core.api.VpnState
import ru.byteforge.xray_core.internal.core.XrayCoreProxy
import ru.byteforge.xray_core.internal.core.XrayCoreProxyImpl
import ru.byteforge.xray_core.internal.service.CoreVpnService
import ru.byteforge.xray_core.internal.storage.ProfileStorage
import ru.byteforge.xray_core.internal.storage.ProfileStorageImpl
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile

object VpnKitConstants {
    const val ACTION_VPN_STATE_CHANGED = "ru.byteforge.xray_core.VPN_STATE_CHANGED"

    const val EXTRA_VPN_STATE_TYPE_STRING = "vpn_state_type_string"

    const val STATE_NAME_IDLE = "IDLE"
    const val STATE_NAME_CONNECTING = "CONNECTING"
    const val STATE_NAME_CONNECTED = "CONNECTED"
    const val STATE_NAME_STOPPING = "STOPPING"
    const val STATE_NAME_ERROR = "ERROR"

    const val EXTRA_VPN_STATE_PROFILE_JSON = "vpn_state_profile_json" // Для CONNECTED
    const val EXTRA_VPN_STATE_START_TIME = "vpn_state_start_time"   // Для CONNECTED
    const val EXTRA_VPN_STATE_ERROR_MESSAGE = "vpn_state_error_message" // Для ERROR
    const val EXTRA_VPN_STATE_ERROR_CAUSE_STRING = "vpn_state_error_cause_string" // Для ERROR

    const val ACTION_START_VPN = "ru.byteforge.xray_core.ACTION_START_VPN"
    const val ACTION_STOP_VPN = "ru.byteforge.xray_core.ACTION_STOP_VPN"

    const val EXTRA_PROFILE_ID = "profile_id_to_start"
    const val EXTRA_TUN2SOCKS_PATH = "tun2socks_executable_path"

    const val GEOIP_FILENAME = "geoip.dat"
    const val GEOSITE_FILENAME = "geosite.dat"
    const val XRAY_ASSETS_DIR_NAME = "xray_assets"
    const val TUN2SOCKS_BIN_NAME = "libtun2socks.so"
}


internal class XrayVpnKitImpl private constructor() : XrayVpnKit {

    private lateinit var appContext: Context
    private lateinit var profileStorage: ProfileStorage
    private lateinit var xrayCoreProxy: XrayCoreProxy

    private val _vpnStateFlow = MutableStateFlow<VpnState>(VpnState.IDLE)
    override fun observeVpnState(): StateFlow<VpnState> = _vpnStateFlow.asStateFlow()

    private var isKitInitialized = false
    private var preparedTun2SocksPath: String? = null
    private var preparedXrayAssetsPath: String? = null

    private val vpnStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == VpnKitConstants.ACTION_VPN_STATE_CHANGED) {
                val stateTypeString = intent.getStringExtra(VpnKitConstants.EXTRA_VPN_STATE_TYPE_STRING)
                Log.d(TAG, "Received VPN state update broadcast. Type String: $stateTypeString")

                val newState: VpnState? = when (stateTypeString) {
                    VpnKitConstants.STATE_NAME_IDLE -> VpnState.IDLE
                    VpnKitConstants.STATE_NAME_CONNECTING -> VpnState.CONNECTING
                    VpnKitConstants.STATE_NAME_STOPPING -> VpnState.STOPPING
                    VpnKitConstants.STATE_NAME_CONNECTED -> {
                        val profileJson = intent.getStringExtra(VpnKitConstants.EXTRA_VPN_STATE_PROFILE_JSON)
                        val startTime = intent.getLongExtra(VpnKitConstants.EXTRA_VPN_STATE_START_TIME, 0L)
                        profileJson?.let {
                            Log.w(TAG, "CONNECTED state needs Profile deserialization. ProfileJson: $profileJson, StartTime: $startTime")
                            getSelectedVpnProfile()?.let { currentProfile -> VpnState.CONNECTED(currentProfile, startTime) }
                                ?: VpnState.ERROR("Connected state received but no profile found for deserialization fallback", null)
                        }
                    }
                    VpnKitConstants.STATE_NAME_ERROR -> {
                        val message = intent.getStringExtra(VpnKitConstants.EXTRA_VPN_STATE_ERROR_MESSAGE) ?: "Unknown error"
                        val causeString = intent.getStringExtra(VpnKitConstants.EXTRA_VPN_STATE_ERROR_CAUSE_STRING)
                        Log.e(TAG, "Received ERROR state: $message, Cause: $causeString")
                        VpnState.ERROR(message, causeString?.let { Throwable(it) })
                    }
                    else -> {
                        Log.w(TAG, "Unknown VPN state type string received: $stateTypeString")
                        null
                    }
                }

                newState?.let {
                    Log.i(TAG, "Updating VpnStateFlow to: $it")
                    _vpnStateFlow.value = it
                }
            }
        }
    }


    override fun initialize(context: Context) {
        if (isKitInitialized) {
            Log.d(TAG, "XrayVpnKit already initialized.")
            return
        }
        Log.d(TAG, "Initializing XrayVpnKit...")
        this.appContext = context.applicationContext

        profileStorage = ProfileStorageImpl(this.appContext)
        xrayCoreProxy = XrayCoreProxyImpl()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val xrayAssetsDir = File(this@XrayVpnKitImpl.appContext.filesDir, VpnKitConstants.XRAY_ASSETS_DIR_NAME)
                if (!xrayAssetsDir.exists()) {
                    xrayAssetsDir.mkdirs()
                }
                preparedXrayAssetsPath = xrayAssetsDir.absolutePath
                Log.d(TAG, "Xray assets path: $preparedXrayAssetsPath")

                val geoipResId = this@XrayVpnKitImpl.appContext.resources.getIdentifier("geoip", "raw", this@XrayVpnKitImpl.appContext.packageName)
                val geositeResId = this@XrayVpnKitImpl.appContext.resources.getIdentifier("geosite", "raw", this@XrayVpnKitImpl.appContext.packageName)

                if (geoipResId != 0) {
                    copyRawResourceToFile(geoipResId, File(xrayAssetsDir, VpnKitConstants.GEOIP_FILENAME))
                } else {
                    Log.e(TAG, "GeoIP resource (geoip.dat) not found! VPN may not work correctly.")
                }
                if (geositeResId != 0) {
                    copyRawResourceToFile(geositeResId, File(xrayAssetsDir, VpnKitConstants.GEOSITE_FILENAME))
                } else {
                    Log.e(TAG, "GeoSite resource (geosite.dat) not found! VPN may not work correctly.")
                }


                preparedTun2SocksPath = copyTun2SocksExecutable()
                if (preparedTun2SocksPath == null) {
                    throw IOException("Failed to prepare tun2socks executable.")
                }
                Log.d(TAG, "Tun2socks executable path: $preparedTun2SocksPath")

                val coreProxyInitialized = xrayCoreProxy.initialize(this@XrayVpnKitImpl.appContext, preparedXrayAssetsPath!!)
                if (!coreProxyInitialized) {
                    throw RuntimeException("Failed to initialize XrayCoreProxy.")
                }

                val intentFilter = IntentFilter(VpnKitConstants.ACTION_VPN_STATE_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    this@XrayVpnKitImpl.appContext.registerReceiver(vpnStateReceiver, intentFilter, Context.RECEIVER_EXPORTED)
                } else {
                    this@XrayVpnKitImpl.appContext.registerReceiver(vpnStateReceiver, intentFilter)
                }
                Log.d(TAG, "VPN state receiver registered.")

                isKitInitialized = true
                _vpnStateFlow.value = VpnState.IDLE
                Log.i(TAG, "XrayVpnKit initialized successfully.")

            } catch (e: Exception) {
                Log.e(TAG, "Error during XrayVpnKit initialization", e)
                _vpnStateFlow.value = VpnState.ERROR("Initialization failed: ${e.message}", e)
                isKitInitialized = false
            }
        }
    }

    @Throws(IOException::class)
    private suspend fun copyRawResourceToFile(resId: Int, destFile: File) = withContext(Dispatchers.IO) {
        if (destFile.exists()) {
            Log.d(TAG, "Destination file ${destFile.name} exists, overwriting.")
        }
        Log.d(TAG, "Copying raw resource ID $resId to ${destFile.absolutePath}")
        try {
            appContext.resources.openRawResource(resId).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Successfully copied ${destFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy raw resource ${destFile.name}", e)
            throw IOException("Failed to copy ${destFile.name}", e)
        }
    }

    private suspend fun copyTun2SocksExecutable(): String? = withContext(Dispatchers.IO) {
        try {
            val deviceAbi = getDeviceAbi()
            Log.d(TAG, "Device ABI: $deviceAbi")

            val executableName = "tun2socks"

            val assetPath = "$deviceAbi/$executableName"

            val binDir = File(appContext.filesDir, "bin")
            if (!binDir.exists()) {
                binDir.mkdirs()
            }

            val targetFile = File(binDir, executableName)

            if (targetFile.exists() && targetFile.canExecute()) {
                Log.d(TAG, "tun2socks executable already exists at ${targetFile.absolutePath}")
                return@withContext targetFile.absolutePath
            }

            try {
                appContext.assets.open(assetPath).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                Log.d(TAG, "Successfully extracted tun2socks from assets")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to extract tun2socks from assets/$assetPath: ${e.message}")
                try {
                    val fallbackPath = "$deviceAbi/lib$executableName.so"
                    appContext.assets.open(fallbackPath).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                            output.flush()
                        }
                    }
                    Log.d(TAG, "Successfully extracted tun2socks from assets using fallback path")
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to extract tun2socks from fallback assets path: ${e.message}")

                    for (abi in Build.SUPPORTED_ABIS) {
                        try {
                            val alternativeAssetPath = "$abi/$executableName"
                            appContext.assets.open(alternativeAssetPath).use { input ->
                                FileOutputStream(targetFile).use { output ->
                                    input.copyTo(output)
                                    output.flush()
                                }
                            }
                            Log.d(TAG, "Successfully extracted tun2socks from alternative ABI: $abi")
                            break
                        } catch (e: IOException) {
                            continue
                        }
                    }

                    if (!targetFile.exists()) {
                        Log.e(TAG, "Could not find tun2socks executable for any supported ABI")
                        return@withContext null
                    }
                }
            }

            val executableSet = targetFile.setExecutable(true, false)
            Log.d(TAG, "Set executable permission via Java API: $executableSet")

            try {
                val process = Runtime.getRuntime().exec("chmod 755 ${targetFile.absolutePath}")
                val exitCode = process.waitFor()
                Log.d(TAG, "chmod command exit code: $exitCode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute chmod: ${e.message}")
            }

            if (!targetFile.canExecute()) {
                Log.e(TAG, "Failed to set executable permissions for ${targetFile.absolutePath}")
                return@withContext null
            }

            val isReadable = targetFile.canRead()
            val isWritable = targetFile.canWrite()
            Log.d(TAG, "File permissions - Read: $isReadable, Write: $isWritable, Execute: ${targetFile.canExecute()}")

            return@withContext targetFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error preparing tun2socks executable", e)
            return@withContext null
        }
    }


    override fun saveProfile(profile: Profile) {
        profileStorage.saveProfile(profile)
    }

    override fun deleteProfile(profileId: String) {
        profileStorage.deleteProfile(profileId)
    }

    override fun getProfile(profileId: String): Profile? {
        return profileStorage.getProfile(profileId)
    }

    override fun getAllProfiles(): List<Profile> {
        return profileStorage.getAllProfiles()
    }

    override fun selectProfileForVpn(profileId: String): Boolean {
        val profile = profileStorage.getProfile(profileId)
        return if (profile != null) {
            profileStorage.setSelectedVpnProfileId(profileId)
            true
        } else {
            false
        }
    }

    override fun getSelectedVpnProfile(): Profile? {
        val selectedId = profileStorage.getSelectedVpnProfileId()
        return selectedId?.let { profileStorage.getProfile(it) }
    }

    override fun startVpn(): Boolean {
        if (!isKitInitialized) {
            Log.e(TAG, "XrayVpnKit not initialized. Call initialize() first.")
            _vpnStateFlow.value = VpnState.ERROR("Kit not initialized")
            return false
        }
        if (_vpnStateFlow.value is VpnState.CONNECTING || _vpnStateFlow.value is VpnState.CONNECTED) {
            Log.w(TAG, "VPN is already connecting or connected.")
            return false
        }

        val selectedProfileId = profileStorage.getSelectedVpnProfileId()
        if (selectedProfileId == null) {
            Log.e(TAG, "No profile selected to start VPN.")
            _vpnStateFlow.value = VpnState.ERROR("No profile selected")
            return false
        }
        if (preparedTun2SocksPath == null) {
            Log.e(TAG, "Tun2socks executable path not prepared.")
            _vpnStateFlow.value = VpnState.ERROR("Tun2socks not prepared")
            return false
        }


        Log.i(TAG, "Attempting to start VPN with profile ID: $selectedProfileId")
        _vpnStateFlow.value = VpnState.CONNECTING

        val intent = Intent(appContext, CoreVpnService::class.java)
        intent.action = VpnKitConstants.ACTION_START_VPN
        intent.putExtra(VpnKitConstants.EXTRA_PROFILE_ID, selectedProfileId)
        intent.putExtra(VpnKitConstants.EXTRA_TUN2SOCKS_PATH, preparedTun2SocksPath)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
            Log.d(TAG, "Sent start command to CoreVpnService.")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to start CoreVpnService due to SecurityException. Did you grant BIND_VPN_SERVICE permission and prepare VpnService?", e)
            _vpnStateFlow.value = VpnState.ERROR("VPN permission not granted or service not prepared.", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CoreVpnService", e)
            _vpnStateFlow.value = VpnState.ERROR("Failed to start VPN service: ${e.message}", e)
            return false
        }
    }

    override fun stopVpn() {
        if (!isKitInitialized) {
            Log.e(TAG, "XrayVpnKit not initialized.")
            return
        }
        if (_vpnStateFlow.value == VpnState.IDLE || _vpnStateFlow.value is VpnState.STOPPING) {
            Log.w(TAG, "VPN is already stopped or stopping.")
            return
        }

        Log.i(TAG, "Attempting to stop VPN.")
        _vpnStateFlow.value = VpnState.STOPPING

        val intent = Intent(appContext, CoreVpnService::class.java)
        intent.action = VpnKitConstants.ACTION_STOP_VPN
        appContext.startService(intent)
        Log.d(TAG, "Sent stop command to CoreVpnService.")
    }


    fun release() {
        if (isKitInitialized) {
            try {
                appContext.unregisterReceiver(vpnStateReceiver)
                Log.d(TAG, "VPN state receiver unregistered.")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "VPN state receiver was not registered or already unregistered.")
            }
            isKitInitialized = false
        }
    }

    private fun getDeviceAbi(): String {
        return when {
            Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "arm64-v8a"
            Build.SUPPORTED_ABIS.contains("armeabi-v7a") -> "armeabi-v7a"
            Build.SUPPORTED_ABIS.contains("x86_64") -> "x86_64"
            Build.SUPPORTED_ABIS.contains("x86") -> "x86"
            else -> {
                Log.w(TAG, "Cannot determine device ABI, defaulting to arm64-v8a")
                "arm64-v8a"
            }
        }
    }


    companion object {
        private const val TAG = "XrayVpnKitImpl"

        @Volatile
        private var instance: XrayVpnKitImpl? = null

        fun getInstance(): XrayVpnKit {
            return instance ?: synchronized(this) {
                instance ?: XrayVpnKitImpl().also { instance = it }
            }
        }
    }
}