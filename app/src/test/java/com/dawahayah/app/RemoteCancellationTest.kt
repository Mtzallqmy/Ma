package com.dawahayah.app

import com.dawahayah.app.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test
import java.lang.reflect.Proxy

class RemoteCancellationTest {
    @Suppress("UNCHECKED_CAST")
    private fun <T> cancelledService(type: Class<T>): T = Proxy.newProxyInstance(
        type.classLoader, arrayOf(type)
    ) { _, _, _ -> throw CancellationException("worker stopped") } as T

    @Test fun functionRequestPropagatesCancellation() = runBlocking {
        val source = RetrofitNeonDataSource("https://example.com/", cancelledService(NeonApiService::class.java))
        try { source.fetchCatalog("ar"); fail("Cancellation must not become a normal API failure") }
        catch (_: CancellationException) { }
    }

    @Test fun dataApiRequestPropagatesCancellation() = runBlocking {
        val source = NeonDataApiRemoteDataSource("https://example.com/rest/v1/", cancelledService(DawaNeonDataApiService::class.java))
        try { source.fetchCatalog("ar"); fail("Cancellation must not become a normal API failure") }
        catch (_: CancellationException) { }
    }
}
