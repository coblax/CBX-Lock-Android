package com.example.coblaxexamlock.platform
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
internal fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }
}
