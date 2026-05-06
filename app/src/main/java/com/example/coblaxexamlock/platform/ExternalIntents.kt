package com.example.coblaxexamlock.platform
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.coblaxexamlock.launchPlatformIntentSafely

internal fun openExternalUrl(context: Context, url: String) {
    launchPlatformIntentSafely(context, Intent(Intent.ACTION_VIEW, url.toUri()))
}
