package ru.byteforge.xray_core.internal.core

import android.os.ParcelFileDescriptor

internal interface Tun2SocksProxy {

    /**
     * Запускает процесс tun2socks.
     *
     * @param tunFd Файловый дескриптор TUN-интерфейса.
     * @param socksProxyAddress Адрес локального SOCKS5-прокси (например, "127.0.0.1:10808").
     * @param tunAddress IPv4-адрес для TUN-интерфейса (например, "10.0.0.1").
     * @param tunPrefixLength Длина префикса для tunAddress (например, 24 для /24).
     * @param tunDns Адреса DNS-серверов для использования (например, "1.1.1.1,8.8.8.8").
     * @param tunMtu MTU для TUN-интерфейса.
     * @param tun2socksExecutablePath Полный путь к исполняемому файлу tun2socks.
     * @return true, если процесс tun2socks был успешно запущен, иначе false.
     */
    fun startTun2Socks(
        tunFd: ParcelFileDescriptor,
        socksProxyAddress: String,
        tunAddress: String,
        tunPrefixLength: Int,
        tunDns: String,
        tunMtu: Int,
        tun2socksExecutablePath: String
    ): Boolean

    /**
     * Останавливает работающий процесс tun2socks.
     */
    fun stopTun2Socks()

    /**
     * Проверяет, запущен ли процесс tun2socks.
     * @return true, если процесс активен, иначе false.
     */
    fun isRunning(): Boolean

}