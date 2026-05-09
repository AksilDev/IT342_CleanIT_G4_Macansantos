package edu.cit.macansantos.cleanit

import android.app.Application
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient

class CleanITApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RetrofitClient with application context
        RetrofitClient.init(this)
    }
}
