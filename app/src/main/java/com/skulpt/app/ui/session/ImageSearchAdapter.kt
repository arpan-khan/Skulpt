package com.skulpt.app.ui.session

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.skulpt.app.databinding.ItemImageSearchResultBinding

class ImageSearchAdapter(
    private val onImageSelected: (String) -> Unit
) : RecyclerView.Adapter<ImageSearchAdapter.ViewHolder>() {

    private var urls: List<String> = emptyList()

    fun submitUrls(newUrls: List<String>) {
        urls = newUrls
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemImageSearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = urls[position]
        Glide.with(holder.binding.ivResult.context)
            .load(url)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .centerCrop()
            .into(holder.binding.ivResult)
        
        holder.binding.root.setOnClickListener { onImageSelected(url) }
    }

    override fun getItemCount() = urls.size

    class ViewHolder(val binding: ItemImageSearchResultBinding) : RecyclerView.ViewHolder(binding.root)
}
