package edu.cit.macansantos.cleanit.features.dashboard

import edu.cit.macansantos.cleanit.features.auth.LoginActivity

import edu.cit.macansantos.cleanit.R

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import coil.transform.RoundedCornersTransformation
import edu.cit.macansantos.cleanit.features.dashboard.DashboardBooking
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianAvailabilityRequest
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianBookingActionRequest
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianBookingPhoto
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianChecklistItem
import edu.cit.macansantos.cleanit.features.dashboard.TechnicianStatusUpdateRequest
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import edu.cit.macansantos.cleanit.shared.session.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class TechnicianDashboardActivity : AppCompatActivity() {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvWelcome: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnToggleAvailability: Button
    private lateinit var btnOverview: Button
    private lateinit var btnPending: Button
    private lateinit var btnMyBookings: Button
    private lateinit var statsGrid: GridLayout
    private lateinit var overviewSection: LinearLayout
    private lateinit var pendingSection: LinearLayout
    private lateinit var myBookingsSection: LinearLayout
    private lateinit var overviewContainer: LinearLayout
    private lateinit var pendingBookingsContainer: LinearLayout
    private lateinit var activeBookingsContainer: LinearLayout
    private lateinit var historyBookingsContainer: LinearLayout

    private var technicianId = ""
    private var technicianEmail = ""
    private var technicianName = "Technician"
    private var isAvailable = true
    private var currentTab = "overview"
    private var assignedBookings: List<DashboardBooking> = emptyList()
    private var pendingBookings: List<DashboardBooking> = emptyList()
    private var uploadBookingId: String? = null
    private var uploadPhotoType: String? = null
    private var detailDialog: AlertDialog? = null
    private var detailContent: LinearLayout? = null
    private var detailBooking: DashboardBooking? = null

    companion object {
        private const val RC_PICK_PHOTO = 4001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_technician_dashboard)

        technicianId = intent.getStringExtra("userId").orEmpty()
        technicianEmail = intent.getStringExtra("email").orEmpty()
        technicianName = intent.getStringExtra("name") ?: "Technician"

        bindViews()
        setupHeader()
        setupActions()
        loadDashboard()
    }

    private fun bindViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        tvWelcome = findViewById(R.id.tvWelcome)
        tvAvatar = findViewById(R.id.tvAvatar)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvStatus = findViewById(R.id.tvStatus)
        tvMessage = findViewById(R.id.tvMessage)
        btnToggleAvailability = findViewById(R.id.btnToggleAvailability)
        btnOverview = findViewById(R.id.btnOverview)
        btnPending = findViewById(R.id.btnPending)
        btnMyBookings = findViewById(R.id.btnMyBookings)
        statsGrid = findViewById(R.id.statsGrid)
        overviewSection = findViewById(R.id.overviewSection)
        pendingSection = findViewById(R.id.pendingSection)
        myBookingsSection = findViewById(R.id.myBookingsSection)
        overviewContainer = findViewById(R.id.overviewContainer)
        pendingBookingsContainer = findViewById(R.id.pendingBookingsContainer)
        activeBookingsContainer = findViewById(R.id.activeBookingsContainer)
        historyBookingsContainer = findViewById(R.id.historyBookingsContainer)
    }

    private fun setupHeader() {
        tvWelcome.text = "Welcome Back, $technicianName"
        tvName.text = technicianName
        tvEmail.text = technicianEmail
        tvAvatar.text = initials(technicianName)
    }

    private fun setupActions() {
        findViewById<Button>(R.id.btnLogout).setOnClickListener { logout() }
        btnToggleAvailability.setOnClickListener { updateAvailability(!isAvailable) }
        btnOverview.setOnClickListener { showTab("overview") }
        btnPending.setOnClickListener { showTab("pending") }
        btnMyBookings.setOnClickListener { showTab("my-bookings") }
        swipeRefreshLayout.setColorSchemeColors(0xFF7C3AED.toInt())
        swipeRefreshLayout.setOnRefreshListener { loadDashboard() }
        showTab("overview")
    }

    private fun loadDashboard() {
        if (technicianId.isBlank()) {
            showMessage("Missing technician ID. Please log in again.", true)
            swipeRefreshLayout.isRefreshing = false
            return
        }

        lifecycleScope.launch {
            try {
                refreshUserProfile()
                val statsResponse = RetrofitClient.instance.getTechnicianStatistics(technicianId)
                val availabilityResponse = RetrofitClient.instance.getTechnicianAvailability(technicianId)
                val assignedResponse = RetrofitClient.instance.getTechnicianBookings(technicianId)
                val pendingResponse = RetrofitClient.instance.getPendingTechnicianBookings(technicianId)

                if (availabilityResponse.isSuccessful) {
                    isAvailable = availabilityResponse.body()?.isAvailable ?: true
                    renderAvailability()
                }

                assignedBookings = assignedResponse.body().orEmpty()
                pendingBookings = pendingResponse.body().orEmpty()

                val stats = statsResponse.body()
                renderStats(
                    listOf(
                        "Active" to "${stats?.active ?: activeBookings().size}",
                        "Pending" to "${pendingBookings.size}",
                        "Confirmed" to "${stats?.confirmed ?: countStatus("confirmed")}",
                        "In Progress" to "${stats?.inProgress ?: countStatus("in_progress")}",
                        "Completed" to "${stats?.completed ?: countStatus("completed")}"
                    )
                )

                renderOverview()
                renderPending()
                renderMyBookings()
                showMessage("", false)
            } catch (e: Exception) {
                showMessage("Failed to load technician dashboard: ${e.message}", true)
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateAvailability(available: Boolean) {
        btnToggleAvailability.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.setTechnicianAvailability(
                    technicianId,
                    TechnicianAvailabilityRequest(available)
                )
                if (response.isSuccessful) {
                    isAvailable = response.body()?.isAvailable ?: available
                    renderAvailability()
                    showMessage("You are now ${if (isAvailable) "available" else "unavailable"} for new bookings.", false)
                } else {
                    showMessage("Failed to update availability", true)
                }
            } catch (e: Exception) {
                showMessage("Failed to update availability: ${e.message}", true)
            } finally {
                btnToggleAvailability.isEnabled = true
            }
        }
    }

    private fun acceptBooking(bookingId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.acceptTechnicianBooking(
                    bookingId,
                    TechnicianBookingActionRequest(technicianId)
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@TechnicianDashboardActivity, "Booking accepted", Toast.LENGTH_SHORT).show()
                    loadDashboard()
                    showTab("my-bookings")
                } else {
                    showMessage(response.errorBody()?.string() ?: "Failed to accept booking", true)
                }
            } catch (e: Exception) {
                showMessage("Failed to accept booking: ${e.message}", true)
            }
        }
    }

    private suspend fun refreshUserProfile() {
        if (technicianEmail.isBlank()) return
        val response = RetrofitClient.instance.getUserProfile(technicianEmail)
        if (response.isSuccessful && response.body() != null) {
            val profile = response.body()!!
            technicianId = profile.id ?: technicianId
            technicianName = profile.name ?: technicianName
            technicianEmail = profile.email ?: technicianEmail
            SessionManager.saveUserProfile(SessionManager.prefs(this), profile)
            runOnUiThread { setupHeader() }
        }
    }

    private fun updateBookingStatus(bookingId: String, newStatus: String) {
        lifecycleScope.launch {
            try {
                if (newStatus == "completed") {
                    val checklistResponse = RetrofitClient.instance.validateChecklist(bookingId)
                    if (!checklistResponse.isSuccessful || checklistResponse.body()?.isComplete != true) {
                        val items = checklistResponse.body()?.incompleteItems.orEmpty()
                        val detail = if (items.isNotEmpty()) {
                            "\n${items.take(3).joinToString(", ")}"
                        } else ""
                        showMessage(
                            "Cannot complete service. Checklist incomplete.$detail",
                            true
                        )
                        return@launch
                    }

                    val photosResponse = RetrofitClient.instance.validatePhotos(bookingId)
                    if (!photosResponse.isSuccessful || photosResponse.body()?.hasRequiredPhotos != true) {
                        val missing = photosResponse.body()?.missingRequirements.orEmpty()
                        showMessage(
                            "Cannot complete service. ${missing.joinToString("\n")}",
                            true
                        )
                        return@launch
                    }
                }

                val response = RetrofitClient.instance.updateTechnicianBookingStatus(
                    bookingId,
                    TechnicianStatusUpdateRequest(
                        status = newStatus,
                        technicianId = technicianId,
                        reason = "Status updated to $newStatus from mobile"
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@TechnicianDashboardActivity, "Status updated", Toast.LENGTH_SHORT).show()
                    loadDashboard()
                } else {
                    showMessage(response.errorBody()?.string() ?: "Failed to update booking status", true)
                }
            } catch (e: Exception) {
                showMessage("Failed to update booking status: ${e.message}", true)
            }
        }
    }

    private fun renderAvailability() {
        tvStatus.text = if (isAvailable) "Available" else "Unavailable"
        tvStatus.setTextColor(if (isAvailable) 0xFF047857.toInt() else 0xFF64748B.toInt())
        btnToggleAvailability.text = if (isAvailable) "Set Unavailable" else "Set Available"
        btnToggleAvailability.setBackgroundColor(if (isAvailable) 0xFF10B981.toInt() else 0xFF7C3AED.toInt())
    }

    private fun renderStats(items: List<Pair<String, String>>) {
        statsGrid.removeAllViews()
        items.forEach { (label, value) ->
            statsGrid.addView(statCard(label, value))
        }
    }

    private fun renderOverview() {
        overviewContainer.removeAllViews()
        val active = activeBookings()
        val nextBooking = active.firstOrNull()

        overviewContainer.addView(sectionCard(
            "Next Active Booking",
            nextBooking?.let { formatBookingSummary(it, includeAddress = true) } ?: "No active bookings right now.",
            nextBooking?.let { statusLabel(it.status) },
            nextBooking
        ))

        overviewContainer.addView(sectionCard(
            "Recent Pending Request",
            pendingBookings.firstOrNull()?.let { formatBookingSummary(it, includeAddress = false) }
                ?: "No pending requests assigned to you.",
            pendingBookings.firstOrNull()?.let { statusLabel(it.status) },
            pendingBookings.firstOrNull(),
            actionLabel = pendingBookings.firstOrNull()?.let { "Accept" },
            action = pendingBookings.firstOrNull()?.id?.let { id -> { acceptBooking(id) } }
        ))
    }

    private fun renderPending() {
        pendingBookingsContainer.removeAllViews()
        if (pendingBookings.isEmpty()) {
            pendingBookingsContainer.addView(emptyState("No pending requests", "Assigned pending requests will appear here."))
            return
        }

        pendingBookings.forEach { booking ->
            pendingBookingsContainer.addView(sectionCard(
                title = booking.bookingCode ?: "Pending Request",
                body = formatBookingSummary(booking, includeAddress = false),
                chip = statusLabel(booking.status),
                booking = booking,
                actionLabel = "Accept Booking",
                action = booking.id?.let { id -> { acceptBooking(id) } }
            ))
        }
    }

    private fun renderMyBookings() {
        activeBookingsContainer.removeAllViews()
        historyBookingsContainer.removeAllViews()

        val active = activeBookings()
        if (active.isEmpty()) {
            activeBookingsContainer.addView(emptyState("No active bookings", "Confirmed and in-progress bookings will appear here."))
        } else {
            active.forEach { booking ->
                activeBookingsContainer.addView(sectionCard(
                    title = booking.bookingCode ?: "Booking",
                    body = formatBookingSummary(booking, includeAddress = true),
                    chip = statusLabel(booking.status),
                    booking = booking,
                    actionLabel = primaryActionLabel(booking),
                    action = primaryAction(booking)
                ))
            }
        }

        val history = assignedBookings.filter { it.status in listOf("completed", "cancelled", "no_show") }
        if (history.isEmpty()) {
            historyBookingsContainer.addView(emptyState("No booking history", "Completed, cancelled, and no-show bookings will appear here."))
        } else {
            history.forEach { booking ->
                historyBookingsContainer.addView(sectionCard(
                    title = booking.bookingCode ?: "Booking",
                    body = formatBookingSummary(booking, includeAddress = true),
                    chip = statusLabel(booking.status),
                    booking = booking
                ))
            }
        }
    }

    private fun sectionCard(
        title: String,
        body: String,
        chip: String? = null,
        booking: DashboardBooking? = null,
        actionLabel: String? = null,
        action: (() -> Unit)? = null
    ): View {
        val card = CardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = title
            setTextColor(0xFF0F172A.toInt())
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (!chip.isNullOrBlank()) {
            top.addView(statusChip(chip, booking?.status))
        }
        content.addView(top)

        content.addView(TextView(this).apply {
            text = body
            setTextColor(0xFF475569.toInt())
            textSize = 12f
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, dp(8), 0, 0)
        })

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }

        if (booking != null) {
            buttons.addView(smallButton("View Details", 0xFFEDE9FE.toInt(), 0xFF6D28D9.toInt()) {
                showBookingDetails(booking)
            })
        }
        if (actionLabel != null && action != null) {
            buttons.addView(smallButton(actionLabel, actionColor(actionLabel), Color.WHITE, action))
        }
        if (buttons.childCount > 0) content.addView(buttons)

        card.addView(content)
        return card
    }

    private fun showBookingDetails(booking: DashboardBooking) {
        detailBooking = booking
        val scrollView = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }
        scrollView.addView(content)
        detailContent = content

        detailDialog = AlertDialog.Builder(this)
            .setTitle("Booking Details")
            .setView(scrollView)
            .setNegativeButton("Close", null)
            .create()

        detailDialog?.setOnShowListener {
            renderBookingDetailContent(booking, content, emptyList(), emptyList(), loading = true)
            loadBookingDetailSupportData(booking, content)
        }
        detailDialog?.show()
    }

    private fun loadBookingDetailSupportData(booking: DashboardBooking, content: LinearLayout) {
        val bookingId = booking.id ?: return
        lifecycleScope.launch {
            try {
                val checklist = RetrofitClient.instance.getTechnicianChecklist(bookingId).body().orEmpty()
                val photos = RetrofitClient.instance.getTechnicianPhotos(bookingId).body().orEmpty()
                renderBookingDetailContent(booking, content, checklist, photos, loading = false)
            } catch (e: Exception) {
                renderBookingDetailContent(booking, content, emptyList(), emptyList(), loading = false)
                showMessage("Failed to load booking details: ${e.message}", true)
            }
        }
    }

    private fun renderBookingDetailContent(
        booking: DashboardBooking,
        content: LinearLayout,
        checklist: List<TechnicianChecklistItem>,
        photos: List<TechnicianBookingPhoto>,
        loading: Boolean
    ) {
        content.removeAllViews()

        content.addView(detailBlock("Booking Code", "#${booking.bookingCode ?: "Booking"}", 0xFFF5F3FF.toInt()))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        row.addView(detailMiniBlock("Service", booking.serviceType ?: "Service"))
        row.addView(detailMiniBlock("Device", booking.deviceType ?: "Device"))
        content.addView(row)

        content.addView(statusChip(statusLabel(booking.status), booking.status).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
        })

        val scheduleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        scheduleRow.addView(detailMiniBlock("Date", booking.bookingDate ?: "TBD"))
        scheduleRow.addView(detailMiniBlock("Time", booking.timeSlot ?: "TBD"))
        content.addView(scheduleRow)

        content.addView(detailBlock(
            "Address",
            listOfNotNull(
                booking.address ?: "Not available",
                booking.landmark?.takeIf { it.isNotBlank() }?.let { "Landmark: $it" }
            ).joinToString("\n"),
            0xFFF8FAFC.toInt()
        ))

        if (booking.status == "in_progress" || booking.status == "confirmed") {
            content.addView(checklistPanel(booking, checklist, loading))
        }

        if (booking.status == "in_progress" || booking.status == "completed") {
            content.addView(photoPanel(booking, photos, loading))
        }

        content.addView(detailBlock(
            "Total Amount",
            "PHP ${"%,.2f".format(booking.totalAmount ?: 0.0)}",
            0xFFF5F3FF.toInt(),
            valueColor = 0xFF7C3AED.toInt(),
            valueSize = 20f
        ))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        when (booking.status) {
            "confirmed" -> {
                actions.addView(fullWidthButton("Start Service", 0xFF2563EB.toInt()) {
                    booking.id?.let { updateBookingStatus(it, "in_progress") }
                    detailDialog?.dismiss()
                })
                actions.addView(fullWidthButton("Mark No Show", 0xFF64748B.toInt()) {
                    booking.id?.let { updateBookingStatus(it, "no_show") }
                    detailDialog?.dismiss()
                })
            }
            "in_progress" -> {
                actions.addView(fullWidthButton("Mark Complete", 0xFF059669.toInt()) {
                    booking.id?.let { updateBookingStatus(it, "completed") }
                    detailDialog?.dismiss()
                })
            }
        }
        if (actions.childCount > 0) content.addView(actions)
    }

    private fun statCard(label: String, value: String): View {
        val card = CardView(this).apply {
            radius = 12f
            cardElevation = 1f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(0), dp(0), dp(8), dp(8))
            }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        content.addView(TextView(this).apply {
            text = value
            setTextColor(0xFF0F172A.toInt())
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = label
            setTextColor(0xFF64748B.toInt())
            textSize = 11f
        })
        card.addView(content)
        return card
    }

    private fun checklistPanel(
        booking: DashboardBooking,
        checklist: List<TechnicianChecklistItem>,
        loading: Boolean
    ): View {
        val checked = checklist.count { it.isChecked == true }
        val percent = if (checklist.isEmpty()) 0 else (checked * 100 / checklist.size)
        val panel = panelContainer()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Pre-Service Checklist"
            setTextColor(0xFF0F172A.toInt())
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "$percent%"
            setTextColor(0xFF0F172A.toInt())
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
        })
        panel.addView(header)

        val progressTrack = View(this).apply {
            background = rounded(0xFFE2E8F0.toInt(), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply {
                topMargin = dp(10)
                bottomMargin = dp(10)
            }
        }
        panel.addView(progressTrack)

        if (loading) {
            panel.addView(ProgressBar(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            })
            return panel
        }

        if (checklist.isEmpty()) {
            panel.addView(TextView(this).apply {
                text = "Checklist is not initialized yet. Start service first."
                setTextColor(0xFF64748B.toInt())
                textSize = 12f
            })
            return panel
        }

        checklist.forEach { item ->
            val row = CheckBox(this).apply {
                text = item.label ?: "Checklist item"
                isChecked = item.isChecked == true
                setTextColor(0xFF334155.toInt())
                textSize = 12f
                buttonTintList = android.content.res.ColorStateList.valueOf(0xFF7C3AED.toInt())
                background = rounded(0xFFFFFFFF.toInt(), dp(8))
                setPadding(dp(8), dp(6), dp(8), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(8)
                }
                setOnClickListener {
                    val bookingId = booking.id ?: return@setOnClickListener
                    val itemId = item.id ?: return@setOnClickListener
                    toggleChecklistItem(bookingId, itemId)
                }
            }
            panel.addView(row)
        }

        return panel
    }

    private fun photoPanel(
        booking: DashboardBooking,
        photos: List<TechnicianBookingPhoto>,
        loading: Boolean
    ): View {
        val beforePhotos = photos.filter { it.type == "BEFORE" }
        val afterPhotos = photos.filter { it.type == "AFTER" }
        val panel = panelContainer()

        panel.addView(TextView(this).apply {
            text = "Photo Documentation"
            setTextColor(0xFF0F172A.toInt())
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
        })

        if (loading) {
            panel.addView(ProgressBar(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(12)
                }
            })
            return panel
        }

        panel.addView(photoUploadRow(booking, "Before Service Photos", "BEFORE", beforePhotos))
        panel.addView(photoUploadRow(booking, "After Service Photos", "AFTER", afterPhotos))
        return panel
    }

    private fun photoUploadRow(
        booking: DashboardBooking,
        label: String,
        type: String,
        photos: List<TechnicianBookingPhoto>
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        val title = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        title.addView(TextView(this).apply {
            text = label
            setTextColor(0xFF334155.toInt())
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        title.addView(TextView(this).apply {
            text = "${photos.size} uploaded"
            setTextColor(0xFF64748B.toInt())
            textSize = 11f
        })
        row.addView(title)

        if (photos.isNotEmpty()) {
            val imageStrip = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, dp(2))
            }
            photos.take(3).forEach { photo ->
                imageStrip.addView(ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = rounded(0xFFE2E8F0.toInt(), dp(8))
                    layoutParams = LinearLayout.LayoutParams(dp(82), dp(82)).apply {
                        marginEnd = dp(8)
                    }
                    load(photo.fileUrl ?: photo.photoUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_service_placeholder)
                        error(R.drawable.ic_service_placeholder)
                        transformations(RoundedCornersTransformation(dp(8).toFloat()))
                    }
                })
            }
            row.addView(imageStrip)
        }

        row.addView(fullWidthButton("Select and Upload", 0xFF7C3AED.toInt()) {
            val bookingId = booking.id ?: return@fullWidthButton
            uploadBookingId = bookingId
            uploadPhotoType = type
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                setType("image/*")
                addCategory(Intent.CATEGORY_OPENABLE)
            }, RC_PICK_PHOTO)
        })
        return row
    }

    private fun toggleChecklistItem(bookingId: String, checklistItemId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.toggleTechnicianChecklistItem(
                    bookingId,
                    checklistItemId,
                    TechnicianBookingActionRequest(technicianId)
                )
                if (response.isSuccessful) {
                    refreshOpenDetail()
                } else {
                    showMessage(response.errorBody()?.string() ?: "Failed to update checklist", true)
                }
            } catch (e: Exception) {
                showMessage("Failed to update checklist: ${e.message}", true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_PHOTO && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            uploadSelectedPhoto(uri)
        }
    }

    private fun uploadSelectedPhoto(uri: Uri) {
        val bookingId = uploadBookingId ?: return
        val type = uploadPhotoType ?: return

        lifecycleScope.launch {
            try {
                val mimeType = contentResolver.getType(uri) ?: "image/*"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Unable to read selected image")
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val part = MultipartBody.Part.createFormData("file", "technician-photo.jpg", requestBody)
                val response = RetrofitClient.instance.uploadTechnicianPhoto(
                    bookingId,
                    part,
                    type.toRequestBody("text/plain".toMediaType()),
                    technicianId.toRequestBody("text/plain".toMediaType())
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@TechnicianDashboardActivity, "$type photo uploaded", Toast.LENGTH_SHORT).show()
                    refreshOpenDetail()
                } else {
                    showMessage(response.errorBody()?.string() ?: "Failed to upload photo", true)
                }
            } catch (e: Exception) {
                showMessage("Failed to upload photo: ${e.message}", true)
            }
        }
    }

    private fun refreshOpenDetail() {
        val booking = detailBooking ?: return
        val content = detailContent ?: return
        loadBookingDetailSupportData(booking, content)
    }

    private fun smallButton(label: String, background: Int, textColor: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(textColor)
            setBackgroundColor(background)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                marginEnd = dp(8)
            }
        }
    }

    private fun fullWidthButton(label: String, background: Int, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(background)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                topMargin = dp(8)
            }
        }
    }

    private fun detailBlock(
        label: String,
        value: String,
        backgroundColor: Int,
        valueColor: Int = 0xFF0F172A.toInt(),
        valueSize: Float = 13f
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(backgroundColor, dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            }
            addView(TextView(this@TechnicianDashboardActivity).apply {
                text = label
                setTextColor(0xFF64748B.toInt())
                textSize = 11f
            })
            addView(TextView(this@TechnicianDashboardActivity).apply {
                text = value
                setTextColor(valueColor)
                textSize = valueSize
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }

    private fun detailMiniBlock(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xFFF8FAFC.toInt(), dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
            addView(TextView(this@TechnicianDashboardActivity).apply {
                text = label
                setTextColor(0xFF64748B.toInt())
                textSize = 11f
            })
            addView(TextView(this@TechnicianDashboardActivity).apply {
                text = value
                setTextColor(0xFF0F172A.toInt())
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }

    private fun panelContainer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(0xFFFFFFFF.toInt(), dp(8))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(16)
            }
        }
    }

    private fun statusChip(label: String, status: String?): TextView {
        return TextView(this).apply {
            text = label
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(statusTextColor(status))
            background = rounded(statusBackgroundColor(status), dp(999))
            setPadding(dp(9), dp(4), dp(9), dp(4))
        }
    }

    private fun emptyState(title: String, body: String): View {
        return TextView(this).apply {
            text = "$title\n$body"
            gravity = Gravity.CENTER
            setTextColor(0xFF64748B.toInt())
            textSize = 13f
            setPadding(dp(24), dp(28), dp(24), dp(28))
            background = rounded(0xFFFFFFFF.toInt(), dp(12))
        }
    }

    private fun formatBookingSummary(booking: DashboardBooking, includeAddress: Boolean): String {
        val lines = mutableListOf<String>()
        lines.add("${booking.serviceType ?: "Service"} - ${booking.deviceType ?: "Device"}")
        lines.add("Schedule: ${booking.bookingDate ?: "TBD"} ${booking.timeSlot ?: ""}".trim())
        lines.add("Amount: PHP ${"%.2f".format(booking.totalAmount ?: 0.0)}")
        booking.clientName?.let { lines.add("Client: $it") }
        booking.clientContact?.let { lines.add("Contact: $it") }
        if (includeAddress) lines.add("Address: ${booking.address ?: "Not available"}")
        booking.landmark?.takeIf { it.isNotBlank() }?.let { lines.add("Landmark: $it") }
        return lines.joinToString("\n")
    }

    private fun showTab(tab: String) {
        currentTab = tab
        overviewSection.visibility = if (tab == "overview") View.VISIBLE else View.GONE
        pendingSection.visibility = if (tab == "pending") View.VISIBLE else View.GONE
        myBookingsSection.visibility = if (tab == "my-bookings") View.VISIBLE else View.GONE
        styleTab(btnOverview, tab == "overview")
        styleTab(btnPending, tab == "pending")
        styleTab(btnMyBookings, tab == "my-bookings")
    }

    private fun styleTab(button: Button, selected: Boolean) {
        button.setTextColor(if (selected) Color.WHITE else 0xFF475569.toInt())
        button.setBackgroundColor(if (selected) 0xFF7C3AED.toInt() else 0xFFE2E8F0.toInt())
    }

    private fun showMessage(message: String, isError: Boolean) {
        if (message.isBlank()) {
            tvMessage.visibility = View.GONE
            return
        }
        tvMessage.text = message
        tvMessage.setTextColor(if (isError) 0xFFDC2626.toInt() else 0xFF059669.toInt())
        tvMessage.setBackgroundResource(if (isError) R.drawable.bg_error_message else R.drawable.bg_success_message)
        tvMessage.visibility = View.VISIBLE
    }

    private fun activeBookings(): List<DashboardBooking> {
        return assignedBookings.filter { it.status in listOf("confirmed", "in_progress") }
    }

    private fun countStatus(status: String): Int {
        return assignedBookings.count { it.status == status }
    }

    private fun primaryActionLabel(booking: DashboardBooking): String? {
        return when (booking.status) {
            "confirmed" -> "Start Service"
            "in_progress" -> "Mark Complete"
            else -> null
        }
    }

    private fun primaryAction(booking: DashboardBooking): (() -> Unit)? {
        val id = booking.id ?: return null
        return when (booking.status) {
            "confirmed" -> { { updateBookingStatus(id, "in_progress") } }
            "in_progress" -> { { updateBookingStatus(id, "completed") } }
            else -> null
        }
    }

    private fun actionColor(label: String): Int {
        return when (label) {
            "Accept Booking" -> 0xFF7C3AED.toInt()
            "Start Service" -> 0xFF2563EB.toInt()
            "Mark Complete" -> 0xFF059669.toInt()
            else -> 0xFF7C3AED.toInt()
        }
    }

    private fun statusLabel(status: String?): String {
        return status?.replace("_", " ")?.split(" ")?.joinToString(" ") {
            it.replaceFirstChar { ch -> ch.uppercase() }
        } ?: "Unknown"
    }

    private fun statusBackgroundColor(status: String?): Int {
        return when (status) {
            "pending" -> 0xFFFFF7ED.toInt()
            "confirmed" -> 0xFFECFDF5.toInt()
            "in_progress" -> 0xFFEFF6FF.toInt()
            "completed" -> 0xFFF5F3FF.toInt()
            "cancelled" -> 0xFFFFF1F2.toInt()
            "no_show" -> 0xFFF1F5F9.toInt()
            else -> 0xFFF1F5F9.toInt()
        }
    }

    private fun statusTextColor(status: String?): Int {
        return when (status) {
            "pending" -> 0xFFC2410C.toInt()
            "confirmed" -> 0xFF047857.toInt()
            "in_progress" -> 0xFF1D4ED8.toInt()
            "completed" -> 0xFF6D28D9.toInt()
            "cancelled" -> 0xFFBE123C.toInt()
            "no_show" -> 0xFF475569.toInt()
            else -> 0xFF475569.toInt()
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun initials(name: String): String {
        return name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "T" }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun logout() {
        SessionManager.clearSession(SessionManager.prefs(this))
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
