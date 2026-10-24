package com.dawahayah.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dawahayah.app.features.body.LocalAnatomyWebView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Loads the actual bundled GLB in the production WebView, with network loads disabled. */
@RunWith(AndroidJUnit4::class)
class LocalAnatomyViewerTest {
    @Test fun verifiedBinaryLoadsAndSelectionBridgeReachesHost() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val ready = CountDownLatch(1)
            val picked = CountDownLatch(1)
            val count = AtomicInteger(0)
            val failure = AtomicReference<String?>(null)
            val selected = AtomicReference<String?>(null)
            lateinit var viewer: LocalAnatomyWebView
            scenario.onActivity { activity ->
                viewer = LocalAnatomyWebView(activity,
                    onMeshSelected = { selected.set(it); picked.countDown() },
                    onFallback = { failure.set(it); ready.countDown() },
                    onModelLoaded = { count.set(it); ready.countDown() })
                activity.setContentView(viewer)
            }
            try {
                assertTrue("Model did not finish loading", ready.await(30, TimeUnit.SECONDS))
                assertNull("Viewer fallback: ${failure.get()}", failure.get())
                assertEquals(6, count.get())
                scenario.onActivity {
                    assertTrue(viewer.settings.blockNetworkLoads)
                    viewer.evaluateJavascript("window.AndroidAnatomyBridge.onMeshSelected('untrusted');window.AndroidAnatomyBridge.onMeshSelected('Atrium_Dextrum');", null)
                }
                assertTrue(picked.await(5, TimeUnit.SECONDS))
                assertEquals("Atrium_Dextrum", selected.get())
            } finally { scenario.onActivity { viewer.releaseViewer() } }
        }
    }

    @Test fun corruptBinaryActivatesFallbackInsteadOfProceduralGeometry() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val ready = CountDownLatch(1)
            val fallback = CountDownLatch(1)
            val failure = AtomicReference<String?>(null)
            lateinit var viewer: LocalAnatomyWebView
            scenario.onActivity { activity ->
                viewer = LocalAnatomyWebView(activity, onMeshSelected = {},
                    onFallback = { failure.set(it); fallback.countDown(); ready.countDown() },
                    onModelLoaded = { ready.countDown() })
                activity.setContentView(viewer)
            }
            try {
                assertTrue(ready.await(30, TimeUnit.SECONDS))
                assertNull(failure.get())
                scenario.onActivity { viewer.evaluateJavascript("window.loadGlbFromBase64('AA==');", null) }
                assertTrue("Corrupt GLB did not trigger fallback", fallback.await(5, TimeUnit.SECONDS))
                assertEquals("GLB parsing failed", failure.get())
            } finally { scenario.onActivity { viewer.releaseViewer() } }
        }
    }
}
