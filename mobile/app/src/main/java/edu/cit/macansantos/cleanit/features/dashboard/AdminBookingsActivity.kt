package edu.cit.macansantos.cleanit.features.dashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import kotlinx.coroutines.launch

class AdminBookingsActivity : AppCompatActivity() {

    private lateinit var adapter: AdminBookingAdapter
    private lateinit var etSearch: EditText
    private var statuses: String = ""
    private var voidingId: String? = null
    private var allBookings: List<AdminBookingDetail> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_bookings)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        statuses = intent.getStringExtra(EXTRA_STATUSES).orEmpty()

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<TextView>(R.id.tvTitle).text = title

        adapter = AdminBookingAdapter(
            onVoid = { booking -> confirmVoid(booking) },
            isVoiding = { id -> voidingId == id }
        )

        findViewById<RecyclerView>(R.id.rvBookings).apply {
            layoutManager = LinearLayoutManager(this@AdminBookingsActivity)
            this.adapter = this@AdminBookingsActivity.adapter
        }

        etSearch = findViewById(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadBookings()
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) {
            allBookings
        } else {
            val q = query.lowercase()
            allBookings.filter { booking ->
                listOfNotNull(
                    booking.bookingCode,
                    booking.clientName,
                    booking.clientEmail,
                    booking.technicianName,
                    booking.serviceType
                ).any { it.lowercase().contains(q) }
            }
        }
        adapter.submit(filtered, voidingId)
        findViewById<RecyclerView>(R.id.rvBookings).visibility =
            if (filtered.isEmpty()) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.tvEmpty).visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadBookings() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val rv = findViewById<RecyclerView>(R.id.rvBookings)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)

        progressBar.visibility = View.VISIBLE
        rv.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        tvMessage.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getAdminBookingsByStatus(statuses)
                if (response.isSuccessful) {
                    allBookings = response.body()?.bookings.orEmpty()
                    applyFilter(etSearch.text.toString())
                    if (allBookings.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rv.visibility = View.VISIBLE
                    }
                } else {
                    showMessage(tvMessage, "Failed to load bookings")
                }
            } catch (e: Exception) {
                showMessage(tvMessage, "Failed to load bookings: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun confirmVoid(booking: AdminBookingDetail) {
        val bookingId = booking.id ?: return
        val code = booking.bookingCode ?: bookingId

        AlertDialog.Builder(this)
            .setTitle("Void booking")
            .setMessage(
                "Void booking $code? This cancels the booking and notifies the client and technician."
            )
            .setPositiveButton("Void") { _, _ -> voidBooking(bookingId, code) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun voidBooking(bookingId: String, bookingCode: String) {
        voidingId = bookingId
        adapter.notifyDataSetChanged()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.voidAdminBooking(bookingId)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@AdminBookingsActivity,
                        "Booking $bookingCode voided",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadBookings()
                } else {
                    Toast.makeText(
                        this@AdminBookingsActivity,
                        "Failed to void booking",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminBookingsActivity,
                    "Failed to void booking: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                voidingId = null
            }
        }
    }

    private fun showMessage(tvMessage: TextView, message: String) {
        tvMessage.text = message
        tvMessage.visibility = View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_STATUSES = "statuses"
    }
}

private class AdminBookingAdapter(
    private val onVoid: (AdminBookingDetail) -> Unit,
    private val isVoiding: (String?) -> Boolean
) : RecyclerView.Adapter<AdminBookingAdapter.ViewHolder>() {

    private var bookings: List<AdminBookingDetail> = emptyList()

    fun submit(items: List<AdminBookingDetail>, voidingId: String?) {
        bookings = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val card = CardView(parent.context).apply {
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(0xFF1E293B.toInt())
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
        val container = android.widget.LinearLayout(parent.context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        card.addView(container)
        return ViewHolder(card, container)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(bookings[position], onVoid, isVoiding)
    }

    override fun getItemCount(): Int = bookings.size

    class ViewHolder(
        private val card: CardView,
        private val container: android.widget.LinearLayout
    ) : RecyclerView.ViewHolder(card) {

        fun bind(
            booking: AdminBookingDetail,
            onVoid: (AdminBookingDetail) -> Unit,
            isVoiding: (String?) -> Boolean
        ) {
            container.removeAllViews()

            val details = listOfNotNull(
                booking.bookingCode,
                "Status: ${booking.status?.uppercase()?.replace('_', ' ')}",
                "Client: ${booking.clientName ?: "N/A"} (${booking.clientEmail ?: ""})",
                "Technician: ${booking.technicianName ?: "Unassigned"}",
                "Service: ${booking.serviceType ?: "N/A"} · ${booking.deviceType ?: ""}",
                booking.addOns?.takeIf { it.isNotEmpty() }?.let { "Add-ons: ${it.joinToString()}" },
                "Schedule: ${booking.bookingDate ?: "TBD"} ${booking.timeSlot ?: ""}",
                "Amount: PHP ${"%.2f".format(booking.totalAmount ?: 0.0)}",
                booking.address?.let { "Address: $it" },
                booking.specialInstructions?.let { "Notes: $it" }
            ).joinToString("\n")

            container.addView(TextView(card.context).apply {
                text = details
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 13f
            })

            val canVoid = booking.status in listOf("pending", "confirmed", "in_progress")
            if (canVoid) {
                container.addView(Button(card.context).apply {
                    text = if (isVoiding(booking.id)) "Voiding..." else "Void / Terminate"
                    isEnabled = !isVoiding(booking.id)
                    setBackgroundColor(0xFFEF4444.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                    setOnClickListener { onVoid(booking) }
                })
            }
        }
    }
}
