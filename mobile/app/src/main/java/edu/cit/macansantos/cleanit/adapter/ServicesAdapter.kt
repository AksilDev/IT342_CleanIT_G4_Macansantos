package edu.cit.macansantos.cleanit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.model.Service

class ServicesAdapter(
    private val services: List<Service>,
    private val onServiceClick: (Service) -> Unit
) : RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivServiceImage: ImageView = itemView.findViewById(R.id.ivServiceImage)
        val tvServiceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val tvServiceDescription: TextView = itemView.findViewById(R.id.tvServiceDescription)
        val tvServicePrice: TextView = itemView.findViewById(R.id.tvServicePrice)
        val tvServiceDuration: TextView = itemView.findViewById(R.id.tvServiceDuration)

        fun bind(service: Service) {
            tvServiceName.text = service.name
            tvServiceDescription.text = service.description
            tvServicePrice.text = "₱${String.format("%.2f", service.basePrice)}"
            val hours = service.durationMinutes / 60.0
            tvServiceDuration.text = "${String.format("%.1f", hours)} hours"

            // Load service image with Coil
            val imageUrl = getServiceImageUrl(service.name)
            ivServiceImage.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_service_placeholder)
                error(R.drawable.ic_service_placeholder)
                transformations(RoundedCornersTransformation(16f))
            }

            itemView.setOnClickListener {
                onServiceClick(service)
            }
        }

        private fun getServiceImageUrl(serviceName: String): String {
            // Map service names to image URLs
            return when {
                serviceName.contains("External Cleaning", ignoreCase = true) -> 
                    "https://images.unsplash.com/photo-1587825140708-dfaf72ae4b04?w=400"
                serviceName.contains("Internal Cleaning", ignoreCase = true) -> 
                    "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=400"
                serviceName.contains("GPU", ignoreCase = true) -> 
                    "https://images.unsplash.com/photo-1591488320449-011701bb6704?w=400"
                serviceName.contains("PSU", ignoreCase = true) -> 
                    "https://images.unsplash.com/photo-1555680202-c86f0e12f086?w=400"
                else -> "https://images.unsplash.com/photo-1587825140708-dfaf72ae4b04?w=400"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount(): Int = services.size
}
