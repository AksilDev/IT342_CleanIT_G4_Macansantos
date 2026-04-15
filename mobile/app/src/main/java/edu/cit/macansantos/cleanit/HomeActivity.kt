package edu.cit.macansantos.cleanit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val name = intent.getStringExtra("name") ?: "User"
        val role = intent.getStringExtra("role") ?: "client"

        findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $name!"
        findViewById<TextView>(R.id.tvRole).text = "Role: $role"
    }
}