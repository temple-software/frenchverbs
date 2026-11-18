package com.templesoftware.frenchverbs.ui.use

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.templesoftware.frenchverbs.R

/** Local mechanical click, loaded before entry and retained across Activity recreation. */
internal class UseKeyClick(context:Context) {
    private val context=context.applicationContext
    private val audio=this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool=SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    internal var ready by mutableStateOf(false);private set
    internal val loadingStarted=android.os.SystemClock.elapsedRealtime()
    internal var loadedAfterMillis=-1L;private set
    internal var attempts=0;private set
    internal var starts=0;private set
    private var active=false;private var released=false
    private val streams=ArrayDeque<Int>()
    private val sample:Int
    init {pool.setOnLoadCompleteListener{_,_,status->ready=status==0;loadedAfterMillis=android.os.SystemClock.elapsedRealtime()-loadingStarted};sample=pool.load(this.context,R.raw.use_key_click,1)}
    fun setActive(value:Boolean){active=value;if(!value&&!released){streams.forEach(pool::stop);streams.clear()}}
    fun play():Int {
        attempts++
        if(!active||released||!ready||audio.ringerMode!=AudioManager.RINGER_MODE_NORMAL||audio.getStreamVolume(AudioManager.STREAM_SYSTEM)==0||
            Settings.System.getInt(context.contentResolver,Settings.System.SOUND_EFFECTS_ENABLED,1)==0)return 0
        val stream=pool.play(sample,.32f,.32f,1,0,1f)
        if(stream!=0){starts++;if(streams.size==16)streams.removeFirst();streams.addLast(stream)}
        return stream
    }
    fun release(){if(!released){setActive(false);released=true;pool.setOnLoadCompleteListener(null);pool.release();ready=false}}
}

/** Activity-scoped preload survives navigation and rotation; never owns an Activity context. */
internal class UseAudioModel(application:android.app.Application):AndroidViewModel(application) {
    val player=UseKeyClick(application)
    override fun onCleared(){player.release()}
}
@Composable internal fun rememberUseAudio():UseKeyClick {
    var context:Context=androidx.compose.ui.platform.LocalView.current.context
    while(context is android.content.ContextWrapper && context !is androidx.activity.ComponentActivity)context=context.baseContext
    val activity=context as androidx.activity.ComponentActivity
    return androidx.lifecycle.viewmodel.compose.viewModel<UseAudioModel>(viewModelStoreOwner=activity).player
}
@Composable internal fun rememberUseKeyClick():()->Unit {
    val player=rememberUseAudio();val owner=LocalLifecycleOwner.current
    DisposableEffect(owner,player){
        player.setActive(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer=LifecycleEventObserver{_,_->player.setActive(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))}
        owner.lifecycle.addObserver(observer)
        onDispose{owner.lifecycle.removeObserver(observer);player.setActive(false)}
    }
    return remember(player){{player.play();Unit}}
}
