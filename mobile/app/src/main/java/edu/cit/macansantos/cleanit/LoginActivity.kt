package edu.cit.macansantos.cleanit

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import edu.cit.macansantos.cleanit.model.LoginRequest
import edu.cit.macansantos.cleanit.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var sharedPreferences: SharedPreferences
    private var isPasswordVisible = false
    
    companion object {
        private const val RC_SIGN_IN = 9001
        private const val PREFS_NAME = "CleanITPrefs"
        private const val KEY_REMEMBER_EMAIL = "remember_email"
        private const val KEY_REMEMBER_PASSWORD = "remember_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupViews()
        loadRememberedCredentials()
    }

    private fun setupViews() {
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val progressBarGoogle = findViewById<ProgressBar>(R.id.progressBarGoogle)
        val tvError = findViewById<TextView>(R.id.tvError)

        // Toggle password visibility
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Email/Password Login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showError(tvError, "Please fill in all fields")
                return@setOnClickListener
            }

            // Save credentials if remember me is checked
            if (cbRememberMe.isChecked) {
                saveCredentials(email, password)
            } else {
                clearCredentials()
            }

            performEmailLogin(email, password, progressBar, btnLogin, tvError)
        }

        // Google Sign-In
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle(progressBarGoogle, btnGoogleSignIn, tvError)
        }

        // Forgot Password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Go to Register
        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performEmailLogin(
        email: String,
        password: String,
        progressBar: ProgressBar,
        btnLogin: Button,
        tvError: TextView
    ) {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    navigateToHome(user.name, user.email, user.role, user.id, user.contactNo, user.verified)
                } else {
                    showError(tvError, "Invalid email or password")
                }
            } catch (e: Exception) {
                showError(tvError, "Connection error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
            }
        }
    }

    private fun signInWithGoogle(
        progressBar: ProgressBar,
        btnGoogle: Button,
        tvError: TextView
    ) {
        progressBar.visibility = View.VISIBLE
        btnGoogle.isEnabled = false
        tvError.visibility = View.GONE

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        val progressBarGoogle = findViewById<ProgressBar>(R.id.progressBarGoogle)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvError = findViewById<TextView>(R.id.tvError)

        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                // Send ID token to backend for verification
                authenticateWithBackend(idToken, progressBarGoogle, btnGoogleSignIn, tvError)
            } else {
                showError(tvError, "Failed to get Google ID token")
                progressBarGoogle.visibility = View.GONE
                btnGoogleSignIn.isEnabled = true
            }
        } catch (e: ApiException) {
            showError(tvError, "Google sign-in failed: ${e.message}")
            progressBarGoogle.visibility = View.GONE
            btnGoogleSignIn.isEnabled = true
        }
    }

    private fun authenticateWithBackend(
        idToken: String,
        progressBar: ProgressBar,
        btnGoogle: Button,
        tvError: TextView
    ) {
        lifecycleScope.launch {
            try {
                // Call backend OAuth endpoint
                val response = RetrofitClient.instance.googleAuth(mapOf("idToken" to idToken))
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    navigateToHome(user.name, user.email, user.role, user.id, user.contactNo, user.verified)
                } else {
                    showError(tvError, "Google authentication failed")
                }
            } catch (e: Exception) {
                showError(tvError, "Connection error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnGoogle.isEnabled = true
            }
        }
    }

    private fun navigateToHome(
        name: String?,
        email: String?,
        role: String?,
        userId: String?,
        contactNo: String?,
        verified: Boolean?
    ) {
        Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, HomeActivity::class.java).apply {
            putExtra("name", name)
            putExtra("email", email)
            putExtra("role", role)
            putExtra("userId", userId)
            putExtra("contact", contactNo ?: "")
            putExtra("verified", verified ?: false)
        })
        finish()
    }

    private fun showError(tvError: TextView, message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit().apply {
            putString(KEY_REMEMBER_EMAIL, email)
            putString(KEY_REMEMBER_PASSWORD, password)
            apply()
        }
    }

    private fun clearCredentials() {
        sharedPreferences.edit().apply {
            remove(KEY_REMEMBER_EMAIL)
            remove(KEY_REMEMBER_PASSWORD)
            apply()
        }
    }

    private fun loadRememberedCredentials() {
        val email = sharedPreferences.getString(KEY_REMEMBER_EMAIL, "")
        val password = sharedPreferences.getString(KEY_REMEMBER_PASSWORD, "")

        if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
            findViewById<EditText>(R.id.etEmail).setText(email)
            findViewById<EditText>(R.id.etPassword).setText(password)
            findViewById<CheckBox>(R.id.cbRememberMe).isChecked = true
        }
    }
}
