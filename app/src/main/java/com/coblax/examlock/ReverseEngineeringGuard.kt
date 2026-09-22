package com.coblax.examlock

import android.content.Context
import android.os.Debug
import com.coblax.examlock.nativebridge.NativeBridgeBackendMode
import com.coblax.examlock.nativebridge.NativeBridgeTestControl
import com.coblax.examlock.nativebridge.NativeSecurityBridge
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
    private val mapMarkersObfuscated = listOf(
        "FQEaFxI=",  // frida
        "FAYeXhkAXh8cHAM=",  // gum-js-loop
        "FBIXFBYH",  // gadget
        "CwMcABYX",  // xposed
        "AAYRAAcBEgcW",  // substrate
        "HwADHAAWFw==",  // lsposed
        "HwADEgcQGw==",  // lspatch
        "HxoRCwMcABYX",  // libxposed
        "HwADHxIdBw==",  // lsplant
        "CQoUGgAY",  // zygisk
        "ABsSHhoYHA==",  // shamiko
        "ARYJChQaABg=",  // rezygisk
        // Modern root/hooking frameworks
        "HhIUGgAY",  // magisk
        "GBYBHRYfAAY=",  // kernelsu
        "XBcSBxJcEhcRXBgABg==",  // /data/adb/ksu
        "XBcSBxJcEhcRXBgABhc=",  // /data/adb/ksud
        "EgMSBxAb",  // apatch
        "HBEZFhAHGhwd",  // objection
        "HxoRARoBBg==",  // libriru
        "XBcSBxJcEhcRXAEaAQY="  // /data/adb/riru
    )

    private val mapMarkers: List<String> by lazy {
        mapMarkersObfuscated.map { RuntimeStringDecoder.decodeBase64Xor(it) }
    }

    private val suspiciousClassNamesObfuscated = listOf(
        "FxZdARwRBV0SHRcBHBoXXQsDHAAWF10rAxwAFhcxARoXFBY=",  // de.robv.android.xposed.XposedBridge
        "FxZdARwRBV0SHRcBHBoXXQsDHAAWF10rMCw+FgcbHBc7HBwY",  // de.robv.android.xposed.XC_MethodHook
        "EBweXQASBgEaGF0ABhEABwESBxZdPiBXQQ==",  // com.saurik.substrate.MS$2
        "EBweXQASBgEaGF0ABhEABwESBxZdIAYRAAcBEgcWOxwcGBYB",  // com.saurik.substrate.SubstrateHooker
        "HAEUXR8AAxwAFhddHwADF10QHAEWXT4SGh0=",  // org.lsposed.lspd.core.Main
        "HAEUXR8AAxwAFhddHwADF10AFgEFGhAWXT8gIyAKAAcWHiAWAQUWAQ==",  // org.lsposed.lspd.service.LSPSystemServer
        "EBweXRQaBxsGEV0YCgYGERoBEh1dFgkLGxYfAxYBXTYJKzsWHwMWAQ==",  // com.github.kyuubiran.ezxhelper.EzXHelper
        "GhxdFBoHGwYRXR8aEQsDHAAWF10SAxpdKwMcABYXOh0HFgEVEhAW",  // io.github.libxposed.api.XposedInterface
        "GhxdFBoHGwYRXR8aEQsDHAAWF10SAxpdKwMcABYXPhwXBh8W"  // io.github.libxposed.api.XposedModule
    )

    private val suspiciousClassNames: List<String> by lazy {
        suspiciousClassNamesObfuscated.map { RuntimeStringDecoder.decodeBase64Xor(it) }
    }

    private val suspiciousPackageNamesObfuscated = listOf(
        "FxZdARwRBV0SHRcBHBoXXQsDHAAWF10aHQAHEh8fFgE=",  // de.robv.android.xposed.installer
        "HAEUXR8AAxwAFhddHhIdEhQWAQ==",  // org.lsposed.manager
        "HAEUXR8AAxwAFhddHwADEgcQGw==",  // org.lsposed.lspatch
        "HAEUXR4WHAQQEgddFhcLAxwAFhddHhIdEhQWAQ==",  // org.meowcat.edxposed.manager
        "EBweXQASBgEaGF0ABhEABwESBxY=",  // com.saurik.substrate
        // Magisk manager variants
        "EBweXQccAxkcGx0EBl0eEhQaABg=",  // com.topjohnwu.magisk
        // KernelSU manager
        "HhZdBBYaABsGXRgWAR0WHwAG",  // me.weishu.kernelsu
        // APatch manager
        "HhZdER4SC10SAxIHEBs=",  // me.bmax.apatch
        // Frida server package
        "ARZdFQEaFxJdABYBBRYB"  // re.frida.server
    )

    private val suspiciousPackageNames: List<String> by lazy {
        suspiciousPackageNamesObfuscated.map { RuntimeStringDecoder.decodeBase64Xor(it) }
    }

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
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        packageName,
                        android.content.pm.PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(packageName, 0)
                }
            }.isSuccess
        }
    }
}
