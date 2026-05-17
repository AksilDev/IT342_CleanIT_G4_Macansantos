package edu.cit.macansantos.cleanit.features.auth

import edu.cit.macansantos.cleanit.shared.navigation.RoleNavigator

import edu.cit.macansantos.cleanit.R

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import edu.cit.macansantos.cleanit.shared.util.ImageUploadHelper
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null

    companion object {
        private const val RC_PICK_IMAGE = 5002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etContact = findViewById<EditText>(R.id.etContact)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvImageStatus = findViewById<TextView>(R.id.tvImageStatus)

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, RC_PICK_IMAGE)
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val contact = etContact.text.toString().trim()
            val role = if (rgRole.checkedRadioButtonId == R.id.rbClient) "client" else "technician"

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || contact.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUri == null) {
                Toast.makeText(this, "ID image is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            lifecycleScope.launch {
                try {
                    val imageUrl = ImageUploadHelper.uploadAuthImage(this@RegisterActivity, selectedImageUri!!)
                    if (imageUrl.isNullOrBlank()) {
                        Toast.makeText(this@RegisterActivity, "Failed to upload ID image", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val response = RetrofitClient.instance.register(
                        RegisterRequest(name, email, password, role, contact, imageUrl)
                    )
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Account created! Logging you in...",
                            Toast.LENGTH_SHORT
                        ).show()

                        val loginResponse = RetrofitClient.instance.login(LoginRequest(email, password))
                        if (loginResponse.isSuccessful && loginResponse.body() != null) {
                            val user = loginResponse.body()!!
                            val prefs = getSharedPreferences("CleanITPrefs", MODE_PRIVATE)
                            RoleNavigator.saveSession(prefs, user)
                            RoleNavigator.navigate(
                                this@RegisterActivity,
                                user.name,
                                user.email,
                                user.role,
                                user.id,
                                user.contactNo,
                                user.verified
                            )
                        } else {
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        }
                    } else {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registration failed: ${response.errorBody()?.string()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_IMAGE && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            findViewById<TextView>(R.id.tvImageStatus).text =
                if (selectedImageUri != null) "Image selected" else "No image selected"
        }
    }
}
