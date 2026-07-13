package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.Reading
import com.example.data.model.Settings
import com.example.data.repository.UtilityRepository
import com.example.data.util.BillingCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class UtilityViewModel(private val repository: UtilityRepository) : ViewModel() {

    // Navigation State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen

    // Selected Customer ID for entering reading or editing
    private val _selectedCustomerId = MutableStateFlow<Int?>(null)
    val selectedCustomerId: StateFlow<Int?> = _selectedCustomerId

    // Active input for reading
    val currentReadingInput = MutableStateFlow("")

    // Search query for customer filter
    val customerSearchQuery = MutableStateFlow("")

    // Search query for history
    val historySearchQuery = MutableStateFlow("")

    // Reading Queue State
    val readingQueue = MutableStateFlow<List<Customer>>(emptyList())
    val activeQueueIndex = MutableStateFlow(-1)

    // Active customer flow
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomerId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getCustomerFlow(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Settings Flow
    val settings: StateFlow<Settings> = repository.settingsFlow
        .combine(flowOf(true)) { s, _ ->
            s ?: repository.getOrInitializeSettings()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Settings()
        )

    // Customers Flow (reactive search filter)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> = customerSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allCustomers
            } else {
                combine(repository.allCustomers) { list ->
                    list[0].filter {
                        it.name.contains(query, ignoreCase = true) ||
                        it.meterNumber.contains(query, ignoreCase = true) ||
                        it.phoneNumber.contains(query, ignoreCase = true)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Readings Flow (searched / filtered history)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val readings: StateFlow<List<Reading>> = historySearchQuery
        .flatMapLatest { query ->
            repository.searchReadings(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dashboard Stats Flow
    val dashboardStats: StateFlow<DashboardStats> = combine(repository.allCustomers, repository.allReadings) { custs, reads ->
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDay(now)
        val todayEnd = getEndOfDay(now)

        val dueToday = mutableListOf<Customer>()
        val upcoming = mutableListOf<Customer>()
        val overdue = mutableListOf<Customer>()

        custs.forEach { customer ->
            val expected = customer.nextExpectedDate
            if (expected < todayStart) {
                overdue.add(customer)
            } else if (expected in todayStart..todayEnd) {
                dueToday.add(customer)
            } else if (expected in (todayEnd + 1)..(todayEnd + 4 * 24 * 60 * 60 * 1000L)) {
                // badge says 🟢 Due in 4 Days so let's match the 4 days range
                upcoming.add(customer)
            }
        }

        // Calculate readings completed this month
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = getStartOfDay(cal.timeInMillis)
        val readingsThisMonth = reads.count { it.readingDate >= monthStart }

        DashboardStats(
            totalCustomers = custs.size,
            dueTodayCount = dueToday.size,
            upcomingCount = upcoming.size,
            overdueCount = overdue.size,
            dueTodayList = dueToday,
            upcomingList = upcoming,
            overdueList = overdue,
            readingsCompletedThisMonth = readingsThisMonth,
            recentReadings = reads.take(5)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    // Real-time Bill Calculator
    val activeBillCalculation: StateFlow<BillingCalculator.BillResult?> = combine(
        selectedCustomer,
        currentReadingInput,
        settings
    ) { customer, inputStr, _ ->
        if (customer == null) return@combine null
        val readingVal = inputStr.toDoubleOrNull() ?: return@combine null

        val prevReading = customer.currentReading
        val lastDate = customer.lastReadingDate
        val expectedDate = customer.nextExpectedDate
        val now = System.currentTimeMillis()

        BillingCalculator.calculateBill(
            currentReading = readingVal,
            previousReading = prevReading,
            lastReadingDate = lastDate,
            readingDate = now,
            expectedReadingDate = expectedDate,
            unitPrice = customer.unitPrice,
            demandCharge = customer.demandCharge,
            vatPercent = customer.vatPercent,
            billingMode = customer.billingMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            repository.getOrInitializeSettings()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectCustomerForReading(customerId: Int) {
        _selectedCustomerId.value = customerId
        currentReadingInput.value = ""
        navigateTo(Screen.ReadingScreen)
    }

    fun selectCustomerForEdit(customerId: Int) {
        _selectedCustomerId.value = customerId
        navigateTo(Screen.EditCustomer)
    }

    fun startReadingQueue() {
        val stats = dashboardStats.value
        // Queue includes customers who are due today or overdue
        val queue = stats.dueTodayList + stats.overdueList
        if (queue.isNotEmpty()) {
            readingQueue.value = queue
            activeQueueIndex.value = 0
            selectCustomerForReading(queue[0].id)
        }
    }

    fun addCustomer(
        name: String,
        meterNumber: String,
        phoneNumber: String,
        address: String,
        startingReading: Double,
        unitPrice: Double?,
        demandCharge: Double?,
        vatPercent: Double?,
        billingDay: Int,
        billingMode: String,
        notes: String
    ) {
        viewModelScope.launch {
            val currentSettings = repository.getOrInitializeSettings()
            repository.insertCustomer(
                name = name,
                meterNumber = meterNumber,
                phoneNumber = phoneNumber,
                address = address,
                startingReading = startingReading,
                unitPrice = unitPrice ?: currentSettings.defaultUnitPrice,
                demandCharge = demandCharge ?: currentSettings.defaultDemandCharge,
                vatPercent = vatPercent ?: currentSettings.defaultVatPercent,
                billingDay = billingDay,
                billingMode = billingMode,
                notes = notes
            )
            navigateTo(Screen.Customers)
        }
    }

    fun updateCustomerDetails(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            navigateTo(Screen.Customers)
        }
    }

    fun duplicateCustomer(customerId: Int) {
        viewModelScope.launch {
            repository.duplicateCustomer(customerId)
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
            if (_selectedCustomerId.value == id) {
                _selectedCustomerId.value = null
            }
            navigateTo(Screen.Customers)
        }
    }

    suspend fun getCustomerAverageUsage(customerId: Int): Double? {
        val list = repository.getReadingsForCustomerDirect(customerId)
        if (list.isEmpty()) return null
        val avg = list.map { it.actualUnits }.average()
        return if (avg.isNaN()) null else avg
    }

    fun saveReading(customerId: Int, readingVal: Double, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.saveReading(customerId, readingVal)
            if (success) {
                currentReadingInput.value = ""
                onSaved()

                // Handle Reading Queue workflow
                val queue = readingQueue.value
                val currentIndex = activeQueueIndex.value
                if (queue.isNotEmpty() && currentIndex >= 0 && currentIndex < queue.size) {
                    val nextIndex = currentIndex + 1
                    if (nextIndex < queue.size) {
                        activeQueueIndex.value = nextIndex
                        selectCustomerForReading(queue[nextIndex].id)
                    } else {
                        // Queue completed!
                        readingQueue.value = emptyList()
                        activeQueueIndex.value = -1
                        _selectedCustomerId.value = null
                        navigateTo(Screen.Dashboard)
                    }
                } else {
                    _selectedCustomerId.value = null
                    navigateTo(Screen.Dashboard)
                }
            }
        }
    }

    fun cancelReadingQueue() {
        readingQueue.value = emptyList()
        activeQueueIndex.value = -1
        _selectedCustomerId.value = null
        navigateTo(Screen.Dashboard)
    }

    fun undoLastReading(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.undoLastReading()
            onComplete(success)
        }
    }

    fun updateSettings(
        unitPrice: Double,
        demandCharge: Double,
        vatPercent: Double,
        billingMode: String,
        currencySymbol: String,
        utilityType: String,
        isDarkMode: Boolean?
    ) {
        viewModelScope.launch {
            val current = repository.getOrInitializeSettings()
            val updated = current.copy(
                defaultUnitPrice = unitPrice,
                defaultDemandCharge = demandCharge,
                defaultVatPercent = vatPercent,
                billingMode = billingMode,
                currencySymbol = currencySymbol,
                utilityType = utilityType,
                isDarkMode = isDarkMode
            )
            repository.saveSettings(updated)
        }
    }

    fun restoreBackupJson(jsonStr: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreData(jsonStr)
            onComplete(success)
        }
    }

    fun generateBackupJsonAsync(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val backup = repository.backupData()
            onComplete(backup)
        }
    }

    private fun getStartOfDay(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

enum class Screen {
    Dashboard,
    Customers,
    History,
    Settings,
    AddCustomer,
    EditCustomer,
    ReadingScreen
}

data class DashboardStats(
    val totalCustomers: Int = 0,
    val dueTodayCount: Int = 0,
    val upcomingCount: Int = 0,
    val overdueCount: Int = 0,
    val readingsCompletedThisMonth: Int = 0,
    val dueTodayList: List<Customer> = emptyList(),
    val upcomingList: List<Customer> = emptyList(),
    val overdueList: List<Customer> = emptyList(),
    val recentReadings: List<Reading> = emptyList()
)
