package org.sil.storyproducer.controller

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.*
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.SlideType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.logging.saveLog
import org.sil.storyproducer.tools.file.storyRelPathExists
import org.sil.storyproducer.tools.media.AudioPlayer
import java.util.*

/**
 * The fragment for the slide image view with audio player.  This fragment will display the picture
 * of the current slide as well as load the corresponding audio that is passed by bundle
 */
class SlidePhaseFrag : Fragment() {

    private lateinit var referenceAudioPlayer: AudioPlayer
    private var referencePlayButton: ImageButton? = null
    private var refPlaybackSeekBar: SeekBar? = null
    private lateinit var fragmentImageView: ImageView
    private var mSeekBarTimer = Timer()

    private var refPlaybackProgress = 0
    private var refPlaybackDuration = 0
    private var wasAudioPlaying = false

    private var slideNum: Int = 0 //gets overwritten
    private lateinit var refPlaybackHolder: LinearLayout
    private lateinit var playbackListener: PlaybackListener

    interface PlaybackListener {
        fun onStoppedPlayback()
        fun onStartedPlayback()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(parentFragment is PlaybackListener){
            playbackListener = parentFragment as PlaybackListener
        }
        else{
            throw ClassCastException("$parentFragment does not implement SlidePhaseFrag.PlaybackListener")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slideNum = arguments?.getInt(SLIDE_NUM)!!
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_slide, container, false)

        fragmentImageView = rootView.findViewById(R.id.fragment_image_view)
        referencePlayButton = rootView.findViewById(R.id.fragment_reference_audio_button)
        refPlaybackHolder = rootView.findViewById(R.id.seek_bar)
        refPlaybackSeekBar = rootView.findViewById(R.id.videoSeekBar)
        val slideNumberText = rootView.findViewById<TextView>(R.id.slide_number_text)
        slideNumberText?.text = slideNum.toString()

        setPic()

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val item = menu!!.getItem(0)
        super.onCreateOptionsMenu(menu, inflater)
        item.setIcon(R.drawable.ic_mic_white_48dp)
    }

    private fun setSeekBarListener() {
        refPlaybackDuration = referenceAudioPlayer.audioDurationInMilliseconds
        refPlaybackSeekBar?.max = refPlaybackDuration
        referenceAudioPlayer.currentPosition = refPlaybackProgress
        refPlaybackSeekBar?.progress = refPlaybackProgress
        refPlaybackSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {
                referenceAudioPlayer.currentPosition = refPlaybackProgress
                if(wasAudioPlaying){
                    referenceAudioPlayer.resumeAudio()
                    referencePlayButton!!.setBackgroundResource(R.drawable.ic_pause_white_48dp)
                    playbackListener.onStartedPlayback()
                }
            }
            override fun onStartTrackingTouch(sBar: SeekBar) {
                wasAudioPlaying = referenceAudioPlayer.isAudioPlaying
                referenceAudioPlayer.pauseAudio()
                referencePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
                playbackListener.onStoppedPlayback()
            }
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    refPlaybackProgress = progress
                }
            }
        })
    }
    /**
     * This function serves to stop the audio streams from continuing after the draft has been
     * put on pause.
     */
    override fun onPause() {
        if(this::referenceAudioPlayer.isInitialized) {
            refPlaybackProgress = referenceAudioPlayer.currentPosition
            referenceAudioPlayer.stopAudio()
            referenceAudioPlayer.release()
        }
        mSeekBarTimer.cancel()
        playbackListener.onStoppedPlayback()
        super.onPause()
    }

    /**
     * This function serves to handle page changes and stops the audio streams from
     * continuing.
     */

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        referenceAudioPlayer.stopAudio()
        referencePlayButton?.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     */
    internal fun setPic() {
        (activity as PhaseBaseActivity).setPic(fragmentImageView, slideNum)
        //Set up the reference audio and slide number overlays
        setReferenceAudioButton()
    }

    private fun setReferenceAudioButton() {
        referencePlayButton?.setOnClickListener {
            if (!storyRelPathExists(context!!,Workspace.activePhase.getReferenceAudioFile(slideNum))) {
                Snackbar.make(view!!, R.string.draft_playback_no_lwc_audio, Snackbar.LENGTH_SHORT).show()
                playbackListener.onStartedPlayback()
            }
            else if (Workspace.activeStory.slides[slideNum].slideType == SlideType.LOCALCREDITS){
                    refPlaybackHolder.visibility = View.GONE
            }
            else {
                //if its been init and audio is currently playing
                if (this::referenceAudioPlayer.isInitialized && referenceAudioPlayer.isAudioPlaying) {
                    stopPlayBackAndRecording()
                    refPlaybackProgress = referenceAudioPlayer.currentPosition
                    refPlaybackSeekBar?.progress = refPlaybackProgress
                }
                //if it hasn't been init
                else if (!this::referenceAudioPlayer.isInitialized) {
                    referenceAudioPlayer = AudioPlayer()
                    referenceAudioPlayer.setStorySource(context!!, Workspace.activePhase.getReferenceAudioFile(slideNum))
                    referenceAudioPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
                        referencePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
                        referenceAudioPlayer.stopAudio()
                        playbackListener.onStoppedPlayback()
                    })
                    mSeekBarTimer = Timer()
                    mSeekBarTimer.schedule(object : TimerTask() {
                        override fun run() {
                            activity!!.runOnUiThread{
                                if(referenceAudioPlayer.isAudioPlaying) {
                                    refPlaybackProgress = referenceAudioPlayer.currentPosition
                                    refPlaybackSeekBar?.progress = refPlaybackProgress
                                }
                            }
                        }
                    },0,33)

                    setSeekBarListener()
                    continuePlayback()
                }
                //if its been init but not playing
                else{
                    continuePlayback()
                }
            }
        }
    }

    private fun continuePlayback(){
        referenceAudioPlayer.resumeAudio()
        playbackListener.onStartedPlayback()
        referencePlayButton?.setBackgroundResource(R.drawable.ic_pause_white_48dp)
        when (Workspace.activePhase.phaseType) {
            PhaseType.DRAFT -> saveLog(activity!!.getString(R.string.LWC_PLAYBACK))
            PhaseType.COMMUNITY_CHECK -> saveLog(activity!!.getString(R.string.DRAFT_PLAYBACK))
            else -> {
            }
        }
    }

    fun stopPlayBackAndRecording() {
        if(this::referenceAudioPlayer.isInitialized) {
            referenceAudioPlayer.pauseAudio()
        }
        playbackListener.onStoppedPlayback()
        referencePlayButton!!.setBackgroundResource(R.drawable.ic_play_arrow_white_36dp)
    }

    companion object {
        const val SLIDE_NUM = "CURRENT_SLIDE_NUM_OF_FRAG"
    }
}
