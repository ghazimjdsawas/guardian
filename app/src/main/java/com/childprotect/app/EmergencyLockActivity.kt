package com.childprotect.app

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** A plainly labeled protection screen. It never replaces Android's lock screen. */
class EmergencyLockActivity : AppCompatActivity() {
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { finish() }
    }

    private val credentialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_lock)
        findViewById<android.widget.TextView>(R.id.emergencyNotice).text = getString(R.string.emergency_notice)
        findViewById<android.widget.Button>(R.id.btnOwnerUnlock).apply {
            text = getString(R.string.emergency_unlock)
            setOnClickListener { requestOwnerAuthentication() }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(ACTION_UNLOCKDOWN), ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        runCatching { unregisterReceiver(unlockReceiver) }
        super.onStop()
    }

    private fun requestOwnerAuthentication() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            getString(R.string.auth_title), getString(R.string.auth_description)
        )
        if (intent != null) credentialLauncher.launch(intent)
        else MaterialAlertDialogBuilder(this)
            .setMessage(R.string.auth_not_configured)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    companion object {
        const val ACTION_UNLOCKDOWN = "com.childprotect.app.action.UNLOCKDOWN"
    }
}
