package edu.cit.macansantos.cleanit.features.auth

import edu.cit.macansantos.cleanit.shared.navigation.RoleNavigator

import edu.cit.macansantos.cleanit.R

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
import edu.cit.macansantos.cleanit.features.auth.LoginRequest
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import edu.cit.macansantos.cleanit.shared.session.SessionManager
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
        tryRestoreSession()
    }

    private fun tryRestoreSession() {
        if (!SessionManager.hasValidSession(sharedPreferences)) return

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val email = SessionManager.getEmail(sharedPreferences)
                val response = RetrofitClient.instance.getUserProfile(email)
                if (response.isSuccessful && response.body() != null) {
                    SessionManager.saveUserProfile(sharedPreferences, response.body()!!)
                    SessionManager.navigateFromSession(this@LoginActivity, sharedPreferences)
                    return@launch
                }
                SessionManager.clearSession(sharedPreferences)
            } catch (_: Exception) {
                SessionManager.clearSession(sharedPreferences)
            } finally {
                progressBar.visibility = View.GONE
                btnLogin.isEnabled = true
            }
        }
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
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                showError(tvError, "Enter your email first")
                return@setOnClickListener
            }
            requestPasswordReset(email, tvError)
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
                    
                    RoleNavigator.saveSession(sharedPreferences, user)
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
            val email = account?.email

            if (email != null) {
                val name = account.displayName ?: email.substringBefore("@")
                authenticateWithBackend(email, name, progressBarGoogle, btnGoogleSignIn, tvError)
            } else {
                showError(tvError, "Failed to get Google email")
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
        email: String,
        name: String,
        progressBar: ProgressBar,
        btnGoogle: Button,
        tvError: TextView
    ) {
        lifecycleScope.launch {
            try {
                val checkResponse = RetrofitClient.instance.oauthCheck(mapOf("email" to email))
                if (!checkResponse.isSuccessful || checkResponse.body() == null) {
                    showError(tvError, "Google authentication failed. Please try again.")
                    return@launch
                }

                val check = checkResponse.body()!!
                if (check.exists == true && !check.token.isNullOrBlank()) {
                    SessionManager.saveToken(sharedPreferences, check.token!!)
                    val profileResponse = RetrofitClient.instance.getUserProfile(email)
                    if (profileResponse.isSuccessful && profileResponse.body() != null) {
                        val profile = profileResponse.body()!!
                        SessionManager.saveUserProfile(sharedPreferences, profile)
                        navigateToHome(
                            profile.name,
                            profile.email,
                            profile.role,
                            profile.id,
                            profile.contactNo,
                            profile.verified
                        )
                    } else {
                        navigateToHome(name, email, check.role, null, null, false)
                    }
                } else {
                    startActivity(Intent(this@LoginActivity, OAuthCompleteActivity::class.java).apply {
                        putExtra(OAuthCompleteActivity.EXTRA_EMAIL, email)
                        putExtra(OAuthCompleteActivity.EXTRA_NAME, name)
                    })
                    finish()
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
        RoleNavigator.navigate(this, name, email, role, userId, contactNo, verified)
    }

    private fun requestPasswordReset(email: String, tvError: TextView) {
        tvError.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.forgotPassword(mapOf("email" to email))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body.resetToken
                    if (!token.isNullOrBlank()) {
                        startActivity(Intent(this@LoginActivity, ResetPasswordActivity::class.java).apply {
                            putExtra(ResetPasswordActivity.EXTRA_TOKEN, token)
                        })
                    } else {
                        Toast.makeText(this@LoginActivity, body.message ?: "Request sent", Toast.LENGTH_LONG).show()
                    }
                } else {
                    showError(tvError, "Failed to request password reset")
                }
            } catch (e: Exception) {
                showError(tvError, "Connection error: ${e.message}")
            }
        }
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
