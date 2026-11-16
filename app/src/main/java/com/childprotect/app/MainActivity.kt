package com.childprotect.app

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.childprotect.app.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val shizukuPermListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        Toast.makeText(this, getString(if (result == PackageManager.PERMISSION_GRANTED) R.string.shizuku_granted else R.string.shizuku_denied), Toast.LENGTH_SHORT).show()
        refreshStatus()
    }
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val denied = result.filterValues { !it }.keys
        Toast.makeText(this, if (denied.isEmpty()) getString(R.string.permissions_granted) else getString(R.string.permissions_missing, denied.joinToString()), if (denied.isEmpty()) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
        refreshStatus()
        maybeRequestBackgroundLocation()
    }
    private val bgLocLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Toast.makeText(this, getString(if (granted) R.string.location_always_granted else R.string.location_always_hint), Toast.LENGTH_LONG).show()
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)
        b.editCode.setText(prefs.secretCode)
        b.editTrusted.setText(prefs.trustedContacts)
        b.switchPin.isChecked = prefs.pinEnabled
        b.editPin.setText(prefs.pin)
        b.switchShizuku.isChecked = prefs.useShizuku
        b.switchRoot.isChecked = prefs.attemptRootData
        b.switchAutoData.isChecked = prefs.autoEnableData
        b.switchAutoLoc.isChecked = prefs.autoEnableLocation
        b.switchRing.isChecked = prefs.ringEnabled
        b.switchSim.isChecked = prefs.simChangeAlert
        b.switchBattery.isChecked = prefs.lowBatteryAlert
        b.switchGuard.isChecked = prefs.lockScreenGuard

        b.btnLanguage.setOnClickListener {
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(if (current.startsWith("ar")) "en" else "ar"))
        }
        b.switchGuard.setOnCheckedChangeListener { _, checked ->
            prefs.lockScreenGuard = checked
            updateGuardService()
            refreshStatus()
        }
        b.btnAccessibilityGuide.setOnClickListener { showProtectionGuide() }
        b.btnEmergency.setOnClickListener { startActivity(Intent(this, EmergencyLockActivity::class.java)) }

        b.btnSave.setOnClickListener {
            prefs.secretCode = b.editCode.text.toString().trim()
            prefs.trustedContacts = b.editTrusted.text.toString().trim()
            prefs.pinEnabled = b.switchPin.isChecked
            prefs.pin = b.editPin.text.toString().trim()
            prefs.useShizuku = b.switchShizuku.isChecked
            prefs.attemptRootData = b.switchRoot.isChecked
            prefs.autoEnableData = b.switchAutoData.isChecked
            prefs.autoEnableLocation = b.switchAutoLoc.isChecked
            prefs.ringEnabled = b.switchRing.isChecked
            prefs.simChangeAlert = b.switchSim.isChecked
            prefs.lowBatteryAlert = b.switchBattery.isChecked
            prefs.lockScreenGuard = b.switchGuard.isChecked
            updateGuardService()
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        }
        b.btnPerms.setOnClickListener { requestPerms() }
        b.btnShizuku.setOnClickListener { requestShizuku() }
        b.btnGuide.setOnClickListener { showShizukuGuide() }
        b.btnAdmin.setOnClickListener { toggleAdmin() }
        Shizuku.addRequestPermissionResultListener(shizukuPermListener)
        refreshStatus()
    }

    override fun onResume() { super.onResume(); refreshStatus() }
    override fun onDestroy() { Shizuku.removeRequestPermissionResultListener(shizukuPermListener); super.onDestroy() }

    private fun updateGuardService() {
        if (prefs.lockScreenGuard || prefs.lockdownActive) GuardianGuardService.start(this)
        else GuardianGuardService.stop(this)
    }

    private fun requestPerms() {
        val needed = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) needed += Manifest.permission.POST_NOTIFICATIONS
        val missing = needed.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) maybeRequestBackgroundLocation() else permLauncher.launch(missing.toTypedArray())
    }
    private fun maybeRequestBackgroundLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bg = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine && !bg) bgLocLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
    private fun requestShizuku() {
        if (!Shizuku.pingBinder()) { Toast.makeText(this, R.string.shizuku_not_running, Toast.LENGTH_LONG).show(); return }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { Toast.makeText(this, R.string.already_granted, Toast.LENGTH_SHORT).show(); return }
        Shizuku.requestPermission(SHIZUKU_REQ)
    }
    private fun toggleAdmin() {
        if (AdminReceiver.isActive(this)) {
            AdminReceiver.dpm(this).removeActiveAdmin(AdminReceiver.component(this))
            Toast.makeText(this, R.string.admin_disabled, Toast.LENGTH_SHORT).show(); refreshStatus()
        } else {
            startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(this@MainActivity))
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_explanation))
            })
        }
    }
    private fun showShizukuGuide() {
        AlertDialog.Builder(this).setTitle(R.string.shizuku_title).setMessage(R.string.shizuku_guide)
            .setPositiveButton(android.R.string.ok, null).show()
    }
    private fun showProtectionGuide() {
        AlertDialog.Builder(this).setTitle(R.string.protection_setup_title).setMessage(R.string.protection_setup_message)
            .setPositiveButton(R.string.open_accessibility_settings) { _, _ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .setNeutralButton(R.string.open_overlay_settings) { _, _ -> startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))) }
            .setNegativeButton(android.R.string.cancel, null).show()
    }
    private fun refreshStatus() {
        val granted = { p: String -> ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED }
        val ok = granted(Manifest.permission.RECEIVE_SMS) && granted(Manifest.permission.SEND_SMS) && granted(Manifest.permission.ACCESS_FINE_LOCATION)
        b.txtStatus.text = getString(if (ok) R.string.permissions_status_ok else R.string.permissions_status_missing)
        b.txtShizuku.text = when {
            !Shizuku.pingBinder() -> getString(R.string.shizuku_status_off)
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> getString(R.string.shizuku_status_authorized)
            else -> getString(R.string.shizuku_status_pending)
        }
        val adminOn = AdminReceiver.isActive(this)
        b.txtAdmin.text = getString(if (adminOn) R.string.admin_status_on else R.string.admin_status_off)
        b.btnAdmin.text = getString(if (adminOn) R.string.admin_disable else R.string.admin_enable)
        b.txtGuardStatus.text = getString(if (prefs.lockdownActive) R.string.guard_status_lockdown else if (prefs.lockScreenGuard) R.string.guard_status_on else R.string.guard_status_off)
    }
    companion object { private const val SHIZUKU_REQ = 1001 }
}
