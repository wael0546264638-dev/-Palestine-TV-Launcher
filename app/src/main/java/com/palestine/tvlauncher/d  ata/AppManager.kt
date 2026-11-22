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
    suspend fun loadInstalledApps(context: Context): List<TvAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val appsMap = linkedMapOf<String, TvAppInfo>()

        val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val normalIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveList = pm.queryIntentActivities(leanbackIntent, 0) +
                pm.queryIntentActivities(normalIntent, 0)

        for (info in resolveList) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || appsMap.containsKey(pkg)) continue

            val label = info.loadLabel(pm).toString()
            val drawable = try {
                pm.getApplicationBanner(pkg) ?: info.loadBanner(pm) ?: info.loadIcon(pm)
            } catch (e: Exception) {
                info.loadIcon(pm)
            }

            val color = extractDominantColor(drawable)
            appsMap[pkg] = TvAppInfo(label, pkg, drawable, color)
        }
        appsMap.values.toList()
    }

    private fun extractDominantColor(drawable: Drawable): Color {
        return try {
            val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val palette = Palette.from(bitmap).generate()
            val intColor = palette.getVibrantColor(
                palette.getDominantColor(android.graphics.Color.WHITE)
            )
            bitmap.recycle()
            Color(intColor)
        } catch (e: Exception) {
            Color.White
        }
    }

    fun launchApp(context: Context, packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}

