package com.example.ui.reader

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.annotation.RawRes
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Multiple Sound Profiles for Page Turn Experience.
 * Each style links directly to high-fidelity, studio-mastered 44.1kHz WAV resources.
 */
enum class PageTurnSoundStyle(
    val title: String,
    val subtitle: String,
    @RawRes val rawResId: Int
) {
    CLASSIC_PAPER(
        title = "Classic Paper",
        subtitle = "Rich authentic tactile paper turn",
        rawResId = R.raw.page_flip
    ),
    SILKY_WHISPER(
        title = "Silky Whisper",
        subtitle = "Ultra-soft gentle airy glide",
        rawResId = R.raw.sound_silky_whisper
    ),
    CRISP_PARCHMENT(
        title = "Crisp Parchment",
        subtitle = "Fine-grain library book leaf",
        rawResId = R.raw.sound_crisp_parchment
    ),
    DIGITAL_SNAP(
        title = "Digital Snap",
        subtitle = "Modern minimalist clean tap",
        rawResId = R.raw.sound_digital_snap
    ),
    WARM_FLUTTER(
        title = "Warm Flutter",
        subtitle = "Vintage archive page flutter",
        rawResId = R.raw.sound_warm_flutter
    )
}

/**
 * High-performance, zero-latency AudioTrack manager for page-turn sonification.
 * Uses direct PCM AudioTrack streams which completely bypass MediaCodec/Codec2,
 * eliminating "Failed to query component interface for required system resources" errors.
 */
class PageTurnSoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("lumina_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PageTurnAudioEngine"

        // Shared engine instance across screens
        @Volatile
        private var sharedEngine: SharedAudioEngine? = null

        private fun getSharedEngine(appContext: Context): SharedAudioEngine {
            return sharedEngine ?: synchronized(this) {
                sharedEngine ?: SharedAudioEngine(appContext.applicationContext).also {
                    sharedEngine = it
                }
            }
        }
    }

    private val engine = getSharedEngine(appContext)

    /**
     * Plays the currently active page turn sound according to user preferences.
     */
    fun playPageTurnSound(styleOverride: PageTurnSoundStyle? = null) {
        val isSoundEnabled = prefs.getBoolean("pref_sound", true)
        if (!isSoundEnabled) return

        val style = styleOverride ?: getCurrentSelectedStyle()
        engine.play(style)
    }

    /**
     * Preview sound in Settings popup without requiring pref_sound to be enabled.
     */
    fun previewSound(style: PageTurnSoundStyle) {
        engine.play(style)
    }

    fun getCurrentSelectedStyle(): PageTurnSoundStyle {
        val savedName = prefs.getString("pref_sound_style", PageTurnSoundStyle.CLASSIC_PAPER.name)
        return try {
            PageTurnSoundStyle.valueOf(savedName ?: PageTurnSoundStyle.CLASSIC_PAPER.name)
        } catch (_: Exception) {
            PageTurnSoundStyle.CLASSIC_PAPER
        }
    }

    fun release() {
        // Shared engine manages background lifetime
    }

    /**
     * Internal Shared Native AudioTrack Engine.
     * Extracts raw 16-bit PCM samples directly from WAV files and loads them into static AudioTracks.
     * Completely eliminates Stagefright/MediaCodec/SoundDecoder queries.
     */
    private class SharedAudioEngine(private val context: Context) {
        private val audioTracks = ConcurrentHashMap<PageTurnSoundStyle, AudioTrack>()
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var lastTriggerTime: Long = 0L

        init {
            scope.launch {
                for (style in PageTurnSoundStyle.entries) {
                    try {
                        val pcmBytes = extractPcm(context, style.rawResId)
                        if (pcmBytes != null && pcmBytes.isNotEmpty()) {
                            val track = createStaticAudioTrack(pcmBytes)
                            if (track != null) {
                                audioTracks[style] = track
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Failed initializing AudioTrack for style $style", e)
                    }
                }
            }
        }

        private fun createStaticAudioTrack(pcmBytes: ByteArray): AudioTrack? {
            return try {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(pcmBytes.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                val written = track.write(pcmBytes, 0, pcmBytes.size)
                if (written > 0) {
                    track
                } else {
                    track.release()
                    null
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Unable to create AudioTrack", e)
                null
            }
        }

        private fun extractPcm(context: Context, @RawRes resId: Int): ByteArray? {
            return try {
                context.resources.openRawResource(resId).use { stream ->
                    val allBytes = stream.readBytes()
                    var dataOffset = -1
                    var dataSize = 0
                    for (i in 0 until allBytes.size - 8) {
                        if (allBytes[i] == 'd'.code.toByte() &&
                            allBytes[i + 1] == 'a'.code.toByte() &&
                            allBytes[i + 2] == 't'.code.toByte() &&
                            allBytes[i + 3] == 'a'.code.toByte()
                        ) {
                            dataOffset = i + 8
                            dataSize = (allBytes[i + 4].toInt() and 0xFF) or
                                    ((allBytes[i + 5].toInt() and 0xFF) shl 8) or
                                    ((allBytes[i + 6].toInt() and 0xFF) shl 16) or
                                    ((allBytes[i + 7].toInt() and 0xFF) shl 24)
                            break
                        }
                    }
                    if (dataOffset != -1 && dataOffset + dataSize <= allBytes.size && dataSize > 0) {
                        allBytes.copyOfRange(dataOffset, dataOffset + dataSize)
                    } else if (allBytes.size > 44) {
                        allBytes.copyOfRange(44, allBytes.size)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error extracting PCM for resource $resId", e)
                null
            }
        }

        fun play(style: PageTurnSoundStyle) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTime < 45L) return
            lastTriggerTime = now

            val track = audioTracks[style] ?: return
            try {
                if (track.state != AudioTrack.STATE_INITIALIZED) return
                if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                    track.stop()
                }
                track.reloadStaticData()
                track.play()
            } catch (e: Throwable) {
                Log.w(TAG, "AudioTrack playback exception", e)
            }
        }
    }
}
