package com.deepseekclone.app.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private val checker = UpdateChecker(currentVersion = "1.0.0")

    @Test
    fun `版本比较规则`() {
        assertTrue(checker.compareVersion("1.1.0", "1.0.0") > 0)
        assertEquals(0, checker.compareVersion("1.0.0", "1.0.0"))
        assertTrue(checker.compareVersion("1.0.0", "1.1.0") < 0)
        assertTrue(checker.compareVersion("1.2.10", "1.2.9") > 0)
        assertTrue(checker.compareVersion("2", "1.9.9") > 0)
        assertEquals(0, checker.compareVersion("1.0", "1.0.0"))
    }
}
