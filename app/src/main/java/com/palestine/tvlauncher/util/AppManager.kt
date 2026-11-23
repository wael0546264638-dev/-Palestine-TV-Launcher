package com.palestine.tvlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap

data class TvAppInfo(
    val label: String,
    val packageName: String,
    val bannerOrIcon: Drawable,
    val dominantColor: Color
)

object AppManager {

    fun loadTvApps(context: Context): List<TvAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolved = (pm.queryIntentActivities(mainIntent, 0) + pm.queryIntentActivities(launcherIntent, 0))
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }

        return resolved.mapNotNull { info ->
            try {
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                val drawable = try {
                    pm.getApplicationBanner(pkg) ?: info.loadBanner(pm) ?: info.loadIcon(pm)
                } catch (_: Exception) {
                    info.loadIcon(pm)
                }
                val color = extractDominantColor(drawable)
                TvAppInfo(label, pkg, drawable, color)
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.label }
    }

    fun launchApp(context: Context, packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun extractDominantColor(drawable: Drawable): Color {
        return try {
            val bmp: Bitmap = drawable.toBitmap(16, 16)
            var r = 0L
            var g = 0L
            var b = 0L
            var count = 0
            for (x in 0 until bmp.width) {
                for (y in 0 until bmp.height) {
                    val pixel = bmp.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xff
                    if (alpha > 128) {
                        r += (pixel shr 16) and 0xff
                        g += (pixel shr 8) and 0xff
                        b += pixel and 0xff
                        count++
                    }
                }
            }
            if (count > 0) Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
            else Color(0xFF38BDF8)
        } catch (_: Exception) {
            Color(0xFF38BDF8)
        }
    }
}

