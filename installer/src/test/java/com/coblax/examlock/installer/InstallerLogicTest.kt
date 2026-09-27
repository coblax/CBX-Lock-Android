package com.coblax.examlock.installer

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallerLogicTest {
    private val official = "E1FD5D3526D50896D2494109C1432E7FA145C5A1134F067AA6A851461EE116E7"
    private val bundle = BundledCbx(
        size = 7_000_000L,
        sha256 = "AB".repeat(32),
        versionCode = 372,
        versionName = "3.2.52",
        minSdk = 24,
        abis = setOf("arm64-v8a", "armeabi-v7a", "x86_64")
    )
    private val vivo = DeviceFacts(
        manufacturer = "vivo",
        brand = "vivo",
        model = "V2310",
        device = "V2310",
        sdkInt = 33,
        release = "13",
        supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
        freeBytes = 8_000_000_000L,
        romDisplay = "PD2310F_EX_A_13.1.9.2"
    )
    private val intact = BundleInspection(actualSha256 = "ab".repeat(32), parsedByPhone = true, certSha256 = official)
    private val indonesian = Texts(indonesian = true)

    private fun check(
        device: DeviceFacts = vivo,
        installed: InstalledCbx? = null,
        inspection: BundleInspection = intact
    ) = evaluatePrechecks(bundle, device, installed, inspection, official)

    @Test
    fun healthyPhoneHasNoProblems() {
        assertEquals(emptyList<PrecheckProblem>(), check())
    }

    @Test
    fun bundleMetadataParsesWhatTheBuildWrites() {
        val parsed = BundledCbx.parse(
            mapOf(
                "size" to "6964462",
                "sha256" to "db41" + "0".repeat(60),
                "versionCode" to "372",
                "versionName" to "3.2.52",
                "minSdk" to "24",
                "abis" to "arm64-v8a,armeabi-v7a,x86_64"
            )
        )
        assertEquals(6_964_462L, parsed.size)
        assertEquals("DB41" + "0".repeat(60), parsed.sha256)
        assertEquals(setOf("arm64-v8a", "armeabi-v7a", "x86_64"), parsed.abis)
    }

    @Test
    fun oldAndroidAndForeignCpuAreFoundBeforeInstalling() {
        val old = vivo.copy(sdkInt = 23, release = "6.0", supportedAbis = listOf("mips"))
        val problems = check(device = old, inspection = intact.copy(parsedByPhone = false))

        assertTrue(PrecheckProblem.AndroidTooOld in problems)
        assertTrue(PrecheckProblem.CpuNotSupported in problems)
        // Android 6 cannot parse a minSdk 24 APK; that is not a separate phone problem.
        assertFalse(PrecheckProblem.UnreadableOnThisPhone in problems)
    }

    @Test
    fun damagedFileIsToldApartFromAPhoneThatCannotReadAnIntactOne() {
        assertEquals(
            listOf(PrecheckProblem.BundleDamaged),
            check(inspection = intact.copy(actualSha256 = "00".repeat(32), parsedByPhone = false))
        )
        assertEquals(
            listOf(PrecheckProblem.UnreadableOnThisPhone),
            check(inspection = intact.copy(parsedByPhone = false, certSha256 = null))
        )
    }

    @Test
    fun aBundleSignedByAnotherKeyIsNeverOfficial() {
        assertEquals(
            listOf(PrecheckProblem.BundleNotOfficial),
            check(inspection = intact.copy(certSha256 = "11".repeat(32)))
        )
    }

    @Test
    fun testBuildOnThePhoneMustBeUninstalledFirst() {
        val debugBuild = InstalledCbx(versionCode = 372, versionName = "3.2.52", certSha256 = "22".repeat(32))
        assertEquals(listOf(PrecheckProblem.OtherCbxSignature), check(installed = debugBuild))
    }

    @Test
    fun newerOfficialBuildOnThePhoneIsADowngrade() {
        val newer = InstalledCbx(versionCode = 380, versionName = "3.3.0", certSha256 = official)
        assertEquals(listOf(PrecheckProblem.NewerCbxInstalled), check(installed = newer))
        val older = newer.copy(versionCode = 360)
        assertEquals(emptyList<PrecheckProblem>(), check(installed = older))
    }

    @Test
    fun lowStorageIsCaughtWithRoomToSpare() {
        assertEquals(
            listOf(PrecheckProblem.NotEnoughStorage),
            check(device = vivo.copy(freeBytes = 25_000_000L))
        )
        // Unknown free space (-1) is not treated as full.
        assertEquals(emptyList<PrecheckProblem>(), check(device = vivo.copy(freeBytes = -1L)))
    }

    @Test
    fun installErrorCodeIsTakenFromTheStatusMessage() {
        assertEquals(
            "INSTALL_PARSE_FAILED_NO_CERTIFICATES",
            installErrorToken("INSTALL_PARSE_FAILED_NO_CERTIFICATES: Failed to collect certificates from /data/app/vmdl1.tmp/base.apk")
        )
        assertEquals(null, installErrorToken("Session was abandoned"))
    }

    @Test
    fun parseFailureIsReportedWithItsExactCode() {
        val explanation = explainInstallFailure(
            SessionStatus.Invalid,
            "INSTALL_PARSE_FAILED_NOT_APK: Failed to parse /data/app/vmdl.tmp/base.apk",
            indonesian
        )
        assertEquals("INSTALL_PARSE_FAILED_NOT_APK", explanation.code)
        assertEquals("HP ini tidak bisa membaca paket CBX Lock", explanation.title)
        assertTrue(explanation.needsAdmin)
    }

    @Test
    fun studentFixableFailuresAreExplainedButNotReported() {
        val cancelled = explainInstallFailure(SessionStatus.Aborted, null, indonesian)
        assertFalse(cancelled.needsAdmin)
        assertEquals("STATUS_FAILURE_ABORTED", cancelled.code)

        val conflict = explainInstallFailure(
            SessionStatus.Conflict,
            "INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package com.coblax.examlock signatures do not match",
            indonesian
        )
        assertEquals("INSTALL_FAILED_UPDATE_INCOMPATIBLE", conflict.code)
        assertFalse(conflict.needsAdmin)
    }

    /** Seen on the emulator: "Don't install app" on Play Protect arrives as an abort. */
    @Test
    fun decliningPlayProtectSaysWhichButtonToPick() {
        val declined = explainInstallFailure(
            SessionStatus.Aborted,
            "INSTALL_FAILED_VERIFICATION_FAILURE: Install not allowed for file:///data/app/vmdl997602913.tmp",
            indonesian
        )
        assertEquals("INSTALL_FAILED_VERIFICATION_FAILURE", declined.code)
        assertEquals("Google Play Protect menahan pemasangan", declined.title)
        assertTrue(declined.advice.contains("Instal tanpa memindai"))
        assertFalse(declined.needsAdmin)

        // Play Protect refusing on its own is a block the admins must hear about.
        val blocked = explainInstallFailure(SessionStatus.Blocked, "INSTALL_FAILED_VERIFICATION_FAILURE: blocked", indonesian)
        assertEquals("Pemasangan diblokir oleh HP", blocked.title)
        assertTrue(blocked.needsAdmin)
    }

    @Test
    fun blockedByThePhoneIsNamedAsSuch() {
        val blocked = explainInstallFailure(SessionStatus.Blocked, null, indonesian)
        assertEquals("Pemasangan diblokir oleh HP", blocked.title)
        assertEquals("STATUS_FAILURE_BLOCKED", blocked.code)
    }

    @Test
    fun everyPrecheckHasAnExplanationInBothLanguages() {
        PrecheckProblem.entries.forEach { problem ->
            listOf(Texts(indonesian = true), Texts(indonesian = false)).forEach { texts ->
                val explanation = explainPrecheck(problem, bundle, vivo, texts)
                assertEquals(problem.code, explanation.code)
                assertTrue(explanation.title.isNotBlank() && explanation.advice.isNotBlank())
            }
        }
    }

    @Test
    fun englishPhonesGetEnglishEverythingElseIndonesian() {
        assertEquals("Installation failed", explainInstallFailure(99, null, Texts.forLocale(Locale.US)).title)
        assertEquals("Pemasangan gagal", explainInstallFailure(99, null, Texts.forLocale(Locale("in", "ID"))).title)
    }

    @Test
    fun reportNamesThePhoneTheFileAndTheExactError() {
        val text = formatInstallReport(
            InstallReport(
                reportId = "IR-7F3A2C",
                stage = "install",
                code = "INSTALL_PARSE_FAILED_NOT_APK",
                studentTitle = "HP ini tidak bisa membaca paket CBX Lock",
                needsAdmin = true,
                statusName = SessionStatus.name(SessionStatus.Invalid),
                statusMessage = "INSTALL_PARSE_FAILED_NOT_APK: Failed to parse",
                legacyStatus = -100,
                bundle = bundle,
                device = vivo.copy(
                    securityPatch = "2024-05-01",
                    fingerprint = "vivo/PD2310F_EX/V2310:13/TP1A.220624.014/compiler05141:user/release-keys",
                    hardware = "mt6768",
                    totalRamBytes = 4L * 1024 * 1024 * 1024,
                    locale = "in-ID",
                    timezone = "Asia/Jakarta",
                    installerSource = "from com.whatsapp via com.android.packageinstaller",
                    canInstallPackages = true
                ),
                installed = null,
                inspection = intact,
                sessionId = 1234,
                installerVersion = "3.2.52",
                timestamp = "2026-09-27 10:15:00 +0700"
            )
        )

        assertTrue(text, text.startsWith("[ADMIN ACTION]"))
        assertTrue(text, text.contains("Report ID: IR-7F3A2C"))
        assertTrue(text, text.contains("Student saw: HP ini tidak bisa membaca paket CBX Lock"))
        assertTrue(text, text.contains("Stage: install | Code: INSTALL_PARSE_FAILED_NOT_APK"))
        assertTrue(text, text.contains("Status: STATUS_FAILURE_INVALID | legacy -100"))
        assertTrue(text, text.contains("Android said: INSTALL_PARSE_FAILED_NOT_APK: Failed to parse"))
        assertTrue(text, text.contains("Phone: vivo V2310 (brand vivo, device V2310, hw mt6768)"))
        assertTrue(text, text.contains("Android: 13 (API 33) | patch 2024-05-01"))
        assertTrue(text, text.contains("Build: vivo/PD2310F_EX"))
        assertTrue(text, text.contains("RAM 4.0 GB"))
        assertTrue(text, text.contains("May install apps: yes | restrictions: none"))
        assertTrue(text, text.contains("Installer came from: from com.whatsapp via com.android.packageinstaller | in-ID | Asia/Jakarta"))
        assertTrue(text, text.contains("Bundled CBX: v3.2.52 (372)"))
        assertTrue(text, text.contains("parsed by phone yes"))
        assertTrue(text, text.contains("CBX on phone: none"))
        assertTrue(text, text.contains("Session: 1234"))
        assertTrue(text.length <= TelegramMessageLimit)
    }

    /** The student can fix these alone, but the admins still see every failure. */
    @Test
    fun studentFixableFailuresAreReportedAsInfo() {
        val text = formatInstallReport(
            InstallReport(
                reportId = "IR-000001",
                stage = "precheck",
                code = "storage_low",
                studentTitle = "Memori HP tidak cukup",
                needsAdmin = false,
                problems = listOf("storage_low"),
                bundle = bundle,
                device = vivo,
                installed = InstalledCbx(365, "3.2.45", official, enabled = false, installerSource = "via com.android.packageinstaller"),
                installerVersion = "3.2.52",
                timestamp = "2026-09-27 10:15:00 +0700"
            )
        )
        assertTrue(text, text.startsWith("[INFO]"))
        assertTrue(text, text.contains("Prechecks: storage_low"))
        assertTrue(text, text.contains("CBX on phone: v3.2.45 (365)"))
        assertTrue(text, text.contains("DISABLED"))
        assertTrue(text, text.contains("installed by via com.android.packageinstaller"))
    }

    @Test
    fun aCrashReportKeepsItsStackTraceWithinTelegramsLimit() {
        val text = formatInstallReport(
            InstallReport(
                reportId = "IR-000002",
                stage = "installer",
                code = "installer_crashed",
                studentTitle = "CBX Installer berhenti tiba-tiba",
                needsAdmin = true,
                bundle = null,
                device = vivo,
                installed = null,
                extra = "java.lang.IllegalStateException: boom\n" + "\tat x.y.z(File.kt:1)\n".repeat(400),
                installerVersion = "3.2.52",
                timestamp = "2026-09-27 10:15:00 +0700"
            )
        )
        assertTrue(text, text.contains("Bundled CBX: unreadable"))
        assertTrue(text, text.contains("IllegalStateException: boom"))
        assertTrue(text.length <= TelegramMessageLimit)
    }

    @Test
    fun devicePolicyThatForbidsInstallingIsFoundFirst() {
        val managed = vivo.copy(installRestrictions = listOf("no_install_unknown_sources"))
        val problems = check(device = managed)
        assertEquals(listOf(PrecheckProblem.InstallBlockedByPolicy), problems)
        val explanation = explainPrecheck(PrecheckProblem.InstallBlockedByPolicy, bundle, managed, indonesian)
        assertTrue(explanation.advice.contains("no_install_unknown_sources"))
        assertTrue(explanation.needsAdmin)
    }

    @Test
    fun aFrozenCbxIsCaughtBeforeItLooksInstalled() {
        val frozen = InstalledCbx(372, "3.2.52", official, enabled = false)
        assertEquals(listOf(PrecheckProblem.CbxDisabled), check(installed = frozen))
    }

    @Test
    fun everyInstallerEventHasAnExplanationInBothLanguages() {
        InstallerEvent.entries.forEach { event ->
            listOf(Texts(indonesian = true), Texts(indonesian = false)).forEach { texts ->
                val explanation = explainEvent(event, texts)
                assertEquals(event.code, explanation.code)
                assertTrue(explanation.title.isNotBlank() && explanation.advice.isNotBlank())
            }
        }
        // Only the student can fix a missing permission; the rest need an admin.
        assertFalse(explainEvent(InstallerEvent.PermissionNotGranted, indonesian).needsAdmin)
        assertTrue(explainEvent(InstallerEvent.NoResult, indonesian).needsAdmin)
    }

    @Test
    fun reportIdsAreShortAndReadable() {
        val id = newReportId(java.util.Random(7))
        assertTrue(id, Regex("IR-[0-9A-F]{6}").matches(id))
    }
}
