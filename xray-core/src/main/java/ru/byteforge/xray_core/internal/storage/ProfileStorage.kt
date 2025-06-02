package ru.byteforge.xray_core.internal.storage

import ru.byteforge.xray_core.api.Profile

internal interface ProfileStorage {

    /**
     * Сохраняет (добавляет новый или обновляет существующий) профиль конфигурации.
     * Профиль идентифицируется по [Profile.id].
     *
     * @param profile Объект [Profile] для сохранения.
     */
    fun saveProfile(profile: Profile)

    /**
     * Удаляет профиль конфигурации по его уникальному идентификатору.
     *
     * @param profileId Уникальный идентификатор профиля для удаления.
     */
    fun deleteProfile(profileId: String)

    /**
     * Получает профиль конфигурации по его уникальному идентификатору.
     *
     * @param profileId Уникальный идентификатор профиля.
     * @return Объект [Profile], если найден, иначе null.
     */
    fun getProfile(profileId: String): Profile?

    /**
     * Возвращает список всех сохраненных профилей конфигурации.
     *
     * @return Список объектов [Profile]. Может быть пустым, если профилей нет.
     */
    fun getAllProfiles(): List<Profile>

    /**
     * Устанавливает идентификатор профиля, который выбран для использования в VPN.
     *
     * @param profileId Уникальный идентификатор выбранного профиля, или null, если выбор сброшен.
     */
    fun setSelectedVpnProfileId(profileId: String?)

    /**
     * Возвращает идентификатор текущего выбранного профиля для VPN.
     *
     * @return Уникальный идентификатор выбранного профиля, или null, если ни один профиль не выбран.
     */
    fun getSelectedVpnProfileId(): String?

}