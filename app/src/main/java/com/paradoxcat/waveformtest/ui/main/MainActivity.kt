package com.paradoxcat.waveformtest.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.paradoxcat.waveformtest.util.extention.registerPlaybackPercentageCallback
import com.paradoxcat.waveformtest.waveform.listener.ProgressChangeListener
import com.paradoxcat.waveformtest.waveviewer.R
import com.paradoxcat.waveformtest.waveviewer.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityMainBinding
    private val _viewModel: MainViewModel by viewModels()

    private val getContentCallback =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                _viewModel.extractDataFromUri(uri)
            }
        }

    private val exoPlayer by lazy { ExoPlayer.Builder(this).build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        setupExoPlayer()
        registerClickListeners()
        registerObservers()
    }

    private fun setupExoPlayer() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    // restarting the audio track.
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = false
                } else if (playbackState == Player.STATE_READY) {
                    _viewModel.convertDurationToTimeFrames(exoPlayer.duration)
                    _binding.playLayout.visibility = View.VISIBLE
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) _binding.playButton.setImageResource(R.drawable.ic_pause)
                else _binding.playButton.setImageResource(R.drawable.ic_play_arrow)
            }
        })

        exoPlayer.registerPlaybackPercentageCallback(100)
            .onEach { percentage ->
                _binding.waveformView.progress = percentage
            }.catch { e ->
                e.printStackTrace()
            }
            .launchIn(lifecycleScope)

        _binding.waveformView.progressChangeListener =
            ProgressChangeListener { progress ->
                // calculating the progress value in milliseconds
                exoPlayer.seekTo((exoPlayer.duration * (progress / 100)).toLong())
            }
    }

    private fun registerClickListeners() {
        _binding.playButton.setOnClickListener {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        }
        _binding.pickAudioButton.setOnClickListener {
            getContentCallback.launch("audio/*")
        }
    }

    private fun registerObservers() {
        _viewModel.uriLiveData.observe(
            this
        ) { uri ->
            val firstItem = MediaItem.fromUri(uri)
            exoPlayer.clearMediaItems()
            exoPlayer.addMediaItem(firstItem)
            exoPlayer.prepare()
        }

        _viewModel.waveFormData.observe(
            this
        ) { waveFormData ->
            _binding.waveformView.setData(waveFormData)
        }

        _viewModel.timeFramesData.observe(
            this
        ) { timeFrames ->
            _binding.waveformView.setTimeFrames(timeFrames)
        }
    }
}
