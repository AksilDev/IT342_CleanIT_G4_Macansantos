package edu.cit.macansantos.cleanit.features.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.features.booking.BookingPhoto

class PhotosAdapter(
    private val photos: List<BookingPhoto>
) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val tvPhotoType: TextView = itemView.findViewById(R.id.tvPhotoType)
        val tvPhotoDate: TextView = itemView.findViewById(R.id.tvPhotoDate)

        fun bind(photo: BookingPhoto) {
            tvPhotoType.text = photo.type ?: "PHOTO"
            tvPhotoDate.text = photo.uploadedAt ?: ""
            
            // Load image with Coil
            ivPhoto.load(photo.fileUrl ?: photo.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_service_placeholder)
                error(R.drawable.ic_service_placeholder)
                transformations(RoundedCornersTransformation(8f))
            }
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
