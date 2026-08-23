package com.iptv.player.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.iptv.player.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    var baseUrl: HttpUrl? = null
        private set

    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val base = baseUrl
                if (base != null) {
                    // Rewrite request to the current server, preserving any reverse-proxy path prefix.
                    val req = chain.request()
                    val newUrl = base.newBuilder()
                        .scheme(base.scheme)
                        .host(base.host)
                        .port(base.port)
                        .encodedPath(joinPath(base.encodedPath, req.url.encodedPath))
                        .encodedQuery(req.url.encodedQuery)
                        .build()
                    return@addInterceptor chain.proceed(req.newBuilder().url(newUrl).build())
                }
                chain.proceed(chain.request())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val service: ApiService by lazy { retrofit.create(ApiService::class.java) }

    fun plainClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

    fun applyBase(server: ServerConfig) {
        baseUrl = normalizeAddress(server.address)
    }

    fun normalizeAddress(address: String): HttpUrl? {
        var a = address.trim()
        if (a.isEmpty()) return null
        if (!a.startsWith("http://") && !a.startsWith("https://")) a = "http://$a"
        while (a.endsWith("/")) a = a.dropLast(1)
        return try {
            a.toHttpUrl().newBuilder().build()
        } catch (e: Exception) {
            null
        }
    }

    /** Concatenate a base path with a sub path, avoiding a missing/duplicate '/'. */
    private fun joinPath(base: String, sub: String): String {
        if (base.isEmpty() || base == "/") return sub
        return base.removeSuffix("/") + "/" + sub.removePrefix("/")
    }

    /** Build an absolute URL like http://host:port[/prefix] + path */
    fun absolute(path: String): String? {
        val base = baseUrl ?: return null
        val clean = if (path.startsWith("/")) path else "/$path"
        return base.newBuilder().encodedPath(joinPath(base.encodedPath, clean)).build().toString()
    }

    /** Resolve a possibly-relative URL (channel logo etc.) against the current server. */
    fun resolve(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        return absolute(raw)
    }

    suspend fun checkHealth(address: String): Boolean = withContext(Dispatchers.IO) {
        val url = normalizeAddress(address) ?: return@withContext false
        val target = url.newBuilder().encodedPath(joinPath(url.encodedPath, "/api/health")).build()
        try {
            val client = plainClient()
            val resp: Response = client.newCall(Request.Builder().url(target).build()).execute()
            val ok = resp.code in 200..299
            resp.close()
            ok
        } catch (e: Exception) {
            false
        }
    }
}
