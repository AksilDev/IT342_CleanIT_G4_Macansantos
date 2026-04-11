package edu.cit.macansantos.cleanit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.macansantos.cleanit.model.RegisterRequest
import edu.cit.macansantos.cleanit.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName      = findViewById<EditText>(R.id.etName)
        val etEmail     = findViewById<EditText>(R.id.etEmail)
        val etPassword  = findViewById<EditText>(R.id.etPassword)
        val etContact   = findViewById<EditText>(R.id.etContact)
        val rgRole      = findViewById<RadioGroup>(R.id.rgRole)
        val rbClient    = findViewById<RadioButton>(R.id.rbClient)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoLogin   = findViewById<TextView>(R.id.tvGoLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnRegister.setOnClickListener {
            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val contact  = etContact.text.toString().trim()
            val role     = if (rgRole.checkedRadioButtonId == R.id.rbClient) "client" else "technician"

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || contact.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.register(
                        RegisterRequest(name, email, password, role, contact)
                    )
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity,
                            "Account created! Logging you in...", Toast.LENGTH_SHORT).show()

                        // Auto-login after register
                        val loginResponse = RetrofitClient.instance.login(
                            edu.cit.macansantos.cleanit.model.LoginRequest(email, password)
                        )
                        if (loginResponse.isSuccessful && loginResponse.body() != null) {
                            val user = loginResponse.body()!!
                            startActivity(Intent(this@RegisterActivity, HomeActivity::class.java)
                                .putExtra("name", user.name)
                                .putExtra("role", user.role))
                            finish()
                        } else {
                            // fallback to login screen
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity,
                            "Registration failed: ${response.errorBody()?.string()}",
                            Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity,
                        "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                }
            }
        }

        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}