package edu.cit.macansantos.cleanit.features.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.features.catalog.Technician

class TechniciansAdapter(
    private val technicians: List<Technician>,
    private val onTechnicianSelected: (Technician) -> Unit
) : RecyclerView.Adapter<TechniciansAdapter.TechnicianViewHolder>() {

    private var selectedPosition = -1

    inner class TechnicianViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rbTechnician: RadioButton = itemView.findViewById(R.id.rbTechnician)
        val tvTechnicianName: TextView = itemView.findViewById(R.id.tvTechnicianName)
        val tvTechnicianContact: TextView = itemView.findViewById(R.id.tvTechnicianContact)

        fun bind(technician: Technician, position: Int) {
            tvTechnicianName.text = technician.name
            tvTechnicianContact.text = technician.contactNo ?: "No contact info"

            rbTechnician.isChecked = position == selectedPosition

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onTechnicianSelected(technician)
            }

            rbTechnician.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onTechnicianSelected(technician)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TechnicianViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_technician, parent, false)
        return TechnicianViewHolder(view)
    }

    override fun onBindViewHolder(holder: TechnicianViewHolder, position: Int) {
        holder.bind(technicians[position], position)
    }

    override fun getItemCount(): Int = technicians.size
}
