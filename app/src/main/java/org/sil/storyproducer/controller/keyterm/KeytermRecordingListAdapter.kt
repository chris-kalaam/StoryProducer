package org.sil.storyproducer.controller.keyterm

import android.app.AlertDialog
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior.STATE_COLLAPSED
import android.support.design.widget.BottomSheetBehavior.from
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.sil.storyproducer.R
import org.sil.storyproducer.model.KeytermRecording
import org.sil.storyproducer.tools.hideKeyboard

class KeytermRecordingListAdapter(val context: Context?, private val recordings: MutableList<KeytermRecording>, val bottomSheet: ConstraintLayout, private val listeners: ClickListeners) : RecyclerView.Adapter<KeytermRecordingListAdapter.RecordingListViewHolder>() {

    interface ClickListeners {
        fun onRowClick(pos: Int)
        fun onPlayClick(pos: Int, buttonClickedNow: ImageButton)
        fun onDeleteClick(name: String, pos: Int)
        fun onRenameClick(pos: Int, newName: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingListViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.keyterm_audio_comment_list_item, parent, false)

        return RecordingListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecordingListViewHolder, position: Int) {
        holder.bindView(recordings[position])
    }

    override fun getItemCount(): Int = recordings.size

    inner class RecordingListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var parentTextView: TextView = itemView.findViewById(R.id.audio_comment_title)
        private var parentPlayButton: ImageButton = itemView.findViewById(R.id.audio_comment_play_button)
        private var parentDeleteButton: ImageButton = itemView.findViewById(R.id.audio_comment_delete_button)
        private var frameLayoutChildItem: FrameLayout = itemView.findViewById(R.id.child_holder)
        private val inflater: LayoutInflater = LayoutInflater.from(itemView.context)

        private val childComment = inflater.inflate(R.layout.backtranslation_comment_list_item, null, false)
        private val childSubmit = inflater.inflate(R.layout.submit_backtranslation_item, null, false)

        fun bindView(keytermRecording : KeytermRecording){
            val audioFilename = keytermRecording.audioRecordingFilename.substringAfterLast('/')
            parentTextView.text = audioFilename.substringBeforeLast('.')
            parentPlayButton.setOnClickListener {
                listeners.onPlayClick(adapterPosition, parentPlayButton)
            }
            parentDeleteButton.setOnClickListener {
                showDeleteItemDialog(adapterPosition, audioFilename)
            }
            parentTextView.setOnClickListener {
                listeners.onRowClick(adapterPosition)
            }
            parentTextView.setOnLongClickListener {
                showItemRenameDialog(adapterPosition)
                return@setOnLongClickListener true
            }

            if(recordings[adapterPosition].isTextBackTranslationSubmitted){
                initCommentState()
            }
            else{
                initSubmitState()
            }
        }

        private fun initSubmitState(){
            frameLayoutChildItem.removeAllViews()
            val addBacktranslation = childSubmit.findViewById<ImageButton>(R.id.submit_backtranslation_button)
            val editText = childSubmit.findViewById<EditText>(R.id.backtranslation_edit_text)
            editText.setText(recordings[adapterPosition].textBackTranslation)

            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    recordings[adapterPosition].textBackTranslation = s.toString()
                }
            })

            addBacktranslation.setOnClickListener {
                if(editText.text.toString() != ""){
                    recordings[adapterPosition].textBackTranslation = editText.text.toString()
                    recordings[adapterPosition].isTextBackTranslationSubmitted = true
                    frameLayoutChildItem.removeAllViews()
                    context?.hideKeyboard(it)
                    initCommentState()
                }
            }
            frameLayoutChildItem.addView(childSubmit)
        }

        private fun initCommentState(){
            frameLayoutChildItem.removeAllViews()
            val currentDeleteButton = childComment.findViewById<ImageButton>(R.id.backtranslation_comment_delete_button)
            val textView = childComment.findViewById<TextView>(R.id.backtranslation_comment_title)
            textView.text = recordings[adapterPosition].textBackTranslation

            currentDeleteButton.setOnClickListener {
                recordings[adapterPosition].textBackTranslation = ""
                recordings[adapterPosition].isTextBackTranslationSubmitted = false
                frameLayoutChildItem.removeAllViews()
                initSubmitState()
            }
        frameLayoutChildItem.addView(childComment)
        }

        private fun showDeleteItemDialog(position: Int, text: String) {
            val dialog = AlertDialog.Builder(itemView.context)
                    .setTitle(itemView.context.getString(R.string.delete_audio_title))
                    .setMessage(itemView.context.getString(R.string.delete_audio_message))
                    .setNegativeButton(itemView.context.getString(R.string.no), null)
                    .setPositiveButton(itemView.context.getString(R.string.yes)) { _, _ ->
                        listeners.onDeleteClick(text, position)
                        notifyItemRemoved(position)
                        if(recordings.isEmpty()) {
                            from(bottomSheet).state = STATE_COLLAPSED
                        }
                    }
                    .create()

            dialog.show()
        }

        /**
         * Show to the user a dialog to rename the audio comment
         *
         * @param position the integer position of the comment the user "long-clicked"
         */
        private fun showItemRenameDialog(position: Int) {
            val newName = EditText(itemView.context)

            // Programmatically set layout properties for edit text field
            val params = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            // Apply layout properties
            newName.layoutParams = params

            val dialog = AlertDialog.Builder(itemView.context)
                    .setTitle(itemView.context.getString(R.string.rename_title))
                    .setView(newName)
                    .setNegativeButton(itemView.context.getString(R.string.cancel), null)
                    .setPositiveButton(itemView.context.getString(R.string.save)) { _, _ ->
                        listeners.onRenameClick(position, newName.text.toString())
                        notifyDataSetChanged()
                    }.create()

            dialog.show()
            //TODO make keyboard show at once, but right now there are too many issues with it.
        }
    }
}