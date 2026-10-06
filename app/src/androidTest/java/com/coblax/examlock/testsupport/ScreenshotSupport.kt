package com.coblax.examlock.testsupport

import android.os.Build

/**
 * Compose's captureToImage reads windows through PixelCopy, which Android 7 lacks, so a
 * screenshot there throws NoSuchMethodError and fails a test whose checks all passed.
 * Screenshots are a review aid, so they are taken from Android 8 on and skipped below.
 */
internal val canCaptureScreenshots: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
