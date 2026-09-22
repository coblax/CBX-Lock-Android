package com.coblax.examlock.config
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
    "/sbin/busybox",
    // Frida instrumentation server — only present if user deliberately deployed it (requires root)
    "/data/local/tmp/frida-server",
    "/data/local/tmp/re.frida.server",
    // Non-standard busybox paths (used by root tools)
    "/data/adb/busybox",
    "/data/local/tmp/busybox",
    // Superuser app artifacts
    "/system/app/Superuser.apk",
    "/system/app/Superuser/Superuser.apk",
    "/system/app/SuperSU/SuperSU.apk"
)

internal val RootPackageNames = listOf(
    // Classic root managers
    "com.topjohnwu.magisk",
    "eu.chainfire.supersu",
    "com.koushikdutta.superuser",
    "com.thirdparty.superuser",
    "com.noshufou.android.su",
    // Modern kernel-level root frameworks (2022+)
    "me.weishu.kernelsu",          // KernelSU Manager
    "me.bmax.apatch",              // APatch Manager
    "io.github.huskydg.magisk",   // Magisk Delta (fork)
    // Hook / injection frameworks
    "org.lsposed.manager",                // LSPosed Manager (runs on Zygisk)
    "de.robv.android.xposed.installer",   // Classic Xposed Installer
    "com.elderdrivers.riru.edxp",         // EdXposed Manager
    // Root cloaking apps — only installed to hide root from detection
    "com.devadvance.rootcloak",           // RootCloak
    "com.devadvance.rootcloakplus",       // RootCloak Plus
    "com.formyhm.hideroot",              // Hide My Root
    "com.amphoras.hidemyroot",           // Hide My Root (alt)
    "com.saurik.substrate"               // Cydia Substrate
)

internal val MagiskIndicatorPaths = listOf(
    // Magisk
    "/sbin/.magisk",
    "/data/adb/magisk",
    "/data/adb/modules",
    "/data/adb/zygisk",
    "/cache/.magisk",
    "/metadata/magisk",
    // KernelSU
    "/data/adb/ksu",
    "/data/adb/ksud",
    // APatch
    "/data/adb/ap"
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
