package com.templesoftware.frenchverbs

import android.app.UiAutomation
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Outermost rule: release UI-automation freezes even after an assertion fails.
 * ActivityScenario closes the test Activity before this restores system preferences.
 * Hard process termination additionally needs the host runner's finally block.
 */
class PreserveRotationRule : TestRule {
    companion object {
        fun shell(command:String):String = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command).use { fd -> java.io.FileInputStream(fd.fileDescriptor)
                .bufferedReader().use { it.readText().trim() } }
        fun setting(key:String)=shell("settings get system $key")
    }
    override fun apply(base:Statement, description:Description)=object:Statement() {
        override fun evaluate() {
            val auto=setting("accelerometer_rotation")
            val rotation=setting("user_rotation")
            try { base.evaluate() } finally {
                try { check(InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)) }
                finally {
                    try { restore("user_rotation",rotation) }
                    finally { restore("accelerometer_rotation",auto) }
                }
            }
        }
        private fun restore(key:String,value:String) {
            shell(if(value=="null")"settings delete system $key" else "settings put system $key $value")
            check(setting(key)==value) { "Rotation setting was not restored: $key" }
        }
    }
}
