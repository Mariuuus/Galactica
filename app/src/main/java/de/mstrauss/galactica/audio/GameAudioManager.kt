package de.mstrauss.galactica.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import de.mstrauss.galactica.R

object GameAudioManager {
    private const val PREFS_NAME = "galactica_audio"
    private const val KEY_VOLUME = "volume"
    private const val DEFAULT_VOLUME = 0.7f

    private var soundPool: SoundPool? = null
    private var soundPlanet = 0
    private var soundBomb = 0
    private var soundRocketship = 0
    private var soundWon = 0
    private var volume = DEFAULT_VOLUME
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext

        volume = appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_VOLUME, DEFAULT_VOLUME)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()

        soundPool?.let { pool ->
            soundPlanet = pool.load(appContext, R.raw.sound_planet, 1)
            soundBomb = pool.load(appContext, R.raw.sound_bomb, 1)
            soundRocketship = pool.load(appContext, R.raw.sound_rocketship, 1)
            soundWon = pool.load(appContext, R.raw.sound_won, 1)
        }

        initialized = true
    }

    fun setVolume(value: Float, context: Context) {
        volume = value.coerceIn(0f, 1f)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_VOLUME, volume)
            .apply()
    }

    fun getVolume(): Float = volume

    fun playPlanetSound() {
        soundPool?.play(soundPlanet, volume, volume, 1, 0, 1f)
    }

    fun playBombSound() {
        soundPool?.play(soundBomb, volume, volume, 1, 0, 1f)
    }

    fun playRocketshipSound() {
        soundPool?.play(soundRocketship, volume, volume, 1, 0, 1f)
    }

    fun playWonSound() {
        soundPool?.play(soundWon, volume, volume, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        initialized = false
    }
}
