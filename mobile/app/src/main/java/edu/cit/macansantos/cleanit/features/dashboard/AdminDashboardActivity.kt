package edu.cit.macansantos.cleanit.features.dashboard

import edu.cit.macansantos.cleanit.features.auth.LoginActivity

import edu.cit.macansantos.cleanit.R

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import edu.cit.macansantos.cleanit.shared.session.SessionManager
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvStats: TextView
    private lateinit var tvMessage: TextView
    private lateinit var statsActionsContainer: LinearLayout
    private lateinit var pendingUsersContainer: LinearLayout
    private lateinit var recentBookingsContainer: LinearLayout

    private var latestStats: AdminDashboardStatistics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        findViewById<TextView>(R.id.tvEmail).text = intent.getStringExtra("email").orEmpty()
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tvStats = findViewById(R.id.tvStats)
        tvMessage = findViewById(R.id.tvMessage)
        statsActionsContainer = findViewById(R.id.statsActionsContainer)
        pendingUsersContainer = findViewById(R.id.pendingUsersContainer)
        recentBookingsContainer = findViewById(R.id.recentBookingsContainer)

        swipeRefreshLayout.setOnRefreshListener { loadDashboard() }
        loadDashboard()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            try {
                val statsResponse = RetrofitClient.instance.getAdminDashboardStatistics()
                if (statsResponse.isSuccessful) {
                    latestStats = statsResponse.body()
                    renderStats(latestStats)
                    renderStatActions(latestStats)
                }

                renderUsers(RetrofitClient.instance.getPendingVerifications().body().orEmpty())
                renderBookings(RetrofitClient.instance.getAdminRecentBookings(10).body().orEmpty())
                tvMessage.visibility = View.GONE
            } catch (e: Exception) {
                showMessage("Failed to load admin dashboard: ${e.message}")
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun renderStats(stats: AdminDashboardStatistics?) {
        tvStats.text = listOf(
            "Total bookings: ${stats?.total ?: 0}",
            "Pending: ${stats?.pending ?: 0}",
            "Confirmed: ${stats?.confirmed ?: 0}",
            "In progress: ${stats?.inProgress ?: 0}",
            "Completed: ${stats?.completed ?: 0}",
            "Cancelled: ${stats?.cancelled ?: 0}",
            "Revenue: PHP ${"%.2f".format(stats?.totalRevenue ?: 0.0)}",
            "This month: PHP ${"%.2f".format(stats?.monthRevenue ?: 0.0)}"
        ).joinToString("\n")
    }

    private fun renderStatActions(stats: AdminDashboardStatistics?) {
        statsActionsContainer.removeAllViews()

        val activeCount = stats?.activeBookings
            ?: ((stats?.pending ?: 0) + (stats?.confirmed ?: 0) + (stats?.inProgress ?: 0))
        val confirmedCount = stats?.confirmedBookings ?: (stats?.confirmed ?: 0)

        statsActionsContainer.addView(
            statActionButton(
                "Active Bookings ($activeCount)",
                "pending,confirmed,in_progress"
            )
        )
        statsActionsContainer.addView(
            statActionButton(
                "Confirmed Bookings ($confirmedCount)",
                "confirmed"
            )
        )
    }

    private fun statActionButton(label: String, statuses: String): Button {
        return Button(this).apply {
            text = "$label →"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF7C3AED.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
            setOnClickListener {
                startActivity(Intent(this@AdminDashboardActivity, AdminBookingsActivity::class.java).apply {
                    putExtra(AdminBookingsActivity.EXTRA_TITLE, label.substringBefore(" ("))
                    putExtra(AdminBookingsActivity.EXTRA_STATUSES, statuses)
                })
            }
        }
    }

    private fun renderUsers(users: List<VerificationUser>) {
        pendingUsersContainer.removeAllViews()

        if (users.isEmpty()) {
            pendingUsersContainer.addView(textBlock("No pending verifications."))
            return
        }

        users.forEach { user ->
            val card = dashboardCard()
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            content.addView(textBlock(listOf(
                user.name ?: "Unnamed user",
                user.email ?: "No email",
                "Role: ${user.role ?: "unknown"}",
                "Contact: ${user.contactNo ?: "N/A"}"
            ).joinToString("\n")))

            val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            buttons.addView(actionButton("Approve", 0xFF16A34A.toInt()) {
                verifyUser(user.id, "approved")
            })
            buttons.addView(actionButton("Reject", 0xFFEF4444.toInt()) {
                verifyUser(user.id, "rejected")
            })
            content.addView(buttons)

            card.addView(content)
            pendingUsersContainer.addView(card)
        }
    }

    private fun renderBookings(bookings: List<DashboardBooking>) {
        recentBookingsContainer.removeAllViews()

        if (bookings.isEmpty()) {
            recentBookingsContainer.addView(textBlock("No recent bookings."))
            return
        }

        bookings.forEach { booking ->
            val card = dashboardCard()
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            content.addView(textBlock(listOf(
                booking.bookingCode ?: "Booking",
                "${booking.serviceType ?: "Service"} - ${booking.deviceType ?: "Device"}",
                "Client: ${booking.clientName ?: "N/A"}",
                "Technician: ${booking.technicianName ?: "Unassigned"}",
                "Status: ${booking.status ?: "unknown"}",
                "Schedule: ${booking.bookingDate ?: "TBD"} ${booking.timeSlot ?: ""}"
            ).joinToString("\n")))

            if (booking.status in listOf("pending", "confirmed", "in_progress")) {
                content.addView(actionButton("Manage / Void", 0xFFEF4444.toInt()) {
                    val statuses = when (booking.status) {
                        "pending" -> "pending"
                        "confirmed" -> "confirmed"
                        else -> "in_progress"
                    }
                    startActivity(Intent(this@AdminDashboardActivity, AdminBookingsActivity::class.java).apply {
                        putExtra(AdminBookingsActivity.EXTRA_TITLE, "Booking Management")
                        putExtra(AdminBookingsActivity.EXTRA_STATUSES, statuses)
                    })
                })
            }

            card.addView(content)
            recentBookingsContainer.addView(card)
        }
    }

    private fun verifyUser(userId: String?, status: String) {
        if (userId.isNullOrBlank()) {
            showMessage("Missing user ID")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.verifyUser(userId, mapOf("status" to status))
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminDashboardActivity, "User $status", Toast.LENGTH_SHORT).show()
                    loadDashboard()
                } else {
                    showMessage("Failed to update verification")
                }
            } catch (e: Exception) {
                showMessage("Failed to update verification: ${e.message}")
            }
        }
    }

    private fun dashboardCard(): CardView {
        return CardView(this).apply {
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(0xFF1E293B.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
    }

    private fun actionButton(label: String, color: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setOnClickListener { onClick() }
        }
    }

    private fun textBlock(message: String): TextView {
        return TextView(this).apply {
            text = message
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(8, 8, 8, 8)
        }
    }

    private fun showMessage(message: String) {
        tvMessage.text = message
        tvMessage.visibility = View.VISIBLE
    }

    private fun logout() {
        SessionManager.clearSession(SessionManager.prefs(this))
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
