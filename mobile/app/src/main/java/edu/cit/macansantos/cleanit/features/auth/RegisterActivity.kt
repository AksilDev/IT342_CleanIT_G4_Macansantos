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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import edu.cit.macansantos.cleanit.shared.session.SessionManager
import edu.cit.macansantos.cleanit.shared.util.ImageUploadHelper
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_PICK_IMAGE = 5002
        private const val RC_GOOGLE_REGISTER = 9002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions())

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etContact = findViewById<EditText>(R.id.etContact)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnGoogleRegister = findViewById<Button>(R.id.btnGoogleRegister)
        val tvGoLogin = findViewById<TextView>(R.id.tvGoLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
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
            // Email format validation
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!emailRegex.matches(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Password strength validation — must match backend requirements
            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!password.any { it.isUpperCase() }) {
                Toast.makeText(this, "Password must contain at least one uppercase letter (A-Z)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!password.any { it.isDigit() }) {
                Toast.makeText(this, "Password must contain at least one number (0-9)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUri == null) {
                Toast.makeText(this, "ID image is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Image size validation — max 5MB
            val imageSize = try {
                contentResolver.openInputStream(selectedImageUri!!)?.use { it.available().toLong() } ?: 0L
            } catch (_: Exception) { 0L }
            if (imageSize > 5 * 1024 * 1024) {
                Toast.makeText(this, "Image size must be less than 5MB", Toast.LENGTH_SHORT).show()
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

        btnGoogleRegister.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            btnGoogleRegister.isEnabled = false
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_REGISTER)
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
        } else if (requestCode == RC_GOOGLE_REGISTER) {
            handleGoogleRegisterResult(GoogleSignIn.getSignedInAccountFromIntent(data))
        }
    }

    private fun handleGoogleRegisterResult(completedTask: Task<GoogleSignInAccount>) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnGoogleRegister = findViewById<Button>(R.id.btnGoogleRegister)

        try {
            val account = completedTask.getResult(ApiException::class.java)
            val email = account?.email
            val name = account?.displayName ?: email?.substringBefore("@")

            if (email.isNullOrBlank()) {
                Toast.makeText(this, "Failed to get Google account info", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                btnGoogleRegister.isEnabled = true
                return
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.oauthCheck(mapOf("email" to email))
                    val check = response.body()
                    if (response.isSuccessful && check?.exists == true && !check.token.isNullOrBlank()) {
                        Toast.makeText(this@RegisterActivity, "This Google account is already registered. Signing you in...", Toast.LENGTH_SHORT).show()
                        signInExistingGoogleUser(email, name, check.role, check.token)
                    } else {
                        startActivity(Intent(this@RegisterActivity, OAuthCompleteActivity::class.java).apply {
                            putExtra(OAuthCompleteActivity.EXTRA_EMAIL, email)
                            putExtra(OAuthCompleteActivity.EXTRA_NAME, name ?: email.substringBefore("@"))
                        })
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterActivity, "Google registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progressBar.visibility = View.GONE
                    btnGoogleRegister.isEnabled = true
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, googleSignInErrorMessage(e), Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            btnGoogleRegister.isEnabled = true
        }
    }

    private fun googleSignInOptions(): GoogleSignInOptions {
        val googleClientId = getString(R.string.default_web_client_id)
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (googleClientId.isNotBlank() && googleClientId != "YOUR_GOOGLE_WEB_CLIENT_ID_HERE") {
            builder.requestIdToken(googleClientId)
        }

        return builder.build()
    }

    private fun googleSignInErrorMessage(error: ApiException): String {
        return if (error.statusCode == GoogleSignInStatusCodes.DEVELOPER_ERROR) {
            "Google Sign-In is not configured for this app. Add an Android OAuth client for package edu.cit.macansantos.cleanit using this debug SHA-1: EE:5C:DE:4D:3D:AC:EE:6D:41:A8:AB:46:79:96:49:77:13:36:D7:83, then set the Web Client ID in strings.xml."
        } else {
            "Google sign-in failed: ${GoogleSignInStatusCodes.getStatusCodeString(error.statusCode)}"
        }
    }

    private suspend fun signInExistingGoogleUser(
        email: String,
        name: String?,
        role: String?,
        token: String?
    ) {
        val prefs = SessionManager.prefs(this)
        SessionManager.saveToken(prefs, token.orEmpty())
        val profileResponse = RetrofitClient.instance.getUserProfile(email)
        if (profileResponse.isSuccessful && profileResponse.body() != null) {
            val profile = profileResponse.body()!!
            SessionManager.saveUserProfile(prefs, profile)
            RoleNavigator.navigate(this, profile.name, profile.email, profile.role, profile.id, profile.contactNo, profile.verified)
        } else {
            RoleNavigator.navigate(this, name, email, role, null, null, false)
        }
    }
}
