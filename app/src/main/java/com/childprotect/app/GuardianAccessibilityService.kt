package com.childprotect.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

/** Registered opt-in helper. It does not inspect window contents or interfere with system UI. */
class GuardianAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility helper connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    companion object { private const val TAG = "GuardianAccessibility" }
}
