package com.example.coblaxexamlock.config
import com.example.coblaxexamlock.model.ExamKeyboardPolicy
import com.example.coblaxexamlock.model.ExamKeyboardRule
// Central registry for keyboards that need explicit allow/block handling.
// System keyboards outside this list are still accepted by the fallback system-app check.
internal val ExamKeyboardRules = listOf(
    ExamKeyboardRule(
        packageName = "com.samsung.android.honeyboard",
        policy = ExamKeyboardPolicy.Allow,
        note = "Samsung Honeyboard"
    ),
    ExamKeyboardRule(
        packageName = "com.sec.android.inputmethod",
        policy = ExamKeyboardPolicy.Allow,
        note = "Samsung legacy keyboard"
    ),
    ExamKeyboardRule(
        packageName = "com.android.inputmethod.latin",
        policy = ExamKeyboardPolicy.Allow,
        note = "AOSP Latin IME"
    ),
    ExamKeyboardRule(
        packageName = "com.google.android.inputmethod.latin",
        policy = ExamKeyboardPolicy.Allow,
        note = "Gboard"
    ),
    ExamKeyboardRule(
        packageName = "com.facemoji.lite.vivo",
        policy = ExamKeyboardPolicy.Allow,
        note = "Vivo Facemoji Lite Keyboard"
    ),
    ExamKeyboardRule(
        packageName = "com.facemoji.lite.transsion",
        policy = ExamKeyboardPolicy.Allow,
        note = "Transsion Facemoji Lite Keyboard"
    ),
    ExamKeyboardRule(
        packageName = "com.grammarly.android.keyboard",
        policy = ExamKeyboardPolicy.Block,
        note = "Grammarly Keyboard"
    ),
    ExamKeyboardRule(
        packageName = "com.touchtype.swiftkey",
        policy = ExamKeyboardPolicy.Block,
        note = "Microsoft SwiftKey"
    ),
    ExamKeyboardRule(
        packageName = "com.aitype.android.p",
        policy = ExamKeyboardPolicy.Block,
        note = "ai.type Keyboard"
    )
)

internal val AllowedExamKeyboardPackages = ExamKeyboardRules
    .filter { it.policy == ExamKeyboardPolicy.Allow }
    .mapTo(linkedSetOf()) { it.packageName }

internal val BlockedExamKeyboardPackages = ExamKeyboardRules
    .filter { it.policy == ExamKeyboardPolicy.Block }
    .mapTo(linkedSetOf()) { it.packageName }

internal val TrustedOemKeyboardManufacturers = setOf(
    "xiaomi",
    "redmi",
    "poco",
    "samsung",
    "oppo",
    "realme",
    "vivo",
    "iqoo",
    "infinix",
    "tecno"
)

internal val AllowedSystemKeyboardPackagePrefixes = listOf(
    "com.android.inputmethod.",
    "com.google.android.inputmethod.",
    "com.samsung.android.",
    "com.sec.android.",
    "com.miui.",
    "com.xiaomi.",
    "com.sohu.inputmethod.sogou.xiaomi",
    "com.coloros.",
    "com.oplus.",
    "com.heytap.",
    "com.realme.",
    "com.baidu.input_vivo",
    "com.vivo.",
    "com.transsion.",
    "com.tecno.",
    "com.infinix."
)

internal val SuspiciousKeyboardPackageTokens = listOf(
    "grammarly",
    "swiftkey",
    "touchtype",
    "copilot",
    "assistant",
    "openai",
    "chatgpt",
    "gemini",
    "claude"
)

internal val RootBinaryIndicatorPaths = listOf(
    "/system/bin/su",
    "/system/xbin/su",
    "/sbin/su",
    "/vendor/bin/su",
    "/su/bin/su",
    "/system/bin/.ext/su",
    "/system/bin/.ext/.su",
    "/system/bin/failsafe/su",
    "/system/xbin/daemonsu",
    "/system/bin/daemonsu",
    "/data/local/su",
    "/data/local/bin/su",
    "/data/local/xbin/su",
    "/system/bin/busybox",
    "/system/xbin/busybox",
    "/vendor/bin/busybox",
    "/system/sbin/busybox",
    "/sbin/busybox"
)

internal val RootPackageNames = listOf(
    "com.topjohnwu.magisk",
    "eu.chainfire.supersu",
    "com.koushikdutta.superuser",
    "com.thirdparty.superuser",
    "com.noshufou.android.su"
)

internal val MagiskIndicatorPaths = listOf(
    "/sbin/.magisk",
    "/data/adb/magisk",
    "/data/adb/modules",
    "/data/adb/zygisk",
    "/cache/.magisk",
    "/metadata/magisk"
)

internal val RiskyAccessibilityKeywords = listOf(
    "teamviewer",
    "anydesk",
    "autoclicker",
    "clickassistant",
    "macro",
    "automate",
    "tasker",
    "control",
    "remote"
)

internal val VirtualFingerprintTokens = listOf(
    "generic",
    "unknown",
    "emulator",
    "sdk_gphone"
)

internal val VirtualModelTokens = listOf(
    "Emulator",
    "Android SDK built for x86",
    "google_sdk",
    "sdk_gphone"
)

internal val VirtualManufacturerTokens = listOf(
    "Genymotion",
    "BlueStacks",
    "Nox",
    "MuMu",
    "Xamarin"
)

internal val VirtualProductTokens = listOf(
    "sdk",
    "sdk_gphone",
    "emulator",
    "vbox86",
    "simulator"
)

internal val VirtualHardwareTokens = listOf(
    "goldfish",
    "ranchu",
    "vbox86",
    "nox",
    "ttvm",
    "android_x86"
)

internal val VirtualQemuFiles = listOf(
    "/dev/qemu_pipe",
    "/dev/qemu_trace",
    "/system/bin/qemu-props",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/system/lib64/libc_malloc_debug_qemu.so"
)

internal val EmulatorPackagePrefixes = listOf(
    "com.bluestacks.",
    "com.genymotion.",
    "com.nox.mopen.app",
    "com.bignox.app",
    "com.microvirt.",
    "com.vmos."
)
