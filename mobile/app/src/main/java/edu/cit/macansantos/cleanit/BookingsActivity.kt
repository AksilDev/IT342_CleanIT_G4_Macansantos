package edu.cit.macansantos.cleanit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.adapter.BookingsAdapter
import edu.cit.macansantos.cleanit.model.Booking
import edu.cit.macansantos.cleanit.network.RetrofitClient
import kotlinx.coroutines.launch

class BookingsActivity : AppCompatActivity() {

    private lateinit var spinnerFilter: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: BookingsAdapter
    
    private var allBookings: List<Booking> = emptyList()
    private var filteredBookings: List<Booking> = emptyList()
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookings)

        supportActionBar?.title = "My Bookings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spinnerFilter = findViewById(R.id.spinnerFilter)
        recyclerView = findViewById(R.id.recyclerViewBookings)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        userId = intent.getStringExtra("userId") ?: ""
        if (userId.isNotEmpty()) {
            setupFilter()
            loadBookings(userId)
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupFilter() {
        val filterOptions = arrayOf("All", "Pending", "Confirmed", "In Progress", "Completed", "Cancelled")
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = arrayAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterBookings(filterOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterBookings(filter: String) {
        filteredBookings = when (filter) {
            "All" -> allBookings
            "Pending" -> allBookings.filter { it.status.equals("pending", ignoreCase = true) }
            "Confirmed" -> allBookings.filter { it.status.equals("confirmed", ignoreCase = true) }
            "In Progress" -> allBookings.filter { it.status.equals("in_progress", ignoreCase = true) }
            "Completed" -> allBookings.filter { it.status.equals("completed", ignoreCase = true) }
            "Cancelled" -> allBookings.filter { it.status.equals("cancelled", ignoreCase = true) || it.status.equals("voided", ignoreCase = true) }
            else -> allBookings
        }
        updateAdapter()
    }

    private fun updateAdapter() {
        if (filteredBookings.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            adapter = BookingsAdapter(filteredBookings) { booking ->
                // Navigate to booking detail
                startActivity(Intent(this@BookingsActivity, BookingDetailActivity::class.java).apply {
                    putExtra("bookingId", booking.id)
                    putExtra("userId", userId)
                })
            }
            recyclerView.adapter = adapter
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun loadBookings(userId: String) {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getClientBookings(userId)
                if (response.isSuccessful && response.body() != null) {
                    allBookings = response.body()!!
                    filteredBookings = allBookings
                    updateAdapter()
                } else {
                    Toast.makeText(this@BookingsActivity,
                        "Failed to load bookings", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BookingsActivity,
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
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