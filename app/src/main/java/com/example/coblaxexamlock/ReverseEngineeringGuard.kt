package com.example.coblaxexamlock

import android.content.Context
import android.os.Debug
import com.example.coblaxexamlock.nativebridge.NativeBridgeBackendMode
import com.example.coblaxexamlock.nativebridge.NativeBridgeTestControl
import com.example.coblaxexamlock.nativebridge.NativeSecurityBridge
import java.io.File
import java.util.Locale


data class ReverseEngineeringResult(
    val tamperDetected: Boolean,
    val score: Int,
    val strongSignals: List<String>,
    val weakSignals: List<String>
) {
    fun summary(): String {
        val strongSummary = strongSignals.joinToString().ifBlank { "-" }
        val weakSummary = weakSignals.joinToString().ifBlank { "-" }
        return "score=$score | strong=$strongSummary | weak=$weakSummary"
    }
}

object ReverseEngineeringGuard {
    private val mapMarkers = listOf(
        "frida",
        "gum-js-loop",
        "gadget",
        "xposed",
        "substrate",
        "lsposed",
        "lspatch",
        "libxposed",
        "lsplant",
        "zygisk",
        "shamiko",
        "rezygisk",
        // Modern root/hooking frameworks
        "magisk",
        "kernelsu",
        "/data/adb/ksu",
        "/data/adb/ksud",
        "apatch",
        "objection",
        "libriru",
        "/data/adb/riru"
    )

    private val suspiciousClassNames = listOf(
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XC_MethodHook",
        "com.saurik.substrate.MS${'$'}2",
        "com.saurik.substrate.SubstrateHooker",
        "org.lsposed.lspd.core.Main",
        "org.lsposed.lspd.service.LSPSystemServer",
        "com.github.kyuubiran.ezxhelper.EzXHelper",
        "io.github.libxposed.api.XposedInterface",
        "io.github.libxposed.api.XposedModule"
    )

    private val suspiciousPackageNames = listOf(
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "org.lsposed.lspatch",
        "org.meowcat.edxposed.manager",
        "com.saurik.substrate",
        // Magisk manager variants
        "com.topjohnwu.magisk",
        // KernelSU manager
        "me.weishu.kernelsu",
        // APatch manager
        "me.bmax.apatch",
        // Frida server package
        "re.frida.server"
    )

    fun inspect(context: Context): ReverseEngineeringResult {
        val strongSignals = mutableListOf<String>()
        val weakSignals = mutableListOf<String>()

        if (Debug.isDebuggerConnected()) {
            strongSignals.add("debugger")
        }
        if (Debug.waitingForDebugger()) {
            strongSignals.add("debugger_wait")
        }

        val tracerPid = readTracerPid()
        if (tracerPid > 0) {
            strongSignals.add("tracerpid:$tracerPid")
        }

        val mapHits = scanProcMapsForMarkers()
        mapHits.forEach { marker ->
            strongSignals.add("maps:$marker")
        }

        val classHits = detectSuspiciousClasses(context)
        classHits.forEach { className ->
            weakSignals.add("class:$className")
        }

        val packageHits = detectSuspiciousPackages(context)
        packageHits.forEach { packageName ->
            weakSignals.add("pkg:$packageName")
        }

        val score = strongSignals.size * 2 + weakSignals.size
        val tamperDetected = strongSignals.isNotEmpty() || weakSignals.size >= 2

        return ReverseEngineeringResult(
            tamperDetected = tamperDetected,
            score = score,
            strongSignals = strongSignals,
            weakSignals = weakSignals
        )
    }

    internal object ParityAccess {
        fun readTracerPidWithBackend(backendMode: NativeBridgeBackendMode): Int =
            NativeBridgeTestControl.withBackendMode(backendMode) {
                readTracerPid()
            }

        fun readTracerPidReference(): Int = readTracerPidKotlin()

        fun scanProcMapsWithBackend(backendMode: NativeBridgeBackendMode): Set<String> =
            NativeBridgeTestControl.withBackendMode(backendMode) {
                scanProcMapsForMarkers()
            }

        fun scanProcMapsReference(): Set<String> = scanProcMapsForMarkersKotlin()

        fun scanProcMapsLineReference(line: String): Set<String> =
            scanProcMapsLineForMarkers(line)

        fun inspectWithBackend(
            context: Context,
            backendMode: NativeBridgeBackendMode
        ): ReverseEngineeringResult = NativeBridgeTestControl.withBackendMode(backendMode) {
            inspect(context)
        }
    }

    private fun readTracerPid(): Int {
        return NativeSecurityBridge.readTracerPid {
            readTracerPidKotlin()
        }
    }

    private fun readTracerPidKotlin(): Int {
        val statusFile = File("/proc/self/status")
        if (!statusFile.canRead()) {
            return 0
        }
        return runCatching {
            statusFile.useLines { lines ->
                lines.firstOrNull { it.startsWith("TracerPid:") }
            }?.substringAfter("TracerPid:")?.trim()?.toIntOrNull() ?: 0
        }.getOrDefault(0)
    }

    private fun scanProcMapsForMarkers(): Set<String> {
        return NativeSecurityBridge.scanProcMaps(mapMarkers) {
            scanProcMapsForMarkersKotlin()
        }
    }

    private fun scanProcMapsForMarkersKotlin(): Set<String> {
        val mapsFile = File("/proc/self/maps")
        if (!mapsFile.canRead()) {
            return emptySet()
        }
        val hits = linkedSetOf<String>()
        runCatching {
            mapsFile.useLines { lines ->
                for (line in lines) {
                    hits.addAll(scanProcMapsLineForMarkers(line))
                    if (hits.size == mapMarkers.size) {
                        break
                    }
                }
            }
        }
        return hits
    }

    private fun scanProcMapsLineForMarkers(line: String): Set<String> {
        val lower = line.lowercase(Locale.US)
        return mapMarkers
            .filterTo(linkedSetOf()) { marker -> marker in lower }
    }

    private fun detectSuspiciousClasses(context: Context): List<String> {
        val classLoader = context.classLoader
        return suspiciousClassNames.filter { className ->
            runCatching {
                Class.forName(className, false, classLoader)
            }.isSuccess
        }
    }

    private fun detectSuspiciousPackages(context: Context): List<String> {
        val packageManager = context.packageManager
        return suspiciousPackageNames.filter { packageName ->
            runCatching { packageManager.getApplicationInfo(packageName, 0) }.isSuccess
        }
    }
}
