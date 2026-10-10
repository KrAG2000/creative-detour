package com.project.creativedetour.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * What we can know about the phone *without* running the model. Anything subtler
 * (weak GPU, broken driver, slow CPU) is found by AiCapability actually running it.
 */
object DeviceSpecs {
    private const val MIN_RAM_GB = 3.5 // phones report less than advertised: a "4 GB" phone shows ~3.6
    private const val MIN_FREE_BYTES = 1_500_000_000L

    fun totalRamGb(context: Context): Double = memoryInfo(context).totalMem / 1_073_741_824.0

    /** A reason the model can never run here, or null. Shown before offering the 2.6 GB download. */
    fun blocker(context: Context): String? {
        val ram = totalRamGb(context)
        return when {
            Build.SUPPORTED_64_BIT_ABIS.none { it == "arm64-v8a" || it == "x86_64" } ->
                "This phone has a 32-bit processor. The AI model needs a 64-bit one."
            context.getSystemService(ActivityManager::class.java).isLowRamDevice ->
                "Android marks this phone as a low-memory device."
            ram < MIN_RAM_GB ->
                "This phone has %.1f GB of memory. The AI model needs about 4 GB.".format(ram)
            else -> null
        }
    }

    /** Is there room to load the model *right now*? Other apps may be using the memory. */
    fun hasHeadroomNow(context: Context): Boolean {
        val info = memoryInfo(context)
        return !info.lowMemory && info.availMem >= MIN_FREE_BYTES
    }

    private fun memoryInfo(context: Context) = ActivityManager.MemoryInfo().also {
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
    }
}
