package com.example.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("stack_ball_prefs", Context.MODE_PRIVATE)

    var currentLevel: Int
        get() = prefs.getInt("current_level", 1)
        set(value) = prefs.edit().putInt("current_level", value.coerceAtLeast(1)).apply()

    var coins: Int
        get() = prefs.getInt("coins", 0)
        set(value) = prefs.edit().putInt("coins", value.coerceAtLeast(0)).apply()

    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        set(value) = prefs.edit().putInt("high_score", value).apply()

    var selectedSkinId: String
        get() = prefs.getString("selected_skin_id", "classic_red") ?: "classic_red"
        set(value) = prefs.edit().putString("selected_skin_id", value).apply()

    var unlockedSkinIds: Set<String>
        get() = prefs.getStringSet("unlocked_skins", setOf("classic_red")) ?: setOf("classic_red")
        set(value) = prefs.edit().putStringSet("unlocked_skins", value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", true).apply()

    fun unlockSkin(skinId: String) {
        val current = unlockedSkinIds.toMutableSet()
        current.add(skinId)
        unlockedSkinIds = current
    }

    fun isSkinUnlocked(skinId: String): Boolean {
        return unlockedSkinIds.contains(skinId)
    }
}
