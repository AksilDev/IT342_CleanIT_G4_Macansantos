package edu.cit.macansantos.cleanit.features.home

import edu.cit.macansantos.cleanit.features.auth.LoginActivity

import edu.cit.macansantos.cleanit.R

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.title = "Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get user data from intent
        val name = intent.getStringExtra("name") ?: "User"
        val email = intent.getStringExtra("email") ?: "No email"
        val role = intent.getStringExtra("role") ?: "client"
        val contact = intent.getStringExtra("contact") ?: "No contact"
        val verified = intent.getBooleanExtra("verified", false)

        // Display user information
        findViewById<TextView>(R.id.tvProfileName).text = name
        findViewById<TextView>(R.id.tvProfileEmail).text = email
        findViewById<TextView>(R.id.tvProfileContact).text = "📞 $contact"
        findViewById<TextView>(R.id.tvProfileRole).text = role.uppercase()
        
        // Verification status
        val tvVerificationStatus = findViewById<TextView>(R.id.tvVerificationStatus)
        if (verified) {
            tvVerificationStatus.text = "✓ Verified Account"
            tvVerificationStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            tvVerificationStatus.text = "⚠ Unverified Account"
            tvVerificationStatus.setTextColor(0xFFF44336.toInt())
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // Clear any saved session data here if needed
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
