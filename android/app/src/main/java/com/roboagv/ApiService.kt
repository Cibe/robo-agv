package com.roboagv

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ── Data models ──────────────────────────────────────────────────────────────

data class RecordRoomRequest(val images: List<String>)

data class RecordRoomResponse(
    val status: String,
    val descriptions: List<String>,
    val memory_count: Int
)

data class NavigateRequest(
    val voice_command: String,
    val current_frame: String
)

data class NavigateResponse(
    val direction: String,
    val speech_text: String,
    val reasoning: String
)

// ── Retrofit interface ───────────────────────────────────────────────────────

interface RoboApi {
    @POST("record-room")
    suspend fun recordRoom(@Body request: RecordRoomRequest): RecordRoomResponse

    @POST("navigate")
    suspend fun navigate(@Body request: NavigateRequest): NavigateResponse

    @DELETE("memory")
    suspend fun clearMemory(): Map<String, String>

    @GET("health")
    suspend fun health(): Map<String, String>
}

// ── Singleton client ─────────────────────────────────────────────────────────

object ApiClient {
    var baseUrl: String = "http://192.168.1.100:8000/"
        private set

    private var retrofit: Retrofit? = null

    fun setBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        if (normalized != baseUrl) {
            baseUrl = normalized
            retrofit = null  // Invalidate cached instance
        }
    }

    fun getApi(): RoboApi {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)   // VLM calls can be slow
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(RoboApi::class.java)
    }
}
