package com.example.coblaxexamlock.ui.exam

import com.example.coblaxexamlock.runtime.ExternalDisplaySnapshot
import com.example.coblaxexamlock.runtime.MultiWindowModeInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeStaticSecuritySplitTest {
    @Test
    fun fastStaticSecuritySnapshotReadsDisplayAndMultiWindowOnly() {
        var displayReads = 0
        var multiWindowReads = 0

        val snapshot = readRuntimeFastStaticSecuritySnapshot(
            displaySnapshotReader = {
                displayReads += 1
                ExternalDisplaySnapshot(
                    count = 1,
                    infoList = emptyList()
                )
            },
            multiWindowInfoReader = {
                multiWindowReads += 1
                MultiWindowModeInfo(
                    multiWindowApiSupported = true,
                    pictureInPictureApiSupported = true,
                    inMultiWindowMode = true,
                    inPictureInPictureMode = false
                )
            }
        )

        assertEquals(1, displayReads)
        assertEquals(1, multiWindowReads)
        assertEquals(1, snapshot.externalDisplayCount)
        assertEquals(true, snapshot.externalDisplayDetected)
        assertEquals(true, snapshot.multiWindowDetected)
    }

    @Test
    fun screenRecorderSnapshotReadsPackageScanOnlyWhenRequested() {
        var screenRecorderReads = 0

        val snapshot = readRuntimeScreenRecorderSnapshot(
            screenRecorderPackagesReader = {
                screenRecorderReads += 1
                listOf("com.example.recorder")
            }
        )

        assertEquals(1, screenRecorderReads)
        assertEquals(listOf("com.example.recorder"), snapshot.screenRecorderPackages)
    }
}
