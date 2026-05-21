package edu.cit.macansantos.cleanit.features.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.macansantos.cleanit.R
import edu.cit.macansantos.cleanit.shared.navigation.RoleNavigator
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import edu.cit.macansantos.cleanit.shared.session.SessionManager
import edu.cit.macansantos.cleanit.shared.util.ImageUploadHelper
import kotlinx.coroutines.launch

class OAuthCompleteActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private var oauthEmail: String = ""
    private var oauthName: String = ""

    companion object {
        private const val RC_PICK_IMAGE = 5001
        const val EXTRA_EMAIL = "oauth_email"
        const val EXTRA_NAME = "oauth_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oauth_complete)

        oauthEmail = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        oauthName = intent.getStringExtra(EXTRA_NAME).orEmpty()

        findViewById<TextView>(R.id.tvOAuthEmail).text = oauthEmail
        findViewById<TextView>(R.id.tvOAuthName).text = oauthName

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, RC_PICK_IMAGE)
        }

        findViewById<Button>(R.id.btnComplete).setOnClickListener { completeRegistration() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_IMAGE && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            findViewById<TextView>(R.id.tvImageStatus).text =
                if (selectedImageUri != null) "Image selected" else "No image selected"
        }
    }

    private fun completeRegistration() {
        val contact = findViewById<EditText>(R.id.etContact).text.toString().trim()
        val role = if (findViewById<RadioGroup>(R.id.rgRole).checkedRadioButtonId == R.id.rbClient) {
            "client"
        } else {
            "technician"
        }
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnComplete = findViewById<Button>(R.id.btnComplete)

        if (contact.isEmpty()) {
            Toast.makeText(this, "Contact number is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "ID image is required", Toast.LENGTH_SHORT).show()
            return
        }
        // Image size validation — max 5MB
        val imageSize = try {
            contentResolver.openInputStream(selectedImageUri!!)?.use { it.available().toLong() } ?: 0L
        } catch (_: Exception) { 0L }
        if (imageSize > 5 * 1024 * 1024) {
            Toast.makeText(this, "Image size must be less than 5MB", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnComplete.isEnabled = false

        lifecycleScope.launch {
            try {
                val imageUrl = ImageUploadHelper.uploadAuthImage(this@OAuthCompleteActivity, selectedImageUri!!)
                if (imageUrl.isNullOrBlank()) {
                    Toast.makeText(this@OAuthCompleteActivity, "Failed to upload ID image", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val response = RetrofitClient.instance.oauthComplete(
                    OAuthCompleteRequest(
                        email = oauthEmail,
                        name = oauthName,
                        role = role,
                        contactNo = contact,
                        imageUrl = imageUrl
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    val prefs = SessionManager.prefs(this@OAuthCompleteActivity)
                    RoleNavigator.saveSession(prefs, user)
                    RoleNavigator.navigate(
                        this@OAuthCompleteActivity,
                        user.name,
                        user.email,
                        user.role,
                        user.id,
                        user.contactNo,
                        user.verified
                    )
                } else {
                    Toast.makeText(
                        this@OAuthCompleteActivity,
                        "Registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@OAuthCompleteActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                btnComplete.isEnabled = true
            }
        }
    }
}
