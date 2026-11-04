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

class AristaHttpException(message: String, val statusCode: Int) : IOException(message)

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

    suspend fun requestPasswordRecovery(email: String): Unit = withContext(Dispatchers.IO) {
        if (email.isBlank()) throw IOException("Masukkan email akun.")
        call("/auth/v1/recover", "POST", JSONObject().put("email", email.trim()), null)
        Unit
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

    suspend fun refreshSession(refreshToken: String): AristaSession = withContext(Dispatchers.IO) {
        val data = JSONObject(call(
            "/auth/v1/token?grant_type=refresh_token",
            "POST",
            JSONObject().put("refresh_token", refreshToken),
            null
        ))
        val user = data.getJSONObject("user")
        AristaSession(
            data.getString("access_token"),
            data.optString("refresh_token", refreshToken),
            user.getString("id"),
            user.optString("email", "")
        )
    }

    suspend fun saveSale(token: String, sale: JSONObject): String = withContext(Dispatchers.IO) {
        val id = sale.optString("id")
        if (id.isNotBlank() && saleExists(token, id)) return@withContext "already_recorded"
        call("/rest/v1/rpc/catat_penjualan", "POST", JSONObject().put("p_sale", sale), token)
    }


    suspend fun saleExists(token: String, saleId: String): Boolean = withContext(Dispatchers.IO) {
        val rows = JSONArray(call("/rest/v1/sales?id=eq.$saleId&select=id&limit=1", "GET", null, token))
        rows.length() > 0
    }
    suspend fun sales(token: String): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(call("/rest/v1/sales?select=id,sale_date,sale_time,items,total,method,paid,change_amount,cashier_name,customer,batal,diskon&order=sale_date.desc,sale_time.desc&limit=200", "GET", null, token))
    }

    suspend fun salesForRange(token: String, from: String, to: String): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(call("/rest/v1/sales?select=id,sale_date,sale_time,items,total,method,paid,change_amount,cashier_name,customer,batal,diskon&sale_date=gte.$from&sale_date=lte.$to&order=sale_date.desc,sale_time.desc&limit=1000", "GET", null, token))
    }


    suspend fun expenses(token: String): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(call("/rest/v1/expenses?select=id,tanggal,nama,kategori,qty,harga,total,catatan&order=tanggal.desc&limit=200", "GET", null, token))
    }

    suspend fun receivableSales(token: String): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(call("/rest/v1/sales?select=id,sale_date,sale_time,customer,items,total,batal&method=eq.Piutang&order=sale_date.asc,created_at.asc&limit=2000", "GET", null, token))
    }

    suspend fun receivablePayments(token: String): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(call("/rest/v1/pembayaran?select=sale_id,nominal,tanggal,metode&order=tanggal.asc&limit=5000", "GET", null, token))
    }

    suspend fun payReceivableNote(token: String, saleId: String, amount: Double, method: String): String = withContext(Dispatchers.IO) {
        call("/rest/v1/rpc/bayar_nota", "POST", JSONObject().put("p_sale", saleId).put("p_nominal", amount).put("p_metode", method), token)
    }

    suspend fun payReceivableCustomer(token: String, customer: String, amount: Double, method: String): String = withContext(Dispatchers.IO) {
        call("/rest/v1/rpc/bayar_sekaligus", "POST", JSONObject().put("p_customer", customer).put("p_nominal", amount).put("p_metode", method), token)
    }


    suspend fun createExpense(token: String, expense: JSONObject): String = withContext(Dispatchers.IO) {
        call("/rest/v1/expenses", "POST", expense, token)
    }

    suspend fun cashierAction(token: String, payload: JSONObject): String = withContext(Dispatchers.IO) {
        call("/functions/v1/swift-worker", "POST", payload, token)
    }

    suspend fun addProduct(token: String, product: JSONObject): String = withContext(Dispatchers.IO) {
        call("/rest/v1/products", "POST", product, token)
    }

    suspend fun updateProduct(token: String, id: String, product: JSONObject): String = withContext(Dispatchers.IO) {
        call("/rest/v1/products?id=eq.$id", "PATCH", product, token)
    }

    suspend fun deleteProduct(token: String, id: String): String = withContext(Dispatchers.IO) {
        call("/rest/v1/products?id=eq.$id", "PATCH", JSONObject().put("deleted", true), token)
    }

    private fun call(path: String, method: String, body: JSONObject?, token: String?): String {
        val builder = Request.Builder()
            .url(base + path)
            .header("apikey", anon)
            .header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        when (method) {
            "POST" -> builder.post((body ?: JSONObject()).toString().toRequestBody(jsonType))
            "PATCH" -> builder.patch((body ?: JSONObject()).toString().toRequestBody(jsonType))
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
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
                throw AristaHttpException(message, response.code)
            }
            return text
        }
    }
}
