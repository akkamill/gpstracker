package com.example.gpstracker.data.drive

import com.example.gpstracker.data.fake.FakeAuthRepository
import com.example.gpstracker.data.telematics.SimulatedTelemetrySource
import com.example.gpstracker.data.telematics.SimulatorMapLookup
import com.example.gpstracker.domain.trip.DriveTracker
import com.example.gpstracker.simulator.vehicle.SimulatedCar
import com.example.gpstracker.tracing.MemorySink
import com.example.gpstracker.tracing.RecordKind
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** One trace must follow a sample from the tracker format, through the app, into the driving rules. */
class TracingAcrossModulesTest {
    private val sink = MemorySink(capacity = 5_000, debugCapacity = 200)

    @Before
    fun setUp() {
        Tracing.reset()
        Tracing.install(sink)
    }

    @After
    fun tearDown() = Tracing.reset()

    @Test
    fun `a harsh braking can be followed from the tracker record to the driving rules`() = runTest {
        val repository = TelemetryDriveRepository(
            source = SimulatedTelemetrySource(SimulatedCar(startMs = 1_700_000_000_000L)),
            tracker = DriveTracker(map = SimulatorMapLookup()),
            settings = MutableStateFlow(SettingsUiState()),
            clock = { testScheduler.currentTime },
        )
        repository.start(backgroundScope)
        advanceTimeBy(120_000)

        val braking = sink.records.value.single { it.name == "driving event" && it.attributes["type"] == "HarshBraking" }
        val trace = sink.trace(braking.traceId!!)
        val steps = trace.map { "${it.module}:${it.name}" }

        assertEquals(listOf("telematics:avl.decode", "domain:driving event", "app:drive.update"), steps)
        // the app step is a child of the decode step, and the event was written inside the app step
        val decode = trace[0]
        val update = trace[2]
        assertEquals(decode.spanId, update.parentSpanId)
        assertEquals(update.spanId, braking.spanId)
        assertEquals("Savanorių pr.", braking.attributes["road"])
    }

    @Test
    fun `the whole story of a drive is in the logs`() = runTest {
        val repository = TelemetryDriveRepository(
            source = SimulatedTelemetrySource(SimulatedCar(startMs = 1_700_000_000_000L)),
            tracker = DriveTracker(map = SimulatorMapLookup()),
            settings = MutableStateFlow(SettingsUiState()),
            clock = { testScheduler.currentTime },
        )
        repository.start(backgroundScope)
        advanceTimeBy(120_000)

        val story = sink.records.value.filter { it.kind == RecordKind.Log }.map { "${it.module}:${it.name}" }
        listOf(
            "app:tracker connected",
            "simulator:engine started",
            "domain:trip started",
            "simulator:no mobile coverage, storing records",
            "app:tracker connection lost, waiting for it to come back",
            "domain:GPS fix lost, score paused",
            "domain:trip ended",
            "app:trip saved",
        ).forEach { assertTrue("missing $it", it in story) }
    }

    @Test
    fun `account logs never contain emails or passwords`() = runTest {
        val auth = FakeAuthRepository(latencyMs = 0)
        auth.signIn(FakeAuthRepository.DEMO_EMAIL, "wrong-password")
        auth.signIn(FakeAuthRepository.DEMO_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        auth.signUp("Ona", "ona@example.com", "secret123")
        auth.signOut()

        val text = sink.records.value.joinToString("\n") { it.format() }
        assertEquals(4, sink.records.value.size)
        listOf("@", "wrong-password", FakeAuthRepository.DEMO_PASSWORD, "secret123", "Ona").forEach {
            assertTrue("logs contain $it", it !in text)
        }
    }
}
