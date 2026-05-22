package edu.cit.macansantos.cleanit.features.booking

import edu.cit.macansantos.cleanit.R

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.features.booking.PhotosAdapter
import edu.cit.macansantos.cleanit.features.booking.Booking
import edu.cit.macansantos.cleanit.features.booking.RescheduleBookingRequest
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvBookingCode: TextView
    private lateinit var tvServiceType: TextView
    private lateinit var tvDeviceType: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusMessage: TextView
    private lateinit var tvBookingDate: TextView
    private lateinit var tvTimeSlot: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvLandmark: TextView
    private lateinit var tvSpecialInstructions: TextView
    private lateinit var tvAddOns: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvTechnicianName: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var layoutActions: LinearLayout
    private lateinit var btnCancel: Button
    private lateinit var btnReschedule: Button
    private lateinit var layoutPhotos: LinearLayout
    private lateinit var rvPhotos: RecyclerView
    private lateinit var tvNoPhotos: TextView

    private var booking: Booking? = null
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        supportActionBar?.title = "Booking Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        userId = intent.getStringExtra("userId") ?: ""
        
        initializeViews()

        val bookingId = intent.getStringExtra("bookingId") ?: ""
        if (bookingId.isNotEmpty()) {
            loadBookingDetails(bookingId)
        } else {
            Toast.makeText(this, "Booking ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        progressBar = findViewById(R.id.progressBar)
        tvBookingCode = findViewById(R.id.tvBookingCode)
        tvServiceType = findViewById(R.id.tvServiceType)
        tvDeviceType = findViewById(R.id.tvDeviceType)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusMessage = findViewById(R.id.tvStatusMessage)
        tvBookingDate = findViewById(R.id.tvBookingDate)
        tvTimeSlot = findViewById(R.id.tvTimeSlot)
        tvAddress = findViewById(R.id.tvAddress)
        tvLandmark = findViewById(R.id.tvLandmark)
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions)
        tvAddOns = findViewById(R.id.tvAddOns)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvTechnicianName = findViewById(R.id.tvTechnicianName)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)
        layoutActions = findViewById(R.id.layoutActions)
        btnCancel = findViewById(R.id.btnCancel)
        btnReschedule = findViewById(R.id.btnReschedule)
        layoutPhotos = findViewById(R.id.layoutPhotos)
        rvPhotos = findViewById(R.id.rvPhotos)
        tvNoPhotos = findViewById(R.id.tvNoPhotos)

        rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        btnCancel.setOnClickListener { showCancelDialog() }
        btnReschedule.setOnClickListener { showRescheduleDialog() }
    }

    private fun loadBookingDetails(bookingId: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBookingById(bookingId, userId)
                if (response.isSuccessful && response.body() != null) {
                    booking = response.body()!!
                    displayBookingDetails()
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

    private fun displayBookingDetails() {
        booking?.let { b ->
            val status = b.status.orEmpty()
            tvBookingCode.text = b.bookingCode ?: "N/A"
            tvServiceType.text = b.serviceType ?: "Service"
            tvDeviceType.text = b.deviceType?.uppercase() ?: "N/A"
            tvStatus.text = status.ifBlank { "unknown" }.uppercase()
            tvBookingDate.text = b.bookingDate ?: "N/A"
            tvTimeSlot.text = b.timeSlot ?: "N/A"
            tvAddress.text = b.address ?: "N/A"
            tvLandmark.text = if (b.landmark.isNullOrBlank()) "N/A" else b.landmark
            tvSpecialInstructions.text = if (b.specialInstructions.isNullOrBlank()) "None" else b.specialInstructions
            tvTotalAmount.text = "PHP ${String.format("%.2f", b.totalAmount ?: 0.0)}"
            
            // Handle technician name for old bookings
            tvTechnicianName.text = when {
                !b.technicianName.isNullOrBlank() -> b.technicianName
                status.lowercase() in listOf("completed", "cancelled", "voided") -> "Technician info not available"
                else -> "Not assigned yet"
            }
            
            tvCreatedAt.text = b.createdAt ?: "N/A"

            // Display add-ons
            if (!b.addOns.isNullOrEmpty()) {
                tvAddOns.text = b.addOns.joinToString("\n") { "• $it" }
            } else {
                tvAddOns.text = "No add-ons"
            }

            // Color code status
            val statusColor = when (status.lowercase()) {
                "pending" -> 0xFFFFA500.toInt()
                "confirmed" -> 0xFF4CAF50.toInt()
                "in_progress" -> 0xFF2196F3.toInt()
                "completed" -> 0xFF9C27B0.toInt()
                "cancelled", "voided" -> 0xFFF44336.toInt()
                else -> 0xFF94A3B8.toInt()
            }
            tvStatus.setTextColor(statusColor)

            // Status-specific messages
            val statusMessage = when (status.lowercase()) {
                "pending" -> "Your booking is waiting for a technician to accept it."
                "confirmed" -> "Great! A technician has accepted your booking."
                "in_progress" -> "Your service is currently in progress."
                "completed" -> "Your service has been completed successfully!"
                "cancelled" -> {
                    // Check if it's a no-show cancellation
                    if (b.statusReason?.lowercase()?.contains("no-show") == true || 
                        b.statusReason?.lowercase()?.contains("no show") == true) {
                        "⚠️ This booking was cancelled due to no-show. You were not available at the scheduled time."
                    } else if (!b.statusReason.isNullOrBlank()) {
                        "This booking has been cancelled. Reason: ${b.statusReason}"
                    } else {
                        "This booking has been cancelled."
                    }
                }
                "voided" -> {
                    if (!b.statusReason.isNullOrBlank()) {
                        "This booking was terminated by an administrator. Reason: ${b.statusReason}"
                    } else {
                        "This booking was terminated by an administrator."
                    }
                }
                else -> ""
            }
            tvStatusMessage.text = statusMessage
            tvStatusMessage.visibility = if (statusMessage.isNotEmpty()) View.VISIBLE else View.GONE

            // Show/hide action buttons based on status
            val canManage = status.lowercase() in listOf("pending", "confirmed")
            layoutActions.visibility = if (canManage) View.VISIBLE else View.GONE

            // Display photos if available
            if (!b.photos.isNullOrEmpty()) {
                layoutPhotos.visibility = View.VISIBLE
                val photosAdapter = PhotosAdapter(b.photos)
                rvPhotos.adapter = photosAdapter
                tvNoPhotos.visibility = View.GONE
            } else {
                layoutPhotos.visibility = View.GONE
            }
        }
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelBooking()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking() {
        booking?.let { b ->
            progressBar.visibility = View.VISIBLE
            btnCancel.isEnabled = false

            lifecycleScope.launch {
                try {
                    val request = mapOf(
                        "userId" to userId,
                        "reason" to "Cancelled by client"
                    )
                    val response = RetrofitClient.instance.cancelBooking(b.id.orEmpty(), request)
                    
                    if (response.isSuccessful) {
                        Toast.makeText(this@BookingDetailActivity,
                            "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@BookingDetailActivity,
                            "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                        btnCancel.isEnabled = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@BookingDetailActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnCancel.isEnabled = true
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun showRescheduleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reschedule, null)
        val etNewDate = dialogView.findViewById<EditText>(R.id.etNewDate)
        val rgTimeSlot = dialogView.findViewById<RadioGroup>(R.id.rgTimeSlot)
        val etReason = dialogView.findViewById<EditText>(R.id.etReason)

        var selectedDate = ""

        // Setup date picker
        etNewDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = dateFormat.format(selectedCalendar.time)
                
                val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                etNewDate.setText(displayFormat.format(selectedCalendar.time))
            }, year, month, day).apply {
                datePicker.minDate = calendar.timeInMillis
                show()
            }
        }

        // Setup time slots
        val timeSlots = listOf(
            "9:00 AM - 11:00 AM",
            "11:00 AM - 1:00 PM",
            "1:00 PM - 3:00 PM",
            "3:00 PM - 5:00 PM"
        )

        timeSlots.forEach { slot ->
            val radioButton = RadioButton(this)
            radioButton.id = View.generateViewId()
            radioButton.text = slot
            radioButton.setTextColor(resources.getColor(android.R.color.black, null))
            rgTimeSlot.addView(radioButton)
        }

        AlertDialog.Builder(this)
            .setTitle("Reschedule Booking")
            .setView(dialogView)
            .setPositiveButton("Reschedule") { _, _ ->
                val selectedTimeSlotId = rgTimeSlot.checkedRadioButtonId
                if (selectedDate.isEmpty()) {
                    Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (selectedTimeSlotId == -1) {
                    Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val timeSlot = dialogView.findViewById<RadioButton>(selectedTimeSlotId).text.toString()
                val reason = etReason.text.toString().trim().ifEmpty { "Rescheduled by client" }

                rescheduleBooking(selectedDate, timeSlot, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rescheduleBooking(newDate: String, newTimeSlot: String, reason: String) {
        booking?.let { b ->
            progressBar.visibility = View.VISIBLE
            btnReschedule.isEnabled = false

            lifecycleScope.launch {
                try {
                    val request = RescheduleBookingRequest(
                        requestedBy = userId,
                        newBookingDate = newDate,
                        newTimeSlot = newTimeSlot,
                        reason = reason
                    )
                    val response = RetrofitClient.instance.rescheduleBooking(b.id.orEmpty(), request)
                    
                    if (response.isSuccessful) {
                        Toast.makeText(this@BookingDetailActivity,
                            "Booking rescheduled successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@BookingDetailActivity,
                            "Failed to reschedule booking", Toast.LENGTH_SHORT).show()
                        btnReschedule.isEnabled = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@BookingDetailActivity,
                        "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnReschedule.isEnabled = true
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
