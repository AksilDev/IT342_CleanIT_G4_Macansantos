package edu.cit.macansantos.cleanit.shared.util

import android.content.Context
import android.net.Uri
import edu.cit.macansantos.cleanit.shared.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

object ImageUploadHelper {
    suspend fun uploadAuthImage(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        val requestBody = bytes.toRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("file", "id-image.jpg", requestBody)
        val response = RetrofitClient.instance.uploadImage(part)
        return if (response.isSuccessful) response.body()?.get("imageUrl") else null
    }
}
