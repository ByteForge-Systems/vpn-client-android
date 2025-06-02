package ru.byteforge.xray_core.internal.core

import android.content.Context

internal interface XrayCoreProxy {

    /**
     * Инициализирует прокси и ядро Xray.
     * Должен быть вызван один раз перед использованием других методов.
     * Этот метод вызывает Libv2ray.initCoreEnv() и Seq.setContext().
     *
     * @param context Контекст приложения.
     * @param assetsPath Путь к директории, где лежат geoip.dat и geosite.dat.
     * @return true, если инициализация прошла успешно, иначе false.
     */
    fun initialize(context: Context, assetsPath: String): Boolean

    /**
     * Запускает ядро Xray с предоставленной конфигурацией.
     * Предполагается, что initialize() уже был успешно вызван.
     *
     * @param rawConfig Полная JSON-конфигурация Xray.
     * @return 0 в случае успеха (ядро запущено), отрицательное значение при ошибке.
     */
    fun runXray(rawConfig: String): Int

    /**
     * Останавливает работающее ядро Xray.
     *
     * @return 0 в случае успеха.
     */
    fun stopXray(): Int

    /**
     * Проверяет, запущено ли ядро Xray.
     * @return true, если ядро запущено, иначе false.
     */
    fun isCoreRunning(): Boolean

}