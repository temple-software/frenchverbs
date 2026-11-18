package com.templesoftware.frenchverbs

import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/** Decodes recordings made by the host at normal speed; no app UI/timeline manipulation. */
class RepeatStage4RecordingTest {
    @Test fun decodeNormalSpeedRecordingsForReview() {
        val root=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"pass30")
        for(name in listOf("portrait","landscape")) {
            val reader=MediaMetadataRetriever()
            reader.setDataSource(File(root,"$name.mp4").path)
            val frames=File(root,"$name-video-frames").apply{mkdirs()}
            File(root,"$name-video-metadata.txt").writeText("durationMs=${reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)} frames=${reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)}")
            for(ms in listOf(2500,3000,3500,3750,4000,4250,4500,5000,5500,6000,6500,7000,7500,8000,9000,10000)) {
                val bitmap=reader.getFrameAtTime(ms*1000L,MediaMetadataRetriever.OPTION_CLOSEST)
                assertNotNull("$name at $ms",bitmap)
                File(frames,"$ms.png").outputStream().use{bitmap!!.compress(Bitmap.CompressFormat.PNG,100,it)}
                bitmap!!.recycle()
            }
            reader.release()
        }
    }
}
