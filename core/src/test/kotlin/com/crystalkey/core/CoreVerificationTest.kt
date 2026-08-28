package com.crystalkey.core

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The same checks the standalone runner executes, wired into the normal Gradle
 * test task so CI and Android Studio run them too.
 */
class CoreVerificationTest {
    @Test
    fun `core verification suite passes`() {
        val report = runCoreVerification()
        assertTrue(
            report.failures.isEmpty(),
            "core verification failed:\n" + report.failures.joinToString("\n") { "  • $it" },
        )
        assertTrue(report.passed >= 30, "expected at least 30 checks, ran ${report.passed}")
    }
}
