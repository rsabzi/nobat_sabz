package ir.sabztime.app.core

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiClient
    private var token: String? = null

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        api = ApiClient(baseUrl = server.url("/").toString(), tokenProvider = { token })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `success GET returns json object and no token when absent`() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"success","data":[1,2]}"""))
        val res: JsonObject = api.get("api.php", mapOf("action" to "test"))
        assertEquals("success", res["status"]!!.jsonPrimitive.content)
        assertNull(server.takeRequest().getHeader("X-App-Token"))
    }

    @Test
    fun `token header is sent when present`() = runTest {
        token = "abc123token"
        server.enqueue(MockResponse().setBody("""{"status":"success"}"""))
        api.get("api.php")
        assertEquals("abc123token", server.takeRequest().getHeader("X-App-Token"))
    }

    @Test
    fun `status error becomes server exception`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(409)
                .setBody("""{"status":"error","message":"این زمان قبلاً رزرو شده است."}""")
        )
        val error = runCatching { api.get("api.php") }.exceptionOrNull()
        assertTrue(error is SabzException.Server)
        assertEquals("این زمان قبلاً رزرو شده است.", (error as SabzException).message)
    }

    @Test
    fun `session expired maps to SessionExpired`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"status":"error","message":"نشست شما منقضی شده است.","code":"AUTH_REQUIRED"}""")
        )
        val error = runCatching { api.get("api.php") }.exceptionOrNull()
        assertTrue(error is SabzException.SessionExpired)
    }

    @Test
    fun `connection failure maps to Network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val error = runCatching { api.get("api.php") }.exceptionOrNull()
        assertTrue(error is SabzException.Network)
        assertEquals(SabzException.Kind.NETWORK, (error as SabzException).kind)
    }

    @Test
    fun `post sends json body`() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"success"}"""))
        api.post("api.php", body = jsonBody {
            put("action", "test")
            put("id", 5)
        })
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertTrue(req.body.readUtf8().contains("\"action\":\"test\""))
    }
}
