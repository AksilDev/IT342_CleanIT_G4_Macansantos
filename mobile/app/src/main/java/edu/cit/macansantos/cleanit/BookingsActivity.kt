package edu.cit.macansantos.cleanit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.adapter.BookingsAdapter
import edu.cit.macansantos.cleanit.network.RetrofitClient
import kotlinx.coroutines.launch

class BookingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: BookingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookings)

        supportActionBar?.title = "My Bookings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerViewBookings)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val userId = intent.getStringExtra("userId") ?: ""
        if (userId.isNotEmpty()) {
            loadBookings(userId)
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadBookings(userId: String) {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getClientBookings(userId)
                if (response.isSuccessful && response.body() != null) {
                    val bookings = response.body()!!
                    if (bookings.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        adapter = BookingsAdapter(bookings) { booking ->
                            // Navigate to booking detail
                            startActivity(Intent(this@BookingsActivity, BookingDetailActivity::class.java).apply {
                                putExtra("bookingId", booking.id)
                            })
                        }
                        recyclerView.adapter = adapter
                        recyclerView.visibility = View.VISIBLE
                    }
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
