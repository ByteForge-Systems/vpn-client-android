package ru.byteforge.xray_core.internal.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.byteforge.xray_core.api.Profile

class ProfileStorageImpl(
    private val context: Context
) : ProfileStorage {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "xray_vpn_kit_profiles"
        private const val KEY_PROFILES_MAP = "profiles_map_v1"
        private const val KEY_SELECTED_VPN_PROFILE_ID = "selected_vpn_profile_id_v1"
    }

    override fun saveProfile(profile: Profile) {
        val profilesMap = getAllProfilesAsMap().toMutableMap()
        profilesMap[profile.id] = gson.toJson(profile)
        saveProfilesMap(profilesMap)
    }

    override fun deleteProfile(profileId: String) {
        val profilesMap = getAllProfilesAsMap().toMutableMap()
        if (profilesMap.remove(profileId) != null) {
            saveProfilesMap(profilesMap)

            if (getSelectedVpnProfileId() == profileId) {
                setSelectedVpnProfileId(null)
            }
        }
    }

    override fun getProfile(profileId: String): Profile? {
        val profileJson = getAllProfilesAsMap()[profileId]
        return profileJson?.let { gson.fromJson(it, Profile::class.java) }
    }

    override fun getAllProfiles(): List<Profile> {
        return getAllProfilesAsMap().values.mapNotNull { profileJson ->
            try {
                gson.fromJson(profileJson, Profile::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun setSelectedVpnProfileId(profileId: String?) {
        prefs.edit().putString(KEY_SELECTED_VPN_PROFILE_ID, profileId).apply()
    }

    override fun getSelectedVpnProfileId(): String? {
        return prefs.getString(KEY_SELECTED_VPN_PROFILE_ID, null)
    }

    /**
     * Вспомогательный метод для получения всех профилей в виде Map<ID, JsonString>.
     */
    private fun getAllProfilesAsMap(): Map<String, String> {
        val json = prefs.getString(KEY_PROFILES_MAP, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, String>>() {}.type
            try {
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    /**
     * Вспомогательный метод для сохранения Map профилей в SharedPreferences.
     */
    private fun saveProfilesMap(profilesMap: Map<String, String>) {
        val json = gson.toJson(profilesMap)
        prefs.edit().putString(KEY_PROFILES_MAP, json).apply()
    }
}