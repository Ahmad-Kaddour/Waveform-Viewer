package com.paradoxcat.waveformtest.extention

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


fun ExoPlayer.registerPlaybackPercentageCallback(interval: Long): Flow<Double>{
    val mediaPlayer = this
    return callbackFlow {
        var didSentLastValue = false
        while (!isClosedForSend){
            delay(interval)
            val percentage = (currentPosition / duration.toDouble()) * 100
            if (mediaPlayer.isPlaying) {
                send(percentage)
                didSentLastValue = false
            }else if (!didSentLastValue){
                send(percentage)
                didSentLastValue = true
            }
        }
    }
}