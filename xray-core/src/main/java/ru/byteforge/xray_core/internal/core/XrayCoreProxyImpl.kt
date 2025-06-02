package ru.byteforge.xray_core.internal.core

import android.content.Context
import android.provider.Settings
import android.util.Log
import go.Seq // Предполагаем, что этот импорт доступен из libv2ray.aar
import libv2ray.CoreCallbackHandler // Предполагаем доступность
import libv2ray.CoreController // Предполагаем доступность
import libv2ray.Libv2ray // Предполагаем доступность
import java.util.UUID

class XrayCoreProxyImpl : XrayCoreProxy {

    private var coreController: CoreController? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "XrayCoreProxy"
    }

    private class DefaultCoreCallback : CoreCallbackHandler {
        override fun onEmitStatus(l: Long, s: String?): Long {
            Log.d(TAG, "CoreCallback - onEmitStatus: code=$l, message=$s")
            return 0
        }

        override fun shutdown(): Long {
            Log.d(TAG, "CoreCallback - shutdown called by core")
            return 0
        }

        override fun startup(): Long {
            Log.d(TAG, "CoreCallback - startup callback from core")
            return 0
        }
    }

    override fun initialize(context: Context, assetsPath: String): Boolean {
        if (isInitialized) {
            Log.d(TAG, "Already initialized.")
            return true
        }
        try {
            Log.d(TAG, "Initializing XrayCoreProxy...")
            Seq.setContext(context.applicationContext)
            Log.d(TAG, "Seq context set.")

            val deviceId = getStableDeviceId(context)
//            Libv2ray.initCoreEnv(assetsPath, deviceId)
            Libv2ray.initCoreEnv(assetsPath, "5b7d8d7c3a9f45e1b6e80c2a6d7f84e50c93a2d67b9") // TODO: Change to generating id
            Log.d(TAG, "Libv2ray.initCoreEnv called with assetsPath: $assetsPath, deviceId: $deviceId")

            coreController = Libv2ray.newCoreController(DefaultCoreCallback())
            Log.d(TAG, "CoreController created.")

            isInitialized = true
            Log.i(TAG, "XrayCoreProxy initialized successfully.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during XrayCoreProxy initialization", e)
            isInitialized = false
            return false
        }
    }

    private fun getStableDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
            return androidId
        }
        // Если ANDROID_ID недоступен, генерируем и сохраняем UUID
        val prefs = context.getSharedPreferences("xray_kit_device_id_pref", Context.MODE_PRIVATE)
        var deviceUuid = prefs.getString("device_uuid", null)
        if (deviceUuid == null) {
            deviceUuid = UUID.randomUUID().toString()
            prefs.edit().putString("device_uuid", deviceUuid).apply()
        }
        return deviceUuid
    }

    override fun runXray(rawConfig: String): Int {
        if (!isInitialized || coreController == null) {
            Log.e(TAG, "XrayCoreProxy not initialized. Cannot run Xray.")
            return -1 // Код ошибки: не инициализировано
        }
        if (coreController?.isRunning == true) {
            Log.w(TAG, "Xray core is already running. Call stopXray() first if you want to restart with a new config.")
            return -2 // Код ошибки: уже запущено
        }

        try {
            Log.d(TAG, "Starting Xray core loop with config...")
            coreController?.startLoop(rawConfig) // startLoop возвращает void

            if (coreController?.isRunning == true) {
                Log.i(TAG, "Xray core started successfully.")
                return 0 // Успех
            } else {
                Log.e(TAG, "Xray core failed to start (isRunning is false after startLoop).")
                return -3 // Код ошибки: не удалось запустить
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Xray core loop", e)
            return -4 // Код ошибки: исключение при запуске
        }
    }

    override fun stopXray(): Int {
        if (!isInitialized || coreController == null) {
            Log.w(TAG, "XrayCoreProxy not initialized or already stopped.")
            return 0 // Считаем успехом, если и так нечего останавливать
        }
        if (coreController?.isRunning == false) {
            Log.d(TAG, "Xray core is not running.")
            return 0 // Успех, уже остановлено
        }

        try {
            Log.d(TAG, "Stopping Xray core loop...")
            coreController?.stopLoop()
            Log.i(TAG, "Xray core stopped.")
            return 0 // Успех
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Xray core loop", e)
            return -1 // Код ошибки: исключение при остановке
        }
    }

    override fun isCoreRunning(): Boolean {
        return isInitialized && coreController?.isRunning == true
    }


}