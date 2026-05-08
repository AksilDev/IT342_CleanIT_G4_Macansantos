package edu.cit.macansantos.cleanit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.model.Booking

class BookingsAdapter(
    private val bookings: List<Booking>,
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBookingCode: TextView = itemView.findViewById(R.id.tvBookingCode)
        val tvServiceType: TextView = itemView.findViewById(R.id.tvServiceType)
        val tvBookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(booking: Booking) {
            tvBookingCode.text = booking.bookingCode
            tvServiceType.text = booking.serviceType
            tvBookingDate.text = "${booking.bookingDate} ${booking.timeSlot}"
            tvStatus.text = booking.status.uppercase()
            tvAmount.text = "₱${String.format("%.2f", booking.totalAmount)}"

            // Color code status
            val statusColor = when (booking.status.lowercase()) {
                "pending" -> 0xFFFFA500.toInt() // Orange
                "confirmed" -> 0xFF4CAF50.toInt() // Green
                "in_progress" -> 0xFF2196F3.toInt() // Blue
                "completed" -> 0xFF9C27B0.toInt() // Purple
                "cancelled", "voided" -> 0xFFF44336.toInt() // Red
                else -> 0xFF94A3B8.toInt() // Gray
            }
            tvStatus.setTextColor(statusColor)

            itemView.setOnClickListener {
                onBookingClick(booking)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size
}
