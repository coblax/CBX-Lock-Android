package com.coblax.examlock.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat

internal val PackageInventoryInvalidationActions = setOf(
    Intent.ACTION_PACKAGE_ADDED,
    Intent.ACTION_PACKAGE_REMOVED,
    Intent.ACTION_PACKAGE_CHANGED,
    Intent.ACTION_PACKAGE_REPLACED
)

internal fun shouldInvalidatePackageInventoryForAction(action: String?): Boolean {
    return action in PackageInventoryInvalidationActions
}

internal fun handlePackageInventoryChange(
    action: String?,
    invalidate: () -> Unit = { SecurityDetectorCache.invalidateStaticSecurity() }
): Boolean {
    if (!shouldInvalidatePackageInventoryForAction(action)) {
        return false
    }
    invalidate()
    return true
}

internal fun registerPackageInventoryInvalidationReceiver(
    context: Context,
    invalidate: () -> Unit = { SecurityDetectorCache.invalidateStaticSecurity() }
): () -> Unit {
    val appContext = context.applicationContext
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handlePackageInventoryChange(intent?.action, invalidate)
        }
    }
    val filter = IntentFilter().apply {
        PackageInventoryInvalidationActions.forEach(::addAction)
        addDataScheme("package")
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
    return {
        runCatching { appContext.unregisterReceiver(receiver) }
    }
}
