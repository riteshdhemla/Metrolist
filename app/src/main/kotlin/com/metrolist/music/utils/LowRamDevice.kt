package com.metrolist.music.utils

import android.app.ActivityManager
import android.content.Context

/**
 * True on devices where RAM, not CPU or network, is what limits playback: those the platform
 * itself flags as low-RAM, and anything with under ~2.5 GB in total (e.g. a 1.7 GB Tegra K1
 * tablet, which the platform does not flag). Callers use it to shrink caches and buffers that
 * are sized for phones with 4 GB or more.
 */
object LowRamDevice {
    private const val THRESHOLD_BYTES = 2_500L * 1024 * 1024

    @Volatile
    private var cached: Boolean? = null

    fun isLowRam(context: Context): Boolean =
        cached ?: run {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val info = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
            val low = am != null && (am.isLowRamDevice || info.totalMem in 1 until THRESHOLD_BYTES)
            cached = low
            low
        }
}
