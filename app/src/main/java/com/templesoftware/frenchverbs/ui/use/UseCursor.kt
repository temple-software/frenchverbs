package com.templesoftware.frenchverbs.ui.use

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** Static caret with reduced motion or touch exploration; preference changes are observed. */
@Composable internal fun useCursorAlpha(enabled:Boolean,phaseMillis:Int=USE_CURSOR_PHASE_MILLIS):State<Float> {
    val context=LocalContext.current
    val accessibility=remember(context){context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager}
    fun allowed()=Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f && !accessibility.isTouchExplorationEnabled
    var motion by remember{mutableStateOf(allowed())}
    DisposableEffect(context,accessibility) {
        val observer=object:ContentObserver(Handler(Looper.getMainLooper())){override fun onChange(selfChange:Boolean){motion=allowed()}}
        val listener=AccessibilityManager.TouchExplorationStateChangeListener{motion=allowed()}
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
        accessibility.addTouchExplorationStateChangeListener(listener)
        onDispose{context.contentResolver.unregisterContentObserver(observer);accessibility.removeTouchExplorationStateChangeListener(listener)}
    }
    if(!enabled||!motion)return rememberUpdatedState(if(enabled)1f else 0f)
    val transition=rememberInfiniteTransition(label="Use visual caret")
    return transition.animateFloat(1f,0f,infiniteRepeatable(keyframes {
        durationMillis=phaseMillis*2
        1f at 0;1f at (phaseMillis*4/5);0f at phaseMillis;0f at (phaseMillis*9/5);1f at (phaseMillis*2)
    }),label="Use caret opacity")
}
