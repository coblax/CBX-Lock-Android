package com.example.coblaxexamlock.runtime

internal fun buildScreenRecorderRuntimeEvidence(
    packages: List<String>,
    violationCount: Int
): String = buildList {
    add("Detection method: known_package lookup + keyword_scan package scan")
    add("Detected package count: ${packages.size}")
    add("Runtime violation count: $violationCount")
    add("Detected packages:")
    if (packages.isEmpty()) {
        add("- no visible recorder package detected")
    } else {
        packages.forEach { packageLabel ->
            add("- ${packageLabel.ifBlank { "-" }}")
        }
    }
}.joinToString("\n")

internal fun buildDisplayMirrorRuntimeEvidence(
    externalDisplayCount: Int,
    externalDisplayInfoList: List<ExternalDisplayInfo>,
    violationCount: Int
): String = buildList {
    val resolvedDisplayCount = maxOf(externalDisplayCount, externalDisplayInfoList.size)
    add("Detection method: DisplayManager.getDisplays")
    add("Blocking definition: external display count > 0")
    add("External display count: $resolvedDisplayCount")
    add("Runtime violation count: $violationCount")
    add("External displays:")
    if (externalDisplayInfoList.isEmpty()) {
        add("-")
    } else {
        externalDisplayInfoList.forEachIndexed { index, display ->
            add(
                "- [$index] id=${display.displayId} | " +
                    "name=${display.name.ifBlank { "-" }} | " +
                    "state=${displayStateLabel(display.state)} | " +
                    "flagsRaw=${display.flags} | " +
                    "flags=${displayFlagsLabel(display.flags)}"
            )
        }
    }
}.joinToString("\n")

internal fun buildMultiWindowRuntimeEvidence(
    modeInfo: MultiWindowModeInfo,
    runtimeDetected: Boolean,
    violationCount: Int
): String = buildList {
    add("Detection method: Activity.isInMultiWindowMode + Activity.isInPictureInPictureMode")
    add("Multi-window API >= 24 supported: ${yesNo(modeInfo.multiWindowApiSupported)}")
    add("PiP API >= 26 supported: ${yesNo(modeInfo.pictureInPictureApiSupported)}")
    add("isInMultiWindowMode: ${yesNo(modeInfo.inMultiWindowMode)}")
    add("isInPictureInPictureMode: ${yesNo(modeInfo.inPictureInPictureMode)}")
    add("isInAnySplitMode: ${yesNo(modeInfo.inAnySplitMode)}")
    add("Runtime combined state: ${yesNo(runtimeDetected)}")
    add("Runtime violation count: $violationCount")
}.joinToString("\n")

internal fun buildVpnRuntimeEvidence(
    transportLabel: String,
    interfaceName: String,
    bypassActive: Boolean,
    bypassTampered: Boolean
): String = buildList {
    add("Transport: ${transportLabel.ifBlank { "-" }}")
    add("Interface: ${interfaceName.ifBlank { "-" }}")
    add("VPN bypass active: ${yesNo(bypassActive)}")
    add("VPN bypass tampered: ${yesNo(bypassTampered)}")
}.joinToString("\n")

private fun yesNo(value: Boolean): String = if (value) "Ya" else "Tidak"
