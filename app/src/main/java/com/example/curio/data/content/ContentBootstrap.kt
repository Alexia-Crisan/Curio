package com.example.curio.data.content

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.TestContent
import java.io.IOException

object ContentBootstrap {
    private const val TAG = "CurioContent"

    fun index(context: Context): ContentIndex {
        val result = try {
            AssetContentSource(context).load()
        } catch (e: IOException) {
            Log.e(TAG, "Cannot read assets/content", e)
            return if (isDebuggable(context)) TestContent.index else ContentIndex(emptyList(), emptyList())
        }
        result.issues.forEach { Log.w(TAG, it.toString()) }
        if (!isDebuggable(context) || result.index.themes.any { it.available }) return result.index
        Log.w(TAG, "No playable theme in assets/content: using test fixtures (debug build only)")
        return TestContent.index
    }

    private fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
