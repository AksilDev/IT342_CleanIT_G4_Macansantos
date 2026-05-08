package edu.cit.macansantos.cleanit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.model.BookingPhoto

class PhotosAdapter(
    private val photos: List<BookingPhoto>
) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val tvPhotoType: TextView = itemView.findViewById(R.id.tvPhotoType)
        val tvPhotoDate: TextView = itemView.findViewById(R.id.tvPhotoDate)

        fun bind(photo: BookingPhoto) {
            tvPhotoType.text = photo.type
            tvPhotoDate.text = photo.uploadedAt
            
            // TODO: Load image with Coil or Glide
            // For now, just show placeholder
            ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size
}
