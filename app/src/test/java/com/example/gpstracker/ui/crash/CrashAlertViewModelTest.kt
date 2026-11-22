package com.example.gpstracker.ui.crash

import com.example.gpstracker.data.emergency.EmergencyNotifier
import com.example.gpstracker.tracing.MemorySink
import com.example.gpstracker.tracing.Tracing
import com.example.gpstracker.ui.model.AlertOutcome
import com.example.gpstracker.ui.model.DrivingEventUi
import com.example.gpstracker.ui.model.EventType
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.SettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrashAlertViewModelTest {
    private class FakeNotifier : EmergencyNotifier {
        var allowed = true
        var smsWorks = true
        val texts = mutableListOf<Pair<String, String>>()
        val calls = mutableListOf<String>()

        override fun canAlert() = allowed

        override fun sendText(phone: String, message: String) = smsWorks.also { if (it) texts += phone to message }

        override fun call(phone: String) = calls.add(phone)
    }

    private val dispatcher = StandardTestDispatcher()
    private val sink = MemorySink()
    private val notifier = FakeNotifier()
    private val settings = MutableStateFlow(SettingsUiState(emergencyContactName = "Mom", emergencyContactPhone = "+370 600 12345"))
    private val crashes = MutableStateFlow<DrivingEventUi?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Tracing.reset()
        Tracing.install(sink)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        Tracing.reset()
    }

    private fun TestScope.alert() = CrashAlertViewModel(
        settings = settings,
        position = { GeoPointUi(54.9, 23.9) },
        notifier = notifier,
        crashes = crashes,
        clock = { 0 },
    ).also { runCurrent() }

    private fun crash(id: String) =
        DrivingEventUi(id, EventType.CrashSuspected, "Possible crash", "3.1 g impact", 0, "14:05", "Jonavos g.", GeoPointUi(54.9, 23.9))

    @Test
    fun `without an answer in 10 seconds the contact gets a text and a call`() = runTest(dispatcher) {
        val vm = alert()
        crashes.value = crash("c1")
        runCurrent()
        assertTrue(vm.state.value.visible)

        advanceTimeBy(9_000)
        assertTrue("nothing sent before the countdown ends", notifier.texts.isEmpty())
        advanceTimeBy(1_001)
        assertEquals(AlertOutcome.TextedAndCalled, vm.state.value.outcome)
        assertEquals("+37060012345", notifier.texts.single().first)
        assertTrue(notifier.texts.single().second.contains("54.90000, 23.90000"))
        assertFalse("a real crash is not a test", notifier.texts.single().second.startsWith("TEST"))
        assertEquals(listOf("+37060012345"), notifier.calls)
    }

    @Test
    fun `I'm OK stops everything`() = runTest(dispatcher) {
        val vm = alert()
        vm.trigger()
        advanceTimeBy(5_000)
        vm.onImOk()
        advanceTimeBy(10_000)
        assertFalse(vm.state.value.visible)
        assertTrue(notifier.texts.isEmpty() && notifier.calls.isEmpty())
    }

    @Test
    fun `send now does not wait, and the test alert says it is a test`() = runTest(dispatcher) {
        val vm = alert()
        vm.trigger()
        vm.onSendNow()
        assertEquals(AlertOutcome.TextedAndCalled, vm.state.value.outcome)
        assertTrue(notifier.texts.single().second.startsWith("TEST"))
    }

    @Test
    fun `without a number or permission nothing is sent, and the alert says why`() = runTest(dispatcher) {
        notifier.allowed = false
        val vm = alert()
        vm.trigger()
        vm.onSendNow()
        assertEquals(AlertOutcome.NotAllowed, vm.state.value.outcome)

        settings.value = settings.value.copy(emergencyContactPhone = "")
        vm.trigger()
        vm.onSendNow()
        assertEquals(AlertOutcome.NoNumber, vm.state.value.outcome)
        assertTrue(notifier.texts.isEmpty() && notifier.calls.isEmpty())
    }

    @Test
    fun `when the text fails the call still goes out`() = runTest(dispatcher) {
        notifier.smsWorks = false
        val vm = alert()
        vm.trigger()
        vm.onSendNow()
        assertEquals(AlertOutcome.CalledOnly, vm.state.value.outcome)
        assertEquals(1, notifier.calls.size)
    }

    @Test
    fun `crashes start the alert only when crash detection is on`() = runTest(dispatcher) {
        settings.value = settings.value.copy(crashDetectionEnabled = false)
        val vm = alert()
        crashes.value = crash("c1")
        runCurrent()
        assertFalse(vm.state.value.visible)
    }

    @Test
    fun `logs never contain the contact's number or name`() = runTest(dispatcher) {
        val vm = alert()
        vm.trigger()
        vm.onSendNow()
        val logs = sink.records.value.joinToString("\n") { it.format() }
        assertTrue(logs.contains("emergency contact alerted"))
        listOf("600", "12345", "Mom").forEach { assertTrue("logs contain $it", it !in logs) }
    }
}
