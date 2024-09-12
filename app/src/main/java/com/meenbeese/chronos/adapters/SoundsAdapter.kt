package com.meenbeese.chronos.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

import com.meenbeese.chronos.Chronos
import com.meenbeese.chronos.R
import com.meenbeese.chronos.data.SoundData
import com.meenbeese.chronos.interfaces.SoundChooserListener


class SoundsAdapter(
    private val chronos: Chronos,
    private val sounds: List<SoundData>
) : RecyclerView.Adapter<SoundsAdapter.ViewHolder>() {
    private var currentlyPlaying = -1
    private var listener: SoundChooserListener? = null
    fun setListener(listener: SoundChooserListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_sound, parent, false)
        )
    }

    @UnstableApi
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.title.setText(R.string.title_sound_none)
            holder.icon.setOnClickListener(null)
            holder.itemView.setOnClickListener {
                listener?.onSoundChosen(null)
            }
            setPlaying(holder, isPlaying = false, isAnimated = false)
            holder.icon.setImageResource(R.drawable.ic_ringtone_disabled)
            val color = ContextCompat.getColor(holder.itemView.context, R.color.textColorPrimary)
            holder.icon.setColorFilter(color)
        } else {
            val sound = sounds[position - 1]
            holder.title.text = sound.name
            holder.icon.setOnClickListener {
                val position1 = holder.bindingAdapterPosition
                val sound1 = sounds[position1 - 1]
                currentlyPlaying = if (sound1.isPlaying(chronos) || currentlyPlaying == position1) {
                    sound1.stop(chronos)
                    -1
                } else {
                    sound1.preview(chronos)
                    if (currentlyPlaying >= 0) {
                        sounds[currentlyPlaying - 1].stop(chronos)
                        notifyItemChanged(currentlyPlaying)
                    }
                    position1
                }
                setPlaying(holder, currentlyPlaying == position1, true)
            }
            holder.itemView.setOnClickListener {
                listener?.onSoundChosen(sounds[holder.bindingAdapterPosition - 1])
            }
            setPlaying(holder, sound.isPlaying(chronos), false)
        }
    }

    @SuppressLint("CheckResult")
    private fun setPlaying(holder: ViewHolder, isPlaying: Boolean, isAnimated: Boolean) {
        val color = ContextCompat.getColor(holder.itemView.context, R.color.textColorPrimary)

        if (isAnimated) {
            val drawable = AnimatedVectorDrawableCompat.create(
                chronos, if (isPlaying) R.drawable.ic_play_to_pause else R.drawable.ic_pause_to_play
            )
            if (drawable != null) {
                holder.icon.setImageDrawable(drawable)
                holder.icon.setColorFilter(color)
                drawable.start()
                return
            }
        }
        holder.icon.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        holder.icon.setColorFilter(color)
    }

    override fun getItemCount(): Int {
        return sounds.size + 1
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val title: TextView = itemView.findViewById(R.id.title)
    }
}
