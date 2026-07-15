package com.coblax.examlock.platform
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.coblax.examlock.launchPlatformIntentSafely

internal fun openExternalUrl(context: Context, url: String) {
    launchPlatformIntentSafely(context, Intent(Intent.ACTION_VIEW, url.toUri()))
}
