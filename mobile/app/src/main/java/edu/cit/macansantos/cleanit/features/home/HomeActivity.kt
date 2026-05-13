package edu.cit.macansantos.cleanit.features.home

import edu.cit.macansantos.cleanit.features.catalog.ServicesActivity

import edu.cit.macansantos.cleanit.features.booking.CreateBookingActivity

import edu.cit.macansantos.cleanit.features.booking.BookingDetailActivity

import edu.cit.macansantos.cleanit.features.auth.LoginActivity

import edu.cit.macansantos.cleanit.R

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.cit.macansantos.cleanit.features.booking.BookingsAdapter
import edu.cit.macansantos.cleanit.features.catalog.ServicesAdapter
import edu.cit.macansantos.cleanit.features.booking.Booking
import edu.cit.macansantos.cleanit.features.catalog.Service
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvContact: TextView
    private lateinit var tvVerificationStatus: TextView
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvErrorMessage: TextView
    private lateinit var tvSuccessMessage: TextView

    // Active Bookings Section
    private lateinit var rvActiveBookings: RecyclerView
    private lateinit var tvNoActiveBookings: TextView
    private lateinit var progressActiveBookings: ProgressBar

    // Services Section
    private lateinit var rvServices: RecyclerView
    private lateinit var tvVerificationLock: TextView
    private lateinit var progressServices: ProgressBar

    // Booking History Section
    private lateinit var rvBookingHistory: RecyclerView
    private lateinit var tvNoBookingHistory: TextView
    private lateinit var progressBookingHistory: ProgressBar

    private var userId: String = ""
    private var userName: String = ""
    private var userEmail: String = ""
    private var userRole: String = ""
    private var userContact: String = ""
    private var isVerified: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Get user data from intent
        userId = intent.getStringExtra("userId") ?: ""
        userName = intent.getStringExtra("name") ?: "User"
        userEmail = intent.getStringExtra("email") ?: ""
        userRole = intent.getStringExtra("role") ?: "client"
        userContact = intent.getStringExtra("contact") ?: ""
        isVerified = intent.getBooleanExtra("verified", false)

        initializeViews()
        setupUserProfile()
        setupSwipeRefresh()
        loadData()
    }

    private fun initializeViews() {
        // Swipe Refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        
        // User Profile
        tvWelcome = findViewById(R.id.tvWelcome)
        tvEmail = findViewById(R.id.tvEmail)
        tvContact = findViewById(R.id.tvContact)
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage)

        // Active Bookings
        rvActiveBookings = findViewById(R.id.rvActiveBookings)
        tvNoActiveBookings = findViewById(R.id.tvNoActiveBookings)
        progressActiveBookings = findViewById(R.id.progressActiveBookings)
        rvActiveBookings.layoutManager = LinearLayoutManager(this)

        // Services
        rvServices = findViewById(R.id.rvServices)
        tvVerificationLock = findViewById(R.id.tvVerificationLock)
        progressServices = findViewById(R.id.progressServices)
        rvServices.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Booking History
        rvBookingHistory = findViewById(R.id.rvBookingHistory)
        tvNoBookingHistory = findViewById(R.id.tvNoBookingHistory)
        progressBookingHistory = findViewById(R.id.progressBookingHistory)
        rvBookingHistory.layoutManager = LinearLayoutManager(this)

        // Logout button
        btnLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        
        // View All Services button
        findViewById<Button>(R.id.btnViewAllServices).setOnClickListener {
            startActivity(Intent(this, ServicesActivity::class.java).apply {
                putExtra("userId", userId)
            })
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(0xFF7C3AED.toInt())
        swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupUserProfile() {
        tvWelcome.text = "Welcome, $userName!"
        tvEmail.text = userEmail
        tvContact.text = "📞 $userContact"
        
        // Verification status
        if (isVerified) {
            tvVerificationStatus.text = "Verified"
            tvVerificationStatus.setBackgroundResource(R.drawable.badge_verified)
        } else {
            tvVerificationStatus.text = "Unverified"
            tvVerificationStatus.setBackgroundResource(R.drawable.badge_unverified)
        }
    }

    private fun loadData() {
        loadActiveBookings()
        loadServices()
        loadBookingHistory()
    }

    private fun loadActiveBookings() {
        progressActiveBookings.visibility = View.VISIBLE
        tvNoActiveBookings.visibility = View.GONE
        rvActiveBookings.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getClientBookings(userId)
                if (response.isSuccessful && response.body() != null) {
                    val allBookings = response.body()!!
                    val activeBookings = allBookings.filter { 
                        it.status in listOf("pending", "confirmed", "in_progress")
                    }

                    if (activeBookings.isEmpty()) {
                        tvNoActiveBookings.visibility = View.VISIBLE
                        rvActiveBookings.visibility = View.GONE
                    } else {
                        val adapter = BookingsAdapter(activeBookings) { booking ->
                            openBookingDetail(booking)
                        }
                        rvActiveBookings.adapter = adapter
                        rvActiveBookings.visibility = View.VISIBLE
                        tvNoActiveBookings.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                showError("Failed to load active bookings: ${e.message}")
            } finally {
                progressActiveBookings.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadServices() {
        progressServices.visibility = View.VISIBLE
        rvServices.visibility = View.GONE
        tvVerificationLock.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getServices()
                if (response.isSuccessful && response.body() != null) {
                    val services = response.body()!!.take(4) // Show first 4 services

                    if (!isVerified) {
                        tvVerificationLock.visibility = View.VISIBLE
                    }

                    val adapter = ServicesAdapter(services) { service ->
                        if (isVerified) {
                            // Navigate to booking creation
                            startActivity(Intent(this@HomeActivity, CreateBookingActivity::class.java).apply {
                                putExtra("serviceId", service.id)
                                putExtra("userId", userId)
                            })
                        } else {
                            Toast.makeText(this@HomeActivity,
                                "Please wait for admin verification", Toast.LENGTH_SHORT).show()
                        }
                    }
                    rvServices.adapter = adapter
                    rvServices.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                showError("Failed to load services: ${e.message}")
            } finally {
                progressServices.visibility = View.GONE
            }
        }
    }

    private fun loadBookingHistory() {
        progressBookingHistory.visibility = View.VISIBLE
        tvNoBookingHistory.visibility = View.GONE
        rvBookingHistory.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getClientBookings(userId)
                if (response.isSuccessful && response.body() != null) {
                    val allBookings = response.body()!!
                    val historyBookings = allBookings.filter { 
                        it.status in listOf("completed", "cancelled", "voided")
                    }

                    if (historyBookings.isEmpty()) {
                        tvNoBookingHistory.visibility = View.VISIBLE
                        rvBookingHistory.visibility = View.GONE
                    } else {
                        val adapter = BookingsAdapter(historyBookings) { booking ->
                            openBookingDetail(booking)
                        }
                        rvBookingHistory.adapter = adapter
                        rvBookingHistory.visibility = View.VISIBLE
                        tvNoBookingHistory.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                showError("Failed to load booking history: ${e.message}")
            } finally {
                progressBookingHistory.visibility = View.GONE
            }
        }
    }

    private fun openBookingDetail(booking: Booking) {
        startActivity(Intent(this, BookingDetailActivity::class.java).apply {
            putExtra("bookingId", booking.id.orEmpty())
            putExtra("userId", userId)
        })
    }

    private fun showError(message: String) {
        tvErrorMessage.text = message
        tvErrorMessage.visibility = View.VISIBLE
        tvSuccessMessage.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        tvSuccessMessage.text = message
        tvSuccessMessage.visibility = View.VISIBLE
        tvErrorMessage.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        loadData()
    }
}
