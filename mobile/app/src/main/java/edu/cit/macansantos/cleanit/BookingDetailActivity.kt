package edu.cit.macansantos.cleanit

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.macansantos.cleanit.network.RetrofitClient
import kotlinx.coroutines.launch

class BookingDetailActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvBookingCode: TextView
    private lateinit var tvServiceType: TextView
    private lateinit var tvDeviceType: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvBookingDate: TextView
    private lateinit var tvTimeSlot: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvLandmark: TextView
    private lateinit var tvSpecialInstructions: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvTechnicianName: TextView
    private lateinit var tvCreatedAt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        supportActionBar?.title = "Booking Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressBar = findViewById(R.id.progressBar)
        tvBookingCode = findViewById(R.id.tvBookingCode)
        tvServiceType = findViewById(R.id.tvServiceType)
        tvDeviceType = findViewById(R.id.tvDeviceType)
        tvStatus = findViewById(R.id.tvStatus)
        tvBookingDate = findViewById(R.id.tvBookingDate)
        tvTimeSlot = findViewById(R.id.tvTimeSlot)
        tvAddress = findViewById(R.id.tvAddress)
        tvLandmark = findViewById(R.id.tvLandmark)
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvTechnicianName = findViewById(R.id.tvTechnicianName)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)

        val bookingId = intent.getStringExtra("bookingId") ?: ""
        if (bookingId.isNotEmpty()) {
            loadBookingDetails(bookingId)
        } else {
            Toast.makeText(this, "Booking ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBookingDetails(bookingId: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBookingById(bookingId)
                if (response.isSuccessful && response.body() != null) {
                    val booking = response.body()!!
                    
                    tvBookingCode.text = booking.bookingCode
                    tvServiceType.text = booking.serviceType
                    tvDeviceType.text = booking.deviceType
                    tvStatus.text = booking.status.uppercase()
                    tvBookingDate.text = booking.bookingDate
                    tvTimeSlot.text = booking.timeSlot
                    tvAddress.text = booking.address
                    tvLandmark.text = booking.landmark ?: "N/A"
                    tvSpecialInstructions.text = booking.specialInstructions ?: "None"
                    tvTotalAmount.text = "₱${String.format("%.2f", booking.totalAmount)}"
                    tvTechnicianName.text = booking.technicianName ?: "Not assigned yet"
                    tvCreatedAt.text = booking.createdAt

                    // Color code status
                    val statusColor = when (booking.status.lowercase()) {
                        "pending" -> 0xFFFFA500.toInt()
                        "confirmed" -> 0xFF4CAF50.toInt()
                        "in_progress" -> 0xFF2196F3.toInt()
                        "completed" -> 0xFF9C27B0.toInt()
                        "cancelled", "voided" -> 0xFFF44336.toInt()
                        else -> 0xFF94A3B8.toInt()
                    }
                    tvStatus.setTextColor(statusColor)
                } else {
                    Toast.makeText(this@BookingDetailActivity,
                        "Failed to load booking details", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BookingDetailActivity,
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
