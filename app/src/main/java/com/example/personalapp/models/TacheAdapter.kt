package com.example.personalapp.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.graphics.Paint
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.personalapp.R
import com.example.personalapp.data.Tache


class TacheAdapter(
    private var items: List<Tache>,
    private val onCheckedChange: (tache: Tache, completed: Boolean) -> Unit,
    private val onItemClick: (tache: Tache) -> Unit
) : RecyclerView.Adapter<TacheAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tacheTitle)
        val duration: TextView = view.findViewById(R.id.tacheDuration)
        val cbDone: CheckBox = view.findViewById(R.id.tacheCheck)

        fun bind(t: Tache) {
            title.text = t.name
            duration.text = "${t.durationMinutes} min"
            cbDone.isChecked = t.completed

            // clic sur item -> ouvrir timer

            itemView.setOnClickListener {
                if (!t.completed) {
                    onItemClick(t)
                } else {
                    Toast.makeText(itemView.context, "Cette tâche est déjà terminée.", Toast.LENGTH_SHORT).show()
                }
            }


            if (t.completed) {
                title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                title.paintFlags = title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // changement d'état "terminé"
            cbDone.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != t.completed) {
                    onCheckedChange(t, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tache, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Tache>) {
        items = newList
        notifyDataSetChanged()
    }
}
