package cl.duocuc.aulaviva.data.remote

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SpringBootClientTest {

    private lateinit var chain: Interceptor.Chain

    // Reflection helper to access private interceptor
    private fun getAuthInterceptor(): Interceptor {
        val field = SpringBootClient::class.java.getDeclaredField("authInterceptor")
        field.isAccessible = true
        return field.get(SpringBootClient) as Interceptor
    }

    @Before
    fun setup() {
        chain = mockk(relaxed = true)
        // Mock the TokenManager object to control its state cleanly
        mockkObject(TokenManager)
    }

    @After
    fun tearDown() {
        unmockkObject(TokenManager)
    }

    @Test
    fun `should add Authorization header if token exists and header is missing`() {
        // Arrange
        val testToken = "test_token_123"
        every { TokenManager.getToken() } returns testToken

        val originalRequest = Request.Builder()
            .url("http://localhost/api/test")
            .build()
        
        val slot = slot<Request>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(capture(slot)) } answers {
            Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        val interceptor = getAuthInterceptor()

        // Act
        interceptor.intercept(chain)

        // Assert
        verify { chain.proceed(any()) }
        assertEquals("Bearer $testToken", slot.captured.header("Authorization"))
    }

    @Test
    fun `should NOT add Authorization header if header already exists`() {
        // Arrange
        val testToken = "test_token_123"
        val existingHeader = "Bearer existing_token_456"
        every { TokenManager.getToken() } returns testToken

        val originalRequest = Request.Builder()
            .url("http://localhost/api/test")
            .header("Authorization", existingHeader)
            .build()
        
        val slot = slot<Request>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(capture(slot)) } answers {
            Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        val interceptor = getAuthInterceptor()

        // Act
        interceptor.intercept(chain)

        // Assert
        verify { chain.proceed(any()) }
        
        // Should preserve the EXISTING header
        assertEquals(existingHeader, slot.captured.header("Authorization"))
        // Existing header + added header = 1 total (if logical check works)
        // OkHttp headers("name") returns list of all values for this name
        assertEquals(1, slot.captured.headers("Authorization").size)
    }

    @Test
    fun `should not add header if no token exists`() {
        // Arrange
        every { TokenManager.getToken() } returns null

        val originalRequest = Request.Builder()
            .url("http://localhost/api/test")
            .build()
        
        val slot = slot<Request>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(capture(slot)) } answers {
             Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        val interceptor = getAuthInterceptor()

        // Act
        interceptor.intercept(chain)

        // Assert
        verify { chain.proceed(any()) }
        assertEquals(null, slot.captured.header("Authorization"))
    }
}
