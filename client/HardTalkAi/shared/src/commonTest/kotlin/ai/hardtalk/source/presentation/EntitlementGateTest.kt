package ai.hardtalk.source.presentation

import ai.hardtalk.source.data.billing.InMemoryEntitlementRepository
import ai.hardtalk.source.domain.billing.BillingActionResult
import ai.hardtalk.source.domain.model.Persona
import ai.hardtalk.source.domain.model.Scenario
import ai.hardtalk.source.domain.repository.PracticeRepository
import ai.hardtalk.source.presentation.paywall.PaywallViewModel
import ai.hardtalk.source.presentation.scenarios.ScenarioListUiState
import ai.hardtalk.source.presentation.scenarios.ScenarioListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EntitlementGateTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unpaidCatalogMarksOnlyFreeScenarioPlayable() = runTest {
        val entitlements = InMemoryEntitlementRepository(initiallyPro = false, configured = true)
        val viewModel = ScenarioListViewModel(CatalogRepository(), entitlements)
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Ready)
        assertFalse(state.isPro)
        val playable = state.scenarios.filter { entitlements.canPlay(it) }.map { it.id }
        assertEquals(listOf("ask-for-raise"), playable)
    }

    @Test
    fun proUnlocksLockedDrills() = runTest {
        val entitlements = InMemoryEntitlementRepository(initiallyPro = true, configured = true)
        val viewModel = ScenarioListViewModel(CatalogRepository(), entitlements)
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Ready)
        assertTrue(state.isPro)
        assertTrue(state.scenarios.all { entitlements.canPlay(it) })
    }

    @Test
    fun scenarioListStillLoadsWhenBillingIsUnconfigured() = runTest {
        val entitlements = InMemoryEntitlementRepository(
            configured = false,
            statusMessage = "offline",
        )
        val viewModel = ScenarioListViewModel(CatalogRepository(), entitlements)
        val state = viewModel.uiState.value
        assertTrue(state is ScenarioListUiState.Ready)
        assertFalse(state.billingConfigured)
        assertTrue(entitlements.canPlay(state.scenarios.first { it.free }))
        assertFalse(entitlements.canPlay(state.scenarios.first { !it.free }))
    }

    @Test
    fun simulatedPurchaseUnlocksCatalog() = runTest {
        val entitlements = InMemoryEntitlementRepository(
            initiallyPro = false,
            configured = true,
            monthlyPriceLabel = "$4.99 / month",
            purchaseResult = BillingActionResult.Success,
        )
        assertFalse(entitlements.state.value.isPro)
        val paywall = PaywallViewModel(entitlements)
        paywall.purchase()
        assertTrue(entitlements.state.value.isPro)
        assertTrue(CatalogRepository().getScenarios().all { entitlements.canPlay(it) })
    }

    @Test
    fun failedPurchaseLeavesScenariosLocked() = runTest {
        val entitlements = InMemoryEntitlementRepository(
            initiallyPro = false,
            configured = true,
            monthlyPriceLabel = "$4.99 / month",
            purchaseResult = BillingActionResult.Error("sandbox down"),
        )
        PaywallViewModel(entitlements).purchase()
        assertFalse(entitlements.state.value.isPro)
        assertEquals("sandbox down", entitlements.state.value.message)
    }

    @Test
    fun restoreUnlocksWhenStoreReturnsPro() = runTest {
        val entitlements = InMemoryEntitlementRepository(
            initiallyPro = false,
            configured = true,
            monthlyPriceLabel = "$4.99 / month",
            purchaseResult = BillingActionResult.Success,
        )
        PaywallViewModel(entitlements).restore()
        assertTrue(entitlements.state.value.isPro)
    }

    private class CatalogRepository : PracticeRepository {
        override suspend fun getScenarios(): List<Scenario> = listOf(
            Scenario(
                id = "ask-for-raise",
                title = "Ask your manager for a raise",
                summary = "Make the case.",
                difficulty = "moderate",
                persona = Persona("Dana", "EM", "busy"),
                opening = "Hey",
                goals = listOf("Ask"),
                free = true,
            ),
            Scenario(
                id = "give-feedback",
                title = "Give a teammate critical feedback",
                summary = "Name the miss.",
                difficulty = "hard",
                persona = Persona("Sam", "Peer", "defensive"),
                opening = "You wanted to chat?",
                goals = listOf("Name it"),
                free = false,
            ),
        )

        override suspend fun sendMessage(
            scenarioId: String,
            message: String,
            history: List<ai.hardtalk.source.domain.model.ChatMessage>,
        ) = error("not used")
    }
}
