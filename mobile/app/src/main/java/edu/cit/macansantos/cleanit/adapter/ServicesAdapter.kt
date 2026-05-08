package edu.cit.macansantos.cleanit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.model.Service

class ServicesAdapter(
    private val services: List<Service>,
    private val onServiceClick: (Service) -> Unit
) : RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

            itemView.setOnClickListener {
                onServiceClick(service)
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
