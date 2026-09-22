package com.coblax.examlock.config
import com.coblax.examlock.RuntimeStringDecoder
import com.coblax.examlock.model.ExamKeyboardPolicy
import com.coblax.examlock.model.ExamKeyboardRule
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

private val RootBinaryIndicatorPathsObfuscated = listOf(
    "XAAKAAcWHlwRGh1cAAY=",  // /system/bin/su
    "XAAKAAcWHlwLERodXAAG",  // /system/xbin/su
    "XAARGh1cAAY=",  // /sbin/su
    "XAUWHRccAVwRGh1cAAY=",  // /vendor/bin/su
    "XAAGXBEaHVwABg==",  // /su/bin/su
    "XAAKAAcWHlwRGh1cXRYLB1wABg==",  // /system/bin/.ext/su
    "XAAKAAcWHlwRGh1cXRYLB1xdAAY=",  // /system/bin/.ext/.su
    "XAAKAAcWHlwRGh1cFRIaHwASFRZcAAY=",  // /system/bin/failsafe/su
    "XAAKAAcWHlwLERodXBcSFh4cHQAG",  // /system/xbin/daemonsu
    "XAAKAAcWHlwRGh1cFxIWHhwdAAY=",  // /system/bin/daemonsu
    "XBcSBxJcHxwQEh9cAAY=",  // /data/local/su
    "XBcSBxJcHxwQEh9cERodXAAG",  // /data/local/bin/su
    "XBcSBxJcHxwQEh9cCxEaHVwABg==",  // /data/local/xbin/su
    "XAAKAAcWHlwRGh1cEQYAChEcCw==",  // /system/bin/busybox
    "XAAKAAcWHlwLERodXBEGAAoRHAs=",  // /system/xbin/busybox
    "XAUWHRccAVwRGh1cEQYAChEcCw==",  // /vendor/bin/busybox
    "XAAKAAcWHlwAERodXBEGAAoRHAs=",  // /system/sbin/busybox
    "XAARGh1cEQYAChEcCw==",  // /sbin/busybox
    // Frida instrumentation server — only present if user deliberately deployed it (requires root)
    "XBcSBxJcHxwQEh9cBx4DXBUBGhcSXgAWAQUWAQ==",  // /data/local/tmp/frida-server
    "XBcSBxJcHxwQEh9cBx4DXAEWXRUBGhcSXQAWAQUWAQ==",  // /data/local/tmp/re.frida.server
    // Non-standard busybox paths (used by root tools)
    "XBcSBxJcEhcRXBEGAAoRHAs=",  // /data/adb/busybox
    "XBcSBxJcHxwQEh9cBx4DXBEGAAoRHAs=",  // /data/local/tmp/busybox
    // Superuser app artifacts
    "XAAKAAcWHlwSAwNcIAYDFgEGABYBXRIDGA==",  // /system/app/Superuser.apk
    "XAAKAAcWHlwSAwNcIAYDFgEGABYBXCAGAxYBBgAWAV0SAxg=",  // /system/app/Superuser/Superuser.apk
    "XAAKAAcWHlwSAwNcIAYDFgEgJlwgBgMWASAmXRIDGA=="  // /system/app/SuperSU/SuperSU.apk
)

internal val RootBinaryIndicatorPaths: List<String> by lazy {
    RootBinaryIndicatorPathsObfuscated.map { RuntimeStringDecoder.decodeBase64Xor(it) }
}

private val RootPackageNamesObfuscated = listOf(
    // Classic root managers
    "EBweXQccAxkcGx0EBl0eEhQaABg=",  // com.topjohnwu.magisk
    "FgZdEBsSGh0VGgEWXQAGAxYBAAY=",  // eu.chainfire.supersu
    "EBweXRgcBgAbGhgXBgcHEl0ABgMWAQYAFgE=",  // com.koushikdutta.superuser
    "EBweXQcbGgEXAxIBBwpdAAYDFgEGABYB",  // com.thirdparty.superuser
    "EBweXR0cABsGFRwGXRIdFwEcGhddAAY=",  // com.noshufou.android.su
    // Modern kernel-level root frameworks (2022+)
    "HhZdBBYaABsGXRgWAR0WHwAG",          // KernelSU Manager
    "HhZdER4SC10SAxIHEBs=",              // APatch Manager
    "GhxdFBoHGwYRXRsGABgKFxRdHhIUGgAY",   // Magisk Delta (fork)
    // Hook / injection frameworks
    "HAEUXR8AAxwAFhddHhIdEhQWAQ==",                // LSPosed Manager (runs on Zygisk)
    "FxZdARwRBV0SHRcBHBoXXQsDHAAWF10aHQAHEh8fFgE=",   // Classic Xposed Installer
    "EBweXRYfFxYBFwEaBRYBAF0BGgEGXRYXCwM=",         // EdXposed Manager
    // Root cloaking apps — only installed to hide root from detection
    "EBweXRcWBRIXBRIdEBZdARwcBxAfHBIY",           // RootCloak
    "EBweXRcWBRIXBRIdEBZdARwcBxAfHBIYAx8GAA==",       // RootCloak Plus
    "EBweXRUcAR4KGx5dGxoXFgEcHAc=",              // Hide My Root
    "EBweXRIeAxscARIAXRsaFxYeCgEcHAc=",           // Hide My Root (alt)
    "EBweXQASBgEaGF0ABhEABwESBxY="               // Cydia Substrate
)

