package edu.cit.macansantos.cleanit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.cit.macansantos.cleanit.adapter.ServicesAdapter
import edu.cit.macansantos.cleanit.network.RetrofitClient
import kotlinx.coroutines.launch

class ServicesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ServicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        supportActionBar?.title = "Services"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerViewServices)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        loadServices()
    }

    private fun loadServices() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getServices()
                if (response.isSuccessful && response.body() != null) {
                    val services = response.body()!!
                    adapter = ServicesAdapter(services) { service ->
                        // Navigate to booking creation
                        startActivity(Intent(this@ServicesActivity, CreateBookingActivity::class.java).apply {
                            putExtra("serviceId", service.id)
                            putExtra("userId", intent.getStringExtra("userId") ?: "")
                        })
                    }
                    recyclerView.adapter = adapter
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
