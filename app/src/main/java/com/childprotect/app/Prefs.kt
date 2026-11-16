package com.childprotect.app

import android.content.Context

/** Small SharedPreferences wrapper for owner-controlled settings. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("child_protect", Context.MODE_PRIVATE)

    init {
        if (!sp.contains("trusted_contacts") && sp.contains("allowed_sender")) {
            val old = sp.getString("allowed_sender", "").orEmpty()
            sp.edit().putString("trusted_contacts", old).remove("allowed_sender").apply()
        }
    }

    var secretCode: String
        get() = sp.getString("secret_code", "") ?: ""
        set(v) = sp.edit().putString("secret_code", v).apply()
    var trustedContacts: String
        get() = sp.getString("trusted_contacts", "") ?: ""
        set(v) = sp.edit().putString("trusted_contacts", v).apply()
    fun trustedList(): List<String> = trustedContacts.split(',', '\n', ';').map { it.trim() }.filter { it.isNotEmpty() }
    val ownerNumber: String get() = trustedList().firstOrNull().orEmpty()
    var pinEnabled: Boolean
        get() = sp.getBoolean("pin_enabled", false)
        set(v) = sp.edit().putBoolean("pin_enabled", v).apply()
    var pin: String
        get() = sp.getString("pin", "") ?: ""
        set(v) = sp.edit().putString("pin", v).apply()
    var useShizuku: Boolean
        get() = sp.getBoolean("use_shizuku", true)
        set(v) = sp.edit().putBoolean("use_shizuku", v).apply()
    var attemptRootData: Boolean
        get() = sp.getBoolean("attempt_root_data", true)
        set(v) = sp.edit().putBoolean("attempt_root_data", v).apply()
    var autoEnableData: Boolean
        get() = sp.getBoolean("auto_enable_data", true)
        set(v) = sp.edit().putBoolean("auto_enable_data", v).apply()
    var autoEnableLocation: Boolean
        get() = sp.getBoolean("auto_enable_location", true)
        set(v) = sp.edit().putBoolean("auto_enable_location", v).apply()
    var ringEnabled: Boolean
        get() = sp.getBoolean("ring_enabled", true)
        set(v) = sp.edit().putBoolean("ring_enabled", v).apply()
    var simChangeAlert: Boolean
        get() = sp.getBoolean("sim_change_alert", true)
        set(v) = sp.edit().putBoolean("sim_change_alert", v).apply()
    var simIccid: String
        get() = sp.getString("sim_iccid", "") ?: ""
        set(v) = sp.edit().putString("sim_iccid", v).apply()
    var lowBatteryAlert: Boolean
        get() = sp.getBoolean("low_battery_alert", true)
        set(v) = sp.edit().putBoolean("low_battery_alert", v).apply()
    var lastAlertAt: Long
        get() = sp.getLong("last_alert_at", 0L)
        set(v) = sp.edit().putLong("last_alert_at", v).apply()
    var lastSimAlertAt: Long
        get() = sp.getLong("last_sim_alert_at", 0L)
        set(v) = sp.edit().putLong("last_sim_alert_at", v).apply()
    var lockScreenGuard: Boolean
        get() = sp.getBoolean("lock_screen_guard", false)
        set(v) = sp.edit().putBoolean("lock_screen_guard", v).apply()
    var lockdownActive: Boolean
        get() = sp.getBoolean("lockdown_active", false)
        set(v) = sp.edit().putBoolean("lockdown_active", v).apply()
}
