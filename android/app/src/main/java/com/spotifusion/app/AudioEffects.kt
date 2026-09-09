package com.spotifusion.app

import android.media.audiofx.Equalizer

/** Owns the device Equalizer attached to the active Media3 audio session. */
object AudioEffects {
    private var eq: Equalizer? = null
    private var session = 0

    fun attach(audioSessionId: Int): Boolean {
        if (audioSessionId <= 0) return false
        if (session == audioSessionId && eq != null) return true
        release()
        return runCatching { Equalizer(0, audioSessionId).also { it.enabled = true; eq = it; session = audioSessionId } }.isSuccess
    }
    fun bands(): Short = eq?.numberOfBands ?: 0
    fun range(): ShortArray = eq?.bandLevelRange ?: shortArrayOf(-1500,1500)
    fun setBand(index: Short, level: Short) { runCatching { eq?.setBandLevel(index, level.coerceIn(range()[0],range()[1])) } }
    fun level(index: Short): Short = runCatching { eq?.getBandLevel(index) ?: 0 }.getOrDefault(0)
    fun setPreset(name: String) { runCatching { eq?.usePreset((0 until (eq?.numberOfPresets ?: 0)).firstOrNull { eq?.getPresetName(it.toShort())?.equals(name,true)==true }?.toShort() ?: return@runCatching) } }
    fun release(){runCatching{eq?.release()};eq=null;session=0}
}