package edu.cit.macansantos.cleanit.features.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOKEN = "reset_token"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        supportActionBar?.title = "Reset Password"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etToken = findViewById<EditText>(R.id.etToken)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        etToken.setText(intent.getStringExtra(EXTRA_TOKEN).orEmpty())

        btnReset.setOnClickListener {
            val token = etToken.text.toString().trim()
            val newPassword = etNewPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            if (token.isEmpty()) {
                Toast.makeText(this, "Reset token is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnReset.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.resetPassword(
                        mapOf("token" to token, "newPassword" to newPassword)
                    )
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Password reset successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            response.errorBody()?.string() ?: "Reset failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ResetPasswordActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progressBar.visibility = View.GONE
                    btnReset.isEnabled = true
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
