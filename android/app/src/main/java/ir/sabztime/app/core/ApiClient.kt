package ir.sabztime.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class ApiEnvelope(
    val status: String = "",
    val message: String? = null,
    val code: String? = null
)

/**
 * کلاینت HTTP سبک برای API موجود (PHP): همه پاسخ‌ها JSON هستند و
 * خطاها به‌صورت {status:"error", message:"..."} برمی‌گردند.
 */
class ApiClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onSessionExpired: (() -> Unit)? = null,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }
) {
    val serializer: Json get() = json

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun get(path: String, params: Map<String, String> = emptyMap()): JsonObject =
        request("GET", path, params)

    suspend fun post(path: String, params: Map<String, String> = emptyMap(), body: JsonObject? = null): JsonObject =
        request("POST", path, params, body)

    suspend fun request(
        method: String,
        path: String,
        params: Map<String, String> = emptyMap(),
        body: JsonObject? = null
    ): JsonObject = withContext(Dispatchers.IO) {
        val urlBuilder = (baseUrl.trimEnd('/') + "/" + path.trimStart('/')).toHttpUrlBuilder(params)
        val builder = Request.Builder().url(urlBuilder.build())
        tokenProvider()?.let { builder.header("X-App-Token", it) }
        builder.header("Accept", "application/json")
        if (method == "POST") {
            val payload = body ?: buildJsonObject {}
            builder.post(payload.toString().toRequestBody(mediaType))
            builder.header("Content-Type", mediaType.toString())
        } else {
            builder.get()
        }

        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw SabzException.Network(e)
        }

        response.use { resp ->
            val text = resp.body?.string() ?: ""
            val root = try {
                json.parseToJsonElement(text).jsonObject
            } catch (e: Exception) {
                throw SabzException.Server("پاسخ نامعتبر از سرور دریافت شد (HTTP ${resp.code}).", "BAD_RESPONSE", resp.code)
            }

            if (!resp.isSuccessful) {
                val msg = root["message"]?.jsonPrimitive?.contentOrNull
                    ?: "خطای سرور (HTTP ${resp.code})."
                failWithHttpError(msg, root["code"]?.jsonPrimitive?.contentOrNull, resp.code)
            }

            val status = root["status"]?.jsonPrimitive?.contentOrNull
            if (status == "error") {
                val msg = root["message"]?.jsonPrimitive?.contentOrNull ?: "خطای ناشناخته."
                failWithHttpError(msg, root["code"]?.jsonPrimitive?.contentOrNull, resp.code)
            }
            root
        }
    }

    /** خطا را می‌سازد، و در صورت انقضای نشست، به لایه بالاتر اطلاع می‌دهد تا کاربر به صفحه ورود برگردد. */
    private fun failWithHttpError(message: String, code: String?, httpCode: Int): Nothing {
        val error = buildHttpError(message, code, httpCode)
        if (error is SabzException.SessionExpired) onSessionExpired?.invoke()
        throw error
    }

    private fun buildHttpError(message: String, code: String?, httpCode: Int): SabzException =
        when {
            code == "AUTH_REQUIRED" || message.contains("نشست", ignoreCase = false) && code != null ->
                SabzException.SessionExpired()
            httpCode == 401 -> SabzException.Auth(message, code)
            else -> SabzException.Server(message, code, httpCode)
        }

    private fun String.toHttpUrlBuilder(params: Map<String, String>): okhttp3.HttpUrl.Builder {
        val base = this.toHttpUrlOrNull()
            ?: throw SabzException.Server("آدرس سرور نامعتبر است.", "BAD_URL", 0)
        val builder = base.newBuilder()
        params.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder
    }
}

// --- کمکی‌های JsonObject ---

fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { !it.isString || it.content.isNotEmpty() }?.contentOrNull

fun JsonObject.intOrNull(key: String): Int? = (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
fun JsonObject.longOrNull(key: String): Long? = (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
fun JsonObject.boolOr(key: String): Boolean? = (this[key] as? JsonPrimitive)?.let {
    it.contentOrNull?.let { c -> c.toBooleanStrictOrNull() } ?: (it.content == "1")
}

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

fun JsonObject.ensureBoolean(key: String): Boolean = boolOr(key) ?: false

/** ساخت بدنه درخواست POST */
fun jsonBody(block: JsonObjectBuilder.() -> Unit): JsonObject {
    val builder = JsonObjectBuilder()
    builder.block()
    return builder.build()
}

class JsonObjectBuilder {
    private val m = mutableMapOf<String, JsonElement>()
    fun put(key: String, value: String?) { m[key] = JsonPrimitive(value ?: "") }
    fun put(key: String, value: Number?) { m[key] = JsonPrimitive(value ?: 0) }
    fun put(key: String, value: Boolean) { m[key] = JsonPrimitive(value) }
    fun putObj(key: String, value: JsonObject) { m[key] = value }
    fun build(): JsonObject = JsonObject(m)
}
