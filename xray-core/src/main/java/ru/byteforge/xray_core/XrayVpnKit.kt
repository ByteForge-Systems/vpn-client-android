package ru.byteforge.xray_core

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ru.byteforge.xray_core.api.Profile
import ru.byteforge.xray_core.api.VpnState
import ru.byteforge.xray_core.internal.XrayVpnKitImpl

interface XrayVpnKit {
    /**
     * Инициализирует библиотеку. Должен быть вызван один раз.
     */
    fun initialize(context: Context)

    /**
     * Добавляет или обновляет профиль конфигурации.
     * @param profile Конфигурация для добавления/обновления.
     */
    fun saveProfile(profile: Profile)

    /**
     * Удаляет профиль по ID.
     * @param profileId ID профиля для удаления.
     */
    fun deleteProfile(profileId: String)

    /**
     * Получает профиль по ID.
     */
    fun getProfile(profileId: String): Profile?

    /**
     * Возвращает список всех сохраненных профилей.
     */
    fun getAllProfiles(): List<Profile>

    /**
     * Выбирает профиль, который будет использоваться для VPN-соединения.
     * @param profileId ID профиля для выбора. False, если профиль не найден.
     */
    fun selectProfileForVpn(profileId: String): Boolean

    /**
     * Получает текущий выбранный для VPN профиль.
     */
    fun getSelectedVpnProfile(): Profile?

    /**
     * Запускает VPN-сервис, используя текущий выбранный профиль.
     * Запускает Xray-ядро и tun2socks.
     * Приложение должно запросить у пользователя разрешение android.permission.BIND_VPN_SERVICE.
     * @return true, если команда на запуск сервиса отправлена.
     */
    fun startVpn(): Boolean

    /**
     * Останавливает VPN-сервис, Xray-ядро и tun2socks.
     */
    fun stopVpn()

    /**
     * Предоставляет Flow для отслеживания состояния VPN-сервиса.
     */
    fun observeVpnState(): StateFlow<VpnState>

    companion object {
        /**
         * Получает единственный экземпляр XrayVpnKit.
         * Убедитесь, что initialize() был вызван перед использованием.
         */
        fun getInstance(): XrayVpnKit {
            // Делегируем вызов реализации
            return XrayVpnKitImpl.getInstance()
        }
    }
}