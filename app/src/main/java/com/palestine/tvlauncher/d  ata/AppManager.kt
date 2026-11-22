package com.palestine.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TvAppInfo(
    val label: String,
    val packageName: String,
    val bannerOrIcon: Drawable,
    val dominantColor: Color
)

object AppManager {
    suspend fun loadInstalledApps(
        context: Context
    ): List<TvAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val map = linkedMapOf<String, TvAppInfo>()
        val i1 = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val i2 = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(i1, 0) +
                pm.queryIntentActivities(i2, 0)

        for (info in list) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || map.containsKey(pkg)) {
                continue
            }
            val label = info.loadLabel(pm).toString()
            val icon = try {
                pm.getApplicationBanner(pkg) ?: info.loadIcon(pm)
            } catch (_: Exception) {
                info.loadIcon(pm)
            }
            map[pkg] = TvAppInfo(
                label = label,
                packageName = pkg,
                bannerOrIcon = icon,
                dominantColor = getColor(icon)
            )
        }
        map.values.toList()
    }

    private fun getColor(d: Drawable): Color {
        return try {
            val bmp = Bitmap.createBitmap(
                32, 32, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            d.setBounds(0, 0, 32, 32)
            d.draw(canvas)
            val p = Palette.from(bmp).generate()
            val c = p.getVibrantColor(
                p.getDominantColor(android.graphics.Color.WHITE)
            )
            bmp.recycle()
            Color(c)
        } catch (_: Exception) {
            Color.White
        }
    }

    fun launchApp(context: Context, pkg: String) {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}