internal val RootPackageNames: List<String> by lazy {
    RootPackageNamesObfuscated.map { RuntimeStringDecoder.decodeBase64Xor(it) }
}

private val MagiskIndicatorPathsObfuscated = listOf(
    // Magisk
    "XAARGh1cXR4SFBoAGA==",  // /sbin/.magisk
    "XBcSBxJcEhcRXB4SFBoAGA==",  // /data/adb/magisk
    "XBcSBxJcEhcRXB4cFwYfFgA=",  // /data/adb/modules
    "XBcSBxJcEhcRXAkKFBoAGA==",  // /data/adb/zygisk
    "XBASEBsWXF0eEhQaABg=",  // /cache/.magisk
    "XB4WBxIXEgcSXB4SFBoAGA==",  // /metadata/magisk
    // KernelSU
    "XBcSBxJcEhcRXBgABg==",  // /data/adb/ksu
    "XBcSBxJcEhcRXBgABhc=",  // /data/adb/ksud
    // APatch
    "XBcSBxJcEhcRXBID"  // /data/adb/ap
)

internal val MagiskIndicatorPaths: List<String> by lazy {
    MagiskIndicatorPathsObfuscated.map { RuntimeStringDecoder.decodeBase64Xor(it) }
}

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
    "sdk_gphone",
    "vsemu",
    "ttVM_Hdragon",
    "vbox86p",
    "nox"
)

internal val VirtualModelTokens = listOf(
    "Emulator",
    "Android SDK built for x86",
    "google_sdk",
    "sdk_gphone",
    "Droid4X",
    "Andy",
    "TiantianVM",
    "AOSP on IA Emulator"
)

internal val VirtualManufacturerTokens = listOf(
    "Genymotion",
    "BlueStacks",
    "Nox",
    "MuMu",
    "Xamarin",
    "Andy",
    "LDPlayer",
    "MEmu",
    "microvirt",
    "Phoenix",
    "Tencent"
)

internal val VirtualProductTokens = listOf(
    "sdk",
    "sdk_gphone",
    "emulator",
    "vbox86",
    "simulator",
    "nox",
    "andy",
    "Droid4X",
    "ldplayer"
)

internal val VirtualHardwareTokens = listOf(
    "goldfish",
    "ranchu",
    "vbox86",
    "nox",
    "ttvm",
    "android_x86",
    "memuplusqemu",
    "intel"
)

internal val VirtualBoardTokens = listOf(
    "unknown",
    "goldfish_x86_64",
    "windows"
)

internal val VirtualQemuFiles = listOf(
    "/dev/qemu_pipe",
    "/dev/qemu_trace",
    "/dev/goldfish_pipe",
    "/dev/socket/qemud",
    "/system/bin/qemu-props",
    "/system/bin/qemud",
    "/system/bin/androVM-prop",
    "/system/bin/microvirt-prop",
    "/system/bin/nox-prop",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/system/lib64/libc_malloc_debug_qemu.so",
    "/system/lib/libldutils.so",
    "/system/bin/ldinit",
    "/system/bin/ldmountsf"
)

// System properties whose mere PRESENCE indicates an emulator. These do not exist on
// real hardware, so a non-blank value is a strong signal.
//
// Note: ro.hardware.audio.primary, qemu.hw.mainkeys and ro.bootimage.build.fingerprint
// were previously in this list, but they are also present on many REAL devices
// (e.g. Samsung Galaxy A55 sets ro.bootimage.build.fingerprint like every AOSP build,
// and qemu.hw.mainkeys ships on plenty of real phones despite its name). Treating their
// presence as an emulator signal produced false "Emulator detected" blocks on genuine
// phones, so they now only count when their VALUE contains an emulator token
// (see VirtualValueSystemProperties below).
internal val VirtualPresenceSystemPropertyKeys = listOf(
    "init.svc.qemud",
    "init.svc.qemu-props",
    "qemu.sf.fake_camera",
    "ro.boot.qemu.avd_name",
    "ro.product.device.goldfish",
    "ro.ndk_translation.version"
)

// System properties that also exist on real devices; only an emulator-shaped VALUE counts.
// Each pair is (propertyKey, tokens that must appear in the value to flag it).
internal val VirtualValueSystemProperties = listOf(
    "ro.hardware.audio.primary" to listOf("goldfish", "ranchu", "emulator"),
    "ro.bootimage.build.fingerprint" to listOf(
        "generic", "emulator", "sdk_gphone", "vbox86", "genymotion", "ttvm", "nox"
    )
)

internal val EmulatorPackagePrefixes = listOf(
    "com.bluestacks.",
    "com.genymotion.",
    "com.nox.mopen.app",
    "com.bignox.app",
    "com.microvirt.",
    "com.vmos.",
    "com.ldmnq.",
    "com.lbe.parallel.",
    "com.excelliance.dualaid",
    "com.ludashi.",
    "me.weishu.exp",
    "com.tencent.gameloop",
    "com.x8bit.biern",
    "com.andydevelopers."
)
