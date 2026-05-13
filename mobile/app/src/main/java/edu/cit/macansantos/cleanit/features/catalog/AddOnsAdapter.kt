package edu.cit.macansantos.cleanit.features.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.features.catalog.AddOn

class AddOnsAdapter(
    private val addOns: List<AddOn>,
    private val onAddOnSelected: (AddOn, Boolean) -> Unit
) : RecyclerView.Adapter<AddOnsAdapter.AddOnViewHolder>() {

    inner class AddOnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbAddOn: CheckBox = itemView.findViewById(R.id.cbAddOn)
        val tvAddOnName: TextView = itemView.findViewById(R.id.tvAddOnName)
        val tvAddOnDescription: TextView = itemView.findViewById(R.id.tvAddOnDescription)
        val tvAddOnPrice: TextView = itemView.findViewById(R.id.tvAddOnPrice)

        fun bind(addOn: AddOn) {
            tvAddOnName.text = addOn.name
            tvAddOnDescription.text = addOn.description
            tvAddOnPrice.text = "₱${String.format("%.2f", addOn.price)}"

            cbAddOn.setOnCheckedChangeListener(null)
            cbAddOn.isChecked = false

            cbAddOn.setOnCheckedChangeListener { _, isChecked ->
                onAddOnSelected(addOn, isChecked)
            }

            itemView.setOnClickListener {
                cbAddOn.isChecked = !cbAddOn.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddOnViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon, parent, false)
        return AddOnViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddOnViewHolder, position: Int) {
        holder.bind(addOns[position])
    }

    override fun getItemCount(): Int = addOns.size
}
