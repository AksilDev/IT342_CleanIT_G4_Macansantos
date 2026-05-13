package edu.cit.macansantos.cleanit.features.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.features.booking.Booking

class BookingsAdapter(
    private val bookings: List<Booking>,
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBookingCode: TextView = itemView.findViewById(R.id.tvBookingCode)
        private val tvServiceType: TextView = itemView.findViewById(R.id.tvServiceType)
        private val tvBookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(booking: Booking) {
            val status = booking.status.orEmpty()

            tvBookingCode.text = booking.bookingCode ?: "Booking"
            tvServiceType.text = booking.serviceType ?: "Service"
            tvBookingDate.text = listOfNotNull(booking.bookingDate, booking.timeSlot).joinToString(" ")
            tvStatus.text = status.ifBlank { "unknown" }.uppercase()
            tvAmount.text = "PHP ${String.format("%.2f", booking.totalAmount ?: 0.0)}"

            val statusColor = when (status.lowercase()) {
                "pending" -> 0xFFFFA500.toInt()
                "confirmed" -> 0xFF4CAF50.toInt()
                "in_progress" -> 0xFF2196F3.toInt()
                "completed" -> 0xFF9C27B0.toInt()
                "cancelled", "voided" -> 0xFFF44336.toInt()
                else -> 0xFF94A3B8.toInt()
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
