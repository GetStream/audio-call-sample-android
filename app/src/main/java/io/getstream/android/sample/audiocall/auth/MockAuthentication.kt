package io.getstream.android.sample.audiocall.auth

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/api/auth/create-token")
    suspend fun createToken(@Query("user_id") userId: String): UserResponse
}

object Client {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.102:3000") // replace with your server's IP address and port
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService: ApiService = retrofit.create(ApiService::class.java)

    suspend fun getToken(userId: String): UserResponse {
        return apiService.createToken(userId)
    }

}

data class UserResponse(
    val userId: String,
    val apiKey: String,
    val token: String
)
