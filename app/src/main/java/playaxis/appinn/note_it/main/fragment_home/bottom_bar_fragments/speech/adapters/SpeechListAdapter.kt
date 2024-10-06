package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.adapters

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import playaxis.appinn.note_it.R
import playaxis.appinn.note_it.application.QuickNotepad
import playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.speech.helper.Speech
import java.io.IOException


class SpeechListAdapter(private val deleteItemEvent: DeleteItemEvent) : RecyclerView.Adapter<SpeechListAdapter.SpeechListViewHolder>() {

    private var speechList: ArrayList<Speech> = ArrayList()
    private var play = false

    fun setSpeechList(speechList: ArrayList<Speech>) {
        this.speechList = speechList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeechListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.voice_note_list_item, parent, false)
        return SpeechListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (speechList.isNotEmpty())
            speechList.size
        else
            0
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SpeechListViewHolder, position: Int) {

        //setting up audio to the wave seekbar
        holder.waveSeekbar.setSampleFrom(speechList[position].audio)
        //setting default color of the 'WaveFromProgress' to white
        holder.waveSeekbar.waveProgressColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)

        // Release previous MediaPlayer resources
        holder.mediaPlayer?.release()
        holder.mediaPlayer = null

        // Initialize new MediaPlayer
        holder.mediaPlayer = MediaPlayer()

        //initializing seekbar
        val mediaPlayer = MediaPlayer.create(QuickNotepad.appContext, Uri.parse(speechList[position].audio))

        if (mediaPlayer != null){
            mediaPlayer.setOnPreparedListener { mediaPlayer ->

                val duration = mediaPlayer.duration // Duration in milliseconds

                //formatting the view for duration
                val minutes = millisecondsToTime(duration) / 60
                val seconds = millisecondsToTime(duration) % 60

                holder.durationAudio.text = "00 : $minutes$seconds"
                // Now you can use 'duration' variable as the total duration of the audio
                mediaPlayer.release() // Release the MediaPlayer when done
            }
        }

        //playing audio on button click
        holder.playAudio.setOnClickListener {

            /**This is needed to be done again and again because the media player is needed to be released again and again because
             * it is needed to be release everytime when it is needed to play an audio. So, as it is in recyclerview so need to
             * release the media player again and again everytime when a new item is loaded in it**/
            // Release previous MediaPlayer resources
            holder.mediaPlayer?.release()
            holder.mediaPlayer = null
            // Initialize new MediaPlayer
            holder.mediaPlayer = MediaPlayer()

            //Setting audio file to the media player reinitialized object
            try {
                holder.mediaPlayer!!.setDataSource(speechList[position].audio)
                holder.mediaPlayer!!.prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            //initializing seekbar
            initializeSeekBar(holder)

            //first ov we need to change the click icon of the (imageView) as button
            play = if (play){

                holder.playAudio.setImageDrawable(ContextCompat.getDrawable(QuickNotepad.appContext,R.drawable.play_arrow))
                //Starting the media player or playing the video here
                holder.mediaPlayer!!.pause()
                false
            }
            else{

                holder.playAudio.setImageDrawable(ContextCompat.getDrawable(QuickNotepad.appContext,R.drawable.pause))
                //Starting the media player or playing the video here
                holder.mediaPlayer!!.start()
                true
            }

            //re-setting the icon of the button
            holder.mediaPlayer!!.setOnCompletionListener {

                holder.playAudio.setImageDrawable(ContextCompat.getDrawable(QuickNotepad.appContext,R.drawable.play_arrow))
                //Starting the media player or playing the video here
                holder.mediaPlayer!!.pause()
                play = false
            }
        }

        //set the seekbar when the audio is playing
        holder.waveSeekbar.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(waveformSeekBar: WaveformSeekBar, progress: Float, fromUser: Boolean) {

                if (fromUser){
                    holder.mediaPlayer!!.seekTo(progress.toInt())
                }
            }

        }

        //delete audio button
        holder.deleteAudio.setOnClickListener {
            //delete the current item
            deleteItemEvent.deleteItem(speechList[position])
        }
    }
    override fun onViewRecycled(holder: SpeechListViewHolder) {
        super.onViewRecycled(holder)

        if (holder.mediaPlayer != null) {
            holder.mediaPlayer!!.release()
            holder.mediaPlayer = null
        }
    }

    private fun initializeSeekBar(holder: SpeechListViewHolder) {

        holder.waveSeekbar.maxProgress = holder.mediaPlayer!!.duration.toFloat()
        val handler = Handler()
        handler.postDelayed(object : Runnable{
            @SuppressLint("SetTextI18n")
            override fun run() {

                try {
                    //setting the progress color
                    if (holder.mediaPlayer!!.currentPosition == holder.mediaPlayer!!.duration)
                        holder.waveSeekbar.waveProgressColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.white)
                    else
                        holder.waveSeekbar.waveProgressColor = ContextCompat.getColor(QuickNotepad.appContext,R.color.wave_progress_color)

                    //advancing the progress
                    holder.waveSeekbar.progress = holder.mediaPlayer!!.currentPosition.toFloat()

                    //formatting the view for duration
                    val minutes = millisecondsToTime(holder.mediaPlayer!!.duration) / 60
                    val seconds = millisecondsToTime(holder.mediaPlayer!!.duration) % 60

                    //formatting for progress
                    val pMinutes = millisecondsToTime(holder.mediaPlayer!!.currentPosition) / 60
                    val pSeconds = millisecondsToTime(holder.mediaPlayer!!.currentPosition) % 60

                    holder.durationAudio.text = "$pMinutes$pSeconds : $minutes$seconds"
                    handler.postDelayed(this,500)
                }
                catch (e:Exception){
                    holder.waveSeekbar.progress = 0f
                }
            }
        },0)
    }
    private fun millisecondsToTime(milliseconds: Int): Int {
        return milliseconds / 1000
    }

    class SpeechListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val playAudio: AppCompatImageView = itemView.findViewById(R.id.play)
        val waveSeekbar: WaveformSeekBar = itemView.findViewById(R.id.audio_visual)
        val durationAudio: AppCompatTextView = itemView.findViewById(R.id.audio_duration)
        val deleteAudio: AppCompatImageView = itemView.findViewById(R.id.delete_audio)

        //media player
        var mediaPlayer: MediaPlayer? = null
    }

    interface DeleteItemEvent{
        fun deleteItem(speech: Speech)
    }
}