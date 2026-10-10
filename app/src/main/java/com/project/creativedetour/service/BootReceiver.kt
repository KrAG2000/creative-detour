package com.project.creativedetour.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.project.creativedetour.app
import com.project.creativedetour.sensing.Access

/** Restarts the service after a reboot or an app update (including every Run ▶ from Android Studio). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (context.app.onboarded && Access.hasActivity(context)) DetourService.start(context)
    }
}
