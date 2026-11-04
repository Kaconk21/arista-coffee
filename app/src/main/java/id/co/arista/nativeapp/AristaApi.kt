package id.co.arista.nativeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AristaSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)

data class AristaProfile(val id: String, val nama: String, val peran: String)
data class AristaProduct(val id: String, val name: String, val category: String, val price: Double, val cost: Double, val stock: Int)

class AristaApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anon = BuildConfig.SUPABASE_ANON_KEY

    suspend fun login(email: String, password: String): AristaSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val data = JSONObject(call("/auth/v1/token?grant_type=password", "POST", body, null))
        val user = data.getJSONObject("user")
        AristaSession(
            data.getString("access_token"),
            data.getString("refresh_token"),
            user.getString("id"),
            user.optString("email", email)
        )
    }

    suspend fun profile(token: String, userId: String): AristaProfile = withContext(Dispatchers.IO) {
        val rows = JSONArray(call(
            "/rest/v1/profiles?id=eq.$userId&select=id,nama,peran",
            "GET", null, token
        ))
        val row = rows.optJSONObject(0) ?: throw IOException("Profil pengguna tidak ditemukan.")
        AristaProfile(row.getString("id"), row.getString("nama"), row.getString("peran"))
    }

    suspend fun products(token: String): List<AristaProduct> = withContext(Dispatchers.IO) {
        val rows = JSONArray(call(
            "/rest/v1/products?deleted=eq.false&select=id,name,cat,price,cost,stock&order=name.asc",
            "GET", null, token
        ))
        buildList {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                add(AristaProduct(
                    row.optString("id"),
                    row.optString("name"),
                    row.optString("cat"),
                    row.optDouble("price"),
                    row.optDouble("cost"),
                    row.optInt("stock")
                ))
            }
        }
    }

    suspend fun saveSale(token: String, sale: JSONObject): String = withContext(Dispatchers.IO) {
        call("/rest/v1/rpc/catat_penjualan", "POST", JSONObject().put("p_sale", sale), token)
    }

    suspend fun cashierAction(token: String, payload: JSONObject): String = withContext(Dispatchers.IO) {
        call("/functions/v1/swift-worker", "POST", payload, token)
    }

    private fun call(path: String, method: String, body: JSONObject?, token: String?): String {
        val builder = Request.Builder()
            .url(base + path)
            .header("apikey", anon)
            .header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        when (method) {
            "POST" -> builder.post((body ?: JSONObject()).toString().toRequestBody(jsonType))
            "GET" -> builder.get()
            else -> throw IllegalArgumentException("Metode tidak didukung")
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = try {
                    val obj = JSONObject(text)
                    obj.optString("msg").ifBlank {
                        obj.optString("message").ifBlank {
                            obj.optString("error", "Permintaan gagal (${response.code})")
                        }
                    }
                } catch (_: Exception) { "Permintaan gagal (${response.code})" }
                throw IOException(message)
            }
            return text
        }
    }
}
