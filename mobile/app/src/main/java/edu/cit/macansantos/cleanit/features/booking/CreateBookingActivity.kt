package edu.cit.macansantos.cleanit.features.booking

import edu.cit.macansantos.cleanit.R

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.features.catalog.AddOnsAdapter
import edu.cit.macansantos.cleanit.features.catalog.TechniciansAdapter
import edu.cit.macansantos.cleanit.features.catalog.AddOn
import edu.cit.macansantos.cleanit.features.booking.CreateBookingRequest
import edu.cit.macansantos.cleanit.features.catalog.Service
import edu.cit.macansantos.cleanit.features.catalog.Technician
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateBookingActivity : AppCompatActivity() {

    private lateinit var tvServiceName: TextView
    private lateinit var tvServicePrice: TextView
    private lateinit var tvServiceDuration: TextView
    private lateinit var rgDeviceType: RadioGroup
    private lateinit var rbPC: RadioButton
    private lateinit var rbLaptop: RadioButton
    private lateinit var rvAddOns: RecyclerView
    private lateinit var tvNoAddOns: TextView
    private lateinit var progressAddOns: ProgressBar
    private lateinit var rvTechnicians: RecyclerView
    private lateinit var tvNoTechnicians: TextView
    private lateinit var progressTechnicians: ProgressBar
    private lateinit var rgTimeSlot: RadioGroup
    private lateinit var etBookingDate: EditText
    private lateinit var etAddress: EditText
    private lateinit var etLandmark: EditText
    private lateinit var etSpecialInstructions: EditText
    private lateinit var tvBasePrice: TextView
    private lateinit var tvAddOnsTotal: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnSubmitBooking: Button
    private lateinit var progressBar: ProgressBar

    private var service: Service? = null
    private var userId: String = ""
    private var selectedAddOns = mutableListOf<AddOn>()
    private var selectedTechnicianId: String? = null
    private var bookingDate: String = ""

    private val timeSlots = listOf(
        "9:00 AM - 11:00 AM",
        "11:00 AM - 1:00 PM",
        "1:00 PM - 3:00 PM",
        "3:00 PM - 5:00 PM"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_booking)

        supportActionBar?.title = "Book Service"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        userId = intent.getStringExtra("userId") ?: ""
        val serviceId = intent.getStringExtra("serviceId") ?: ""

        if (userId.isEmpty() || serviceId.isEmpty()) {
            Toast.makeText(this, "Invalid booking data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        loadServiceDetails(serviceId)
        loadAddOns(serviceId)
        loadTechnicians()
        setupDatePicker()
        setupTimeSlots()
    }

    private fun initializeViews() {
        tvServiceName = findViewById(R.id.tvServiceName)
        tvServicePrice = findViewById(R.id.tvServicePrice)
        tvServiceDuration = findViewById(R.id.tvServiceDuration)
        rgDeviceType = findViewById(R.id.rgDeviceType)
        rbPC = findViewById(R.id.rbPC)
        rbLaptop = findViewById(R.id.rbLaptop)
        rvAddOns = findViewById(R.id.rvAddOns)
        tvNoAddOns = findViewById(R.id.tvNoAddOns)
        progressAddOns = findViewById(R.id.progressAddOns)
        rvTechnicians = findViewById(R.id.rvTechnicians)
        tvNoTechnicians = findViewById(R.id.tvNoTechnicians)
        progressTechnicians = findViewById(R.id.progressTechnicians)
        rgTimeSlot = findViewById(R.id.rgTimeSlot)
        etBookingDate = findViewById(R.id.etBookingDate)
        etAddress = findViewById(R.id.etAddress)
        etLandmark = findViewById(R.id.etLandmark)
        etSpecialInstructions = findViewById(R.id.etSpecialInstructions)
        tvBasePrice = findViewById(R.id.tvBasePrice)
        tvAddOnsTotal = findViewById(R.id.tvAddOnsTotal)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking)
        progressBar = findViewById(R.id.progressBar)

        rvAddOns.layoutManager = LinearLayoutManager(this)
        rvTechnicians.layoutManager = LinearLayoutManager(this)

        btnSubmitBooking.setOnClickListener { submitBooking() }
    }

    private fun loadServiceDetails(serviceId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getServiceById(serviceId)
                if (response.isSuccessful && response.body() != null) {
                    service = response.body()!!
                    displayServiceDetails()
                } else {
                    val message = response.errorBody()?.string()?.ifBlank { null } ?: "Failed to load service"
                    Toast.makeText(this@CreateBookingActivity, 
                        message, Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateBookingActivity, 
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayServiceDetails() {
        service?.let {
            tvServiceName.text = it.name
            tvServicePrice.text = "₱${String.format("%.2f", it.basePrice)}"
            val hours = it.durationMinutes / 60.0
            tvServiceDuration.text = "${String.format("%.1f", hours)} hours"
            tvBasePrice.text = "₱${String.format("%.2f", it.basePrice)}"
            calculateTotal()
        }
    }

    private fun loadAddOns(serviceId: String) {
        progressAddOns.visibility = View.VISIBLE
        tvNoAddOns.visibility = View.GONE
        rvAddOns.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getServiceAddOns(serviceId)
                if (response.isSuccessful && response.body() != null) {
                    val addOns = response.body()!!
                    if (addOns.isEmpty()) {
                        tvNoAddOns.visibility = View.VISIBLE
                    } else {
                        val adapter = AddOnsAdapter(addOns) { addOn, isSelected ->
                            if (isSelected) {
                                selectedAddOns.add(addOn)
                            } else {
                                selectedAddOns.remove(addOn)
                            }
                            calculateTotal()
                        }
                        rvAddOns.adapter = adapter
                        rvAddOns.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                tvNoAddOns.visibility = View.VISIBLE
                tvNoAddOns.text = "Failed to load add-ons"
            } finally {
                progressAddOns.visibility = View.GONE
            }
        }
    }

    private fun loadTechnicians() {
        progressTechnicians.visibility = View.VISIBLE
        tvNoTechnicians.visibility = View.GONE
        rvTechnicians.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getVerifiedTechnicians()
                if (response.isSuccessful && response.body() != null) {
                    val technicians = response.body()!!
                    if (technicians.isEmpty()) {
                        tvNoTechnicians.visibility = View.VISIBLE
                    } else {
                        val adapter = TechniciansAdapter(technicians) { technician ->
                            selectedTechnicianId = technician.id
                        }
                        rvTechnicians.adapter = adapter
                        rvTechnicians.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                tvNoTechnicians.visibility = View.VISIBLE
                tvNoTechnicians.text = "Failed to load technicians"
            } finally {
                progressTechnicians.visibility = View.GONE
            }
        }
    }

    private fun setupDatePicker() {
        etBookingDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                bookingDate = dateFormat.format(selectedCalendar.time)
                
                val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                etBookingDate.setText(displayFormat.format(selectedCalendar.time))
            }, year, month, day).apply {
                datePicker.minDate = calendar.timeInMillis
                show()
            }
        }
    }

    private fun setupTimeSlots() {
        timeSlots.forEachIndexed { index, slot ->
            val radioButton = RadioButton(this)
            radioButton.id = View.generateViewId()
            radioButton.text = slot
            radioButton.setTextColor(resources.getColor(android.R.color.white, null))
            rgTimeSlot.addView(radioButton)
        }
    }

    private fun calculateTotal() {
        val basePrice = service?.basePrice ?: 0.0
        val addOnsTotal = selectedAddOns.sumOf { it.price }
        val total = basePrice + addOnsTotal

        tvAddOnsTotal.text = "₱${String.format("%.2f", addOnsTotal)}"
        tvTotalAmount.text = "₱${String.format("%.2f", total)}"
    }

    private fun submitBooking() {
        // Validation
        val deviceType = when (rgDeviceType.checkedRadioButtonId) {
            R.id.rbPC -> "pc"
            R.id.rbLaptop -> "laptop"
            else -> null
        }

        if (deviceType == null) {
            Toast.makeText(this, "Please select device type", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedTechnicianId == null) {
            Toast.makeText(this, "Please select a technician", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTimeSlotId = rgTimeSlot.checkedRadioButtonId
        if (selectedTimeSlotId == -1) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show()
            return
        }

        val timeSlot = findViewById<RadioButton>(selectedTimeSlotId).text.toString()

        if (bookingDate.isEmpty()) {
            Toast.makeText(this, "Please select a booking date", Toast.LENGTH_SHORT).show()
            return
        }

        val address = etAddress.text.toString().trim()
        if (address.isEmpty()) {
            Toast.makeText(this, "Please enter your address", Toast.LENGTH_SHORT).show()
            return
        }

        // Create booking request
        progressBar.visibility = View.VISIBLE
        btnSubmitBooking.isEnabled = false

        lifecycleScope.launch {
            try {
                val bookingData = CreateBookingRequest(
                    clientId = userId,
                    technicianId = selectedTechnicianId!!,
                    serviceId = service!!.id,
                    serviceType = service!!.name,
                    deviceType = deviceType,
                    addOns = selectedAddOns.map { it.id },
                    timeSlot = timeSlot,
                    bookingDate = bookingDate,
                    address = address,
                    landmark = etLandmark.text.toString().trim(),
                    specialInstructions = etSpecialInstructions.text.toString().trim(),
                    totalAmount = service!!.basePrice + selectedAddOns.sumOf { it.price }
                )

                val response = RetrofitClient.instance.createBooking(bookingData)
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateBookingActivity, 
                        "Booking created successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val message = response.errorBody()?.string()?.ifBlank { null } ?: "Failed to create booking"
                    Toast.makeText(this@CreateBookingActivity, 
                        message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateBookingActivity, 
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
                btnSubmitBooking.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
