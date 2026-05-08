package edu.cit.macansantos.cleanit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.adapter.ServicesAdapter
import edu.cit.macansantos.cleanit.model.Service
import edu.cit.macansantos.cleanit.network.RetrofitClient
import kotlinx.coroutines.launch

class ServicesActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ServicesAdapter
    
    private var allServices: List<Service> = emptyList()
    private var filteredServices: List<Service> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        supportActionBar?.title = "Services"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        searchView = findViewById(R.id.searchView)
        recyclerView = findViewById(R.id.recyclerViewServices)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupSearch()
        loadServices()
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterServices(newText ?: "")
                return true
            }
        })
    }

    private fun filterServices(query: String) {
        filteredServices = if (query.isEmpty()) {
            allServices
        } else {
            allServices.filter { service ->
                service.name.contains(query, ignoreCase = true) ||
                service.description.contains(query, ignoreCase = true)
            }
        }
        updateAdapter()
    }

    private fun updateAdapter() {
        adapter = ServicesAdapter(filteredServices) { service ->
            // Navigate to booking creation
            startActivity(Intent(this@ServicesActivity, CreateBookingActivity::class.java).apply {
                putExtra("serviceId", service.id)
                putExtra("userId", intent.getStringExtra("userId") ?: "")
            })
        }
        recyclerView.adapter = adapter
    }

    private fun loadServices() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getServices()
                if (response.isSuccessful && response.body() != null) {
                    allServices = response.body()!!
                    filteredServices = allServices
                    updateAdapter()
                } else {
                    Toast.makeText(this@ServicesActivity,
                        "Failed to load services", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ServicesActivity,
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