package com.templesoftware.frenchverbs

import android.app.UiAutomation
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RotationCleanupTest {
    private fun verify(fail:Boolean) {
        val before=listOf("accelerometer_rotation","user_rotation").associateWith{PreserveRotationRule.setting(it)}
        var frozen=false
        val test=object:Statement(){override fun evaluate(){
            check(InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0))
            frozen=PreserveRotationRule.setting("accelerometer_rotation")=="0"
            if(fail)throw AssertionError("Intentional failure to exercise cleanup")
        }}
        try { PreserveRotationRule().apply(test,Description.EMPTY).evaluate();assertFalse(fail) }
        catch(e:AssertionError) { if(!fail || e.message!="Intentional failure to exercise cleanup")throw e }
        assertTrue("Freeze actually disabled Android automatic rotation",frozen)
        before.forEach{(key,value)->assertEquals(key,value,PreserveRotationRule.setting(key))}
    }
    @Test fun restoresAfterSuccess()=verify(false)
    @Test fun restoresAfterAssertionFailure()=verify(true)
    @Test fun respectsPreexistingUserLock() {
        val test=object:Statement(){override fun evaluate(){
            PreserveRotationRule.shell("wm user-rotation lock 1")
            verify(true)
            assertEquals("0",PreserveRotationRule.setting("accelerometer_rotation"))
            assertEquals("1",PreserveRotationRule.setting("user_rotation"))
        }}
        PreserveRotationRule().apply(test,Description.EMPTY).evaluate()
    }

}
