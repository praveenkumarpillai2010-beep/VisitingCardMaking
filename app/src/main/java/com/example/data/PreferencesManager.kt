package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cardmaker_prefs", Context.MODE_PRIVATE)

    companion object {
        // Auth Keys
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_ACCOUNT_TYPE = "account_type" // "Free" or "Premium"
        private const val KEY_SUB_PLAN = "sub_plan" // "None", "Monthly", "Yearly", "Lifetime"

        // Mode System
        private const val KEY_THEME_MODE = "theme_mode" // "SYSTEM", "LIGHT", "DARK"
        private const val KEY_LANGUAGE = "language" // "en", "hi", "es", "fr"
        private const val KEY_NOTIFS_ENABLED = "notifs_enabled"

        // Ad ID Keys
        private const val KEY_BANNER_ID = "banner_id"
        private const val KEY_BANNER_ENABLED = "banner_enabled"
        private const val KEY_INTERSTITIAL_ID = "interstitial_id"
        private const val KEY_INTERSTITIAL_ENABLED = "interstitial_enabled"
        private const val KEY_REWARDED_ID = "rewarded_id"
        private const val KEY_REWARDED_ENABLED = "rewarded_enabled"
        private const val KEY_APP_OPEN_ID = "app_open_id"
        private const val KEY_APP_OPEN_ENABLED = "app_open_enabled"
        private const val KEY_NATIVE_ID = "native_id"
        private const val KEY_NATIVE_ENABLED = "native_enabled"
    }

    // AUTH GETTERS & SETTERS
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Guest User") ?: "Guest"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "guest@pillaiplay.com") ?: "guest@pillaiplay.com"
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userPhoto: String
        get() = prefs.getString(KEY_USER_PHOTO, "ic_avatar") ?: "ic_avatar"
        set(value) = prefs.edit().putString(KEY_USER_PHOTO, value).apply()

    var accountType: String
        get() = prefs.getString(KEY_ACCOUNT_TYPE, "Free") ?: "Free"
        set(value) = prefs.edit().putString(KEY_ACCOUNT_TYPE, value).apply()

    var subscriptionPlan: String
        get() = prefs.getString(KEY_SUB_PLAN, "None") ?: "None"
        set(value) {
            prefs.edit().putString(KEY_SUB_PLAN, value).apply()
            accountType = if (value == "None") "Free" else "Premium"
        }

    // THEME SYSTEM & SETTINGS
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFS_ENABLED, value).apply()

    // ADBANNER VALUES
    var bannerAdId: String
        get() = prefs.getString(KEY_BANNER_ID, "ca-app-pub-3940256099942544/6300978111") ?: "ca-app-pub-3940256099942544/6300978111"
        set(value) = prefs.edit().putString(KEY_BANNER_ID, value).apply()

    var bannerAdEnabled: Boolean
        get() = prefs.getBoolean(KEY_BANNER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BANNER_ENABLED, value).apply()

    // INTERSTITIAL AD VALUES
    var interstitialAdId: String
        get() = prefs.getString(KEY_INTERSTITIAL_ID, "ca-app-pub-3940256099942544/1033173712") ?: "ca-app-pub-3940256099942544/1033173712"
        set(value) = prefs.edit().putString(KEY_INTERSTITIAL_ID, value).apply()

    var interstitialAdEnabled: Boolean
        get() = prefs.getBoolean(KEY_INTERSTITIAL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_INTERSTITIAL_ENABLED, value).apply()

    // REWARDED AD VALUES
    var rewardedAdId: String
        get() = prefs.getString(KEY_REWARDED_ID, "ca-app-pub-3940256099942544/5224354917") ?: "ca-app-pub-3940256099942544/5224354917"
        set(value) = prefs.edit().putString(KEY_REWARDED_ID, value).apply()

    var rewardedAdEnabled: Boolean
        get() = prefs.getBoolean(KEY_REWARDED_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REWARDED_ENABLED, value).apply()

    // OPEN APP AD VALUES
    var appOpenAdId: String
        get() = prefs.getString(KEY_APP_OPEN_ID, "ca-app-pub-3940256099942544/3419835294") ?: "ca-app-pub-3940256099942544/3419835294"
        set(value) = prefs.edit().putString(KEY_APP_OPEN_ID, value).apply()

    var appOpenAdEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_OPEN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_OPEN_ENABLED, value).apply()

    // NATIVE AD VALUES
    var nativeAdId: String
        get() = prefs.getString(KEY_NATIVE_ID, "ca-app-pub-3940256099942544/2247696110") ?: "ca-app-pub-3940256099942544/2247696110"
        set(value) = prefs.edit().putString(KEY_NATIVE_ID, value).apply()

    var nativeAdEnabled: Boolean
        get() = prefs.getBoolean(KEY_NATIVE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NATIVE_ENABLED, value).apply()

    // CLEAR ALL FOR SIGN OUT
    fun clearAuth() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO)
            .remove(KEY_ACCOUNT_TYPE)
            .remove(KEY_SUB_PLAN)
            .apply()
    }
}
