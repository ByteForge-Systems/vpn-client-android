package ru.byteforge.xray_core.internal.core

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class Tun2SocksProxyImpl : Tun2SocksProxy {

    private var tun2socksProcess: Process? = null
    private var monitorJob: Job? = null

    companion object {
        private const val TAG = "Tun2SocksProxy"
    }

    override fun startTun2Socks(
        tunFd: ParcelFileDescriptor,
        socksProxyAddress: String,
        tunAddress: String,
        tunPrefixLength: Int,
        tunDns: String,
        tunMtu: Int,
        tun2socksExecutablePath: String
    ): Boolean {
        if (isRunning()) {
            Log.w(TAG, "tun2socks is already running.")
            return true
        }

        stopTun2Socks()

        val command = mutableListOf(
            tun2socksExecutablePath,
            "--netif-fd", tunFd.fd.toString(),
            "--socks-server-addr", socksProxyAddress,
            "--tunmtu", tunMtu.toString(),
            "--tundns", tunDns,
            "--loglevel", "3" // 0 (none), 1 (error), 2 (warning), 3 (notice), 4 (info), 5 (debug)

        )

        Log.i(TAG, "Starting tun2socks with command: ${command.joinToString(" ")}")

        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)

            tun2socksProcess = processBuilder.start()

            monitorJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reader = BufferedReader(InputStreamReader(tun2socksProcess?.inputStream))
                    var line: String? = ""
                    while (isActive && reader.readLine().also { line = it } != null) {
                        Log.d(TAG, "tun2socks: $line")
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error reading tun2socks output", e)
                    }
                } finally {
                    val exitCode = tun2socksProcess?.waitFor()
                    Log.i(TAG, "tun2socks process exited with code: $exitCode")

                    synchronized(this@Tun2SocksProxyImpl) {
                        if (tun2socksProcess?.exitValue() != null) {
                            tun2socksProcess = null
                        }
                    }
                }
            }


            Thread.sleep(500)
            if (tun2socksProcess?.isAlive == true) {
                Log.i(TAG, "tun2socks process started successfully.")
                return true
            } else {
                Log.e(TAG, "tun2socks process failed to start or exited immediately.")
                stopTun2Socks()
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tun2socks process", e)
            tun2socksProcess = null
            return false
        }
    }

    override fun stopTun2Socks() {
        monitorJob?.cancel()
        monitorJob = null

        if (tun2socksProcess != null) {
            Log.i(TAG, "Stopping tun2socks process...")
            try {
                tun2socksProcess?.destroy()
                val exited = tun2socksProcess?.waitFor(1, java.util.concurrent.TimeUnit.SECONDS) ?: true
                if (!exited) {
                    Log.w(TAG, "tun2socks process did not exit gracefully, forcing destroyForcibly.")
                    tun2socksProcess?.destroyForcibly()
                }
                Log.i(TAG, "tun2socks process stopped.")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping tun2socks process", e)
            } finally {
                tun2socksProcess = null
            }
        } else {
            Log.d(TAG, "tun2socks process was not running or already stopped.")
        }
    }

    override fun isRunning(): Boolean {
        return tun2socksProcess?.isAlive == true
    }
}