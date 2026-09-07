package com.example.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
  private const val BASE_URL = "https://spotifusion.onrender.com/"

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
  }

  private val authInterceptor = Interceptor { chain ->
    val originalRequest = chain.request()
    val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    val currentUser = auth?.currentUser
    val token = try {
      if (currentUser != null) {
        Tasks.await(currentUser.getIdToken(false)).token
      } else {
        null
      }
    } catch (e: Exception) {
      Log.w("ApiClient", "Error acquiring Firebase ID token: ${e.message}")
      null
    }

    val requestBuilder = originalRequest.newBuilder()
    if (!token.isNullOrBlank()) {
      requestBuilder.header("Authorization", "Bearer $token")
    }

    chain.proceed(requestBuilder.build())
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(loggingInterceptor)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()
  }

  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  val service: SpotifusionApi by lazy {
    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(SpotifusionApi::class.java)
  }
}
