package com.childprotect.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        val prefs = Prefs(this)
        val sender = intent?.getStringExtra(EXTRA_REPLY_TO).orEmpty()
        val command = runCatching { Command.valueOf(intent?.getStringExtra(EXTRA_COMMAND) ?: Command.LOCATE.name) }.getOrDefault(Command.LOCATE)
        val prefix = intent?.getStringExtra(EXTRA_PREFIX).orEmpty()
        val triggerBody = intent?.getStringExtra(EXTRA_BODY).orEmpty()
        scope.launch {
            try { handle(command, sender, prefix, prefs) }
            catch (e: Exception) { Log.e(TAG, "handle failed", e) }
            finally { if (triggerBody.isNotBlank()) kotlinx.coroutines.delay(1500); stopSelf() }
        }
        return START_NOT_STICKY
    }

    private suspend fun handle(command: Command, sender: String, prefix: String, prefs: Prefs) {
        when (command) {
            Command.HELP -> Reporter.sendSms(this, sender, Reporter.helpText())
            Command.STATUS -> Reporter.sendSms(this, sender, Reporter.statusReport(this, prefs))
            Command.LOCKDOWN -> {
                prefs.lockdownActive = true
                GuardianGuardService.start(this)
                AdminReceiver.lockNow(this)
                Reporter.sendSms(this, sender, getString(R.string.sms_lockdown_active))
            }
            Command.UNLOCKDOWN -> {
                prefs.lockdownActive = false
                sendBroadcast(Intent(EmergencyLockActivity.ACTION_UNLOCKDOWN).setPackage(packageName))
                if (prefs.lockScreenGuard) GuardianGuardService.start(this) else GuardianGuardService.stop(this)
                Reporter.sendSms(this, sender, getString(R.string.sms_lockdown_cleared))
            }
            Command.DATA_ON -> Reporter.sendSms(this, sender, resultLine("Mobile data ON", PrivilegedShell.enableMobileData(this, prefs)))
            Command.DATA_OFF -> Reporter.sendSms(this, sender, resultLine("Mobile data OFF", PrivilegedShell.disableMobileData(this, prefs)))
            Command.LOC_ON -> Reporter.sendSms(this, sender, resultLine("Location ON", PrivilegedShell.enableHighAccuracyLocation(this, prefs)))
            Command.RING -> if (prefs.ringEnabled) {
                val ringJob = scope.launch { AlarmPlayer.ring(this@LocateService, 30) }
                Reporter.sendSms(this, sender, "Ringing alarm for 30s… (send STOP to silence)")
                ringJob.join()
            } else Reporter.sendSms(this, sender, "Ring is disabled in settings.")
            Command.STOP -> { AlarmPlayer.stop(); Reporter.sendSms(this, sender, "Alarm stopped.") }
            Command.LOCK -> {
                val locked = AdminReceiver.lockNow(this)
                Reporter.sendSms(this, sender, if (locked) "Phone locked." else "Couldn't lock — enable Device admin in the app first.")
            }
            Command.LOCATE, Command.NONE -> {
                if (prefs.autoEnableLocation) PrivilegedShell.enableHighAccuracyLocation(this, prefs)
                if (prefs.autoEnableData) PrivilegedShell.enableMobileData(this, prefs)
                Reporter.sendSms(this, sender, Reporter.locationReport(this, Locator.current(this), prefix))
            }
        }
    }

    private fun resultLine(action: String, r: PrivilegedShell.Result): String = when (r) {
        is PrivilegedShell.Result.Ok -> "$action (via ${r.via})."
        PrivilegedShell.Result.Unavailable -> "$action FAILED — no Shizuku/root. Start Shizuku to allow this."
    }
    private fun buildNotification(): Notification {
        val mgr = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) mgr.createNotificationChannel(NotificationChannel(CHANNEL, "Locator", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, CHANNEL).setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build()
    }
    companion object {
        const val EXTRA_REPLY_TO = "reply_to"; const val EXTRA_COMMAND = "command"; const val EXTRA_PREFIX = "prefix"; const val EXTRA_BODY = "body"
        private const val TAG = "LocateService"; private const val CHANNEL = "locator"; private const val NOTIF_ID = 42
        fun start(context: android.content.Context, replyTo: String, command: Command, prefix: String = "", triggerBody: String = "") {
            val i = Intent(context, LocateService::class.java).apply {
                putExtra(EXTRA_REPLY_TO, replyTo); putExtra(EXTRA_COMMAND, command.name); putExtra(EXTRA_PREFIX, prefix); putExtra(EXTRA_BODY, triggerBody)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i) else context.startService(i)
        }
    }
}
