package org.netizencoders.kotaquest.helpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.netizencoders.kotaquest.R
import org.netizencoders.kotaquest.models.Quest

class ListDataAdapter(private val data: ArrayList<Quest>): RecyclerView.Adapter<ListDataAdapter.ListViewHolder>() {
    private lateinit var onItemBtnClickCallback: OnItemBtnClickCallback

    fun setOnItemBtnClickCallback(onItemBtnClickCallback: OnItemBtnClickCallback) {
        this.onItemBtnClickCallback = onItemBtnClickCallback
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ListViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context).inflate(R.layout.quest_item, viewGroup, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val item = data[position]

        if (!item.ImageURL.isNullOrEmpty()) {
            holder.qImage.layoutParams?.height = 197
            holder.qImage.layoutParams?.width = 350
            holder.qImage.requestLayout()

            Picasso.get().load(item.ImageURL).fit().centerCrop().into(holder.qImage)
        }

        holder.qTitle.text=item.Title
        holder.qLocation.text=item.Location
        holder.qDescription.text=item.Description

        holder.btnR.setOnClickListener { onItemBtnClickCallback.onItemBtnClicked(data[holder.bindingAdapterPosition], "btnR") }
        holder.btnL.setOnClickListener { onItemBtnClickCallback.onItemBtnClicked(data[holder.bindingAdapterPosition], "btnL") }
    }

    interface OnItemBtnClickCallback {
        fun onItemBtnClicked(data: Quest, button: String)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class ListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var qTitle: TextView = itemView.findViewById(R.id.quest_item_title)
        var qLocation: TextView = itemView.findViewById(R.id.quest_item_location)
        var qDescription: TextView = itemView.findViewById(R.id.quest_item_description)
        var qImage: ImageView = itemView.findViewById(R.id.quest_item_image)
        var btnR: Button = itemView.findViewById(R.id.quest_item_btnR)
        var btnL: Button = itemView.findViewById(R.id.quest_item_btnL)
    }
}

