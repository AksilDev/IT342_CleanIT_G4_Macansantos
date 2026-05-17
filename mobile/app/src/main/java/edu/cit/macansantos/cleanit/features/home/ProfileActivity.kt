package edu.cit.macansantos.cleanit.features.home

import edu.cit.macansantos.cleanit.features.auth.LoginActivity

import edu.cit.macansantos.cleanit.R

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import edu.cit.macansantos.cleanit.shared.session.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileContact: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvVerificationStatus: TextView

    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressBar = findViewById(R.id.progressBar)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfileContact = findViewById(R.id.tvProfileContact)
        tvProfileRole = findViewById(R.id.tvProfileRole)
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus)

        displayProfile(
            name = intent.getStringExtra("name") ?: "User",
            email = intent.getStringExtra("email") ?: "No email",
            contact = intent.getStringExtra("contact") ?: "No contact",
            role = intent.getStringExtra("role") ?: "client",
            verified = intent.getBooleanExtra("verified", false)
        )

        userEmail = intent.getStringExtra("email").orEmpty()
        if (userEmail.isNotBlank()) {
            refreshProfile()
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            SessionManager.clearSession(SessionManager.prefs(this))
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun refreshProfile() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile(userEmail)
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    SessionManager.saveUserProfile(SessionManager.prefs(this@ProfileActivity), profile)
                    displayProfile(
                        name = profile.name ?: "User",
                        email = profile.email ?: userEmail,
                        contact = profile.contactNo ?: "No contact",
                        role = profile.role ?: "client",
                        verified = profile.verified == true
                    )
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayProfile(
        name: String,
        email: String,
        contact: String,
        role: String,
        verified: Boolean
    ) {
        tvProfileName.text = name
        tvProfileEmail.text = email
        tvProfileContact.text = "📞 $contact"
        tvProfileRole.text = role.uppercase()

        if (verified) {
            tvVerificationStatus.text = "✓ Verified Account"
            tvVerificationStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            tvVerificationStatus.text = "⚠ Unverified Account"
            tvVerificationStatus.setTextColor(0xFFF44336.toInt())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
