package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Customer
import com.example.data.model.Reading
import com.example.data.util.BillingCalculator
import com.example.data.util.ReportExporter
import com.example.ui.viewmodel.DashboardStats
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.UtilityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityAppUI(viewModel: UtilityViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Set custom page titles and terminology according to selected Utility Type
    val unitLabel = when (settings.utilityType) {
        "Electricity" -> "kWh"
        "Water" -> "m³"
        "Gas" -> "m³"
        "Generator" -> "Units"
        else -> "Units"
    }

    // Handle system back press
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        viewModel.navigateTo(Screen.Dashboard)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = getUtilityIcon(settings.utilityType),
                            contentDescription = settings.utilityType,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = when (currentScreen) {
                                Screen.Dashboard -> "UnitMate"
                                Screen.Customers -> "Meters & Customers"
                                Screen.AddCustomer -> "Add Customer"
                                Screen.EditCustomer -> "Edit Profile"
                                Screen.ReadingScreen -> "Enter Reading"
                                Screen.History -> "Readings History"
                                Screen.Settings -> "System Settings"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreen != Screen.Dashboard && 
                        currentScreen != Screen.Customers && 
                        currentScreen != Screen.History && 
                        currentScreen != Screen.Settings) {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // Exactly 4 tabs bottom navigation
            if (currentScreen == Screen.Dashboard || 
                currentScreen == Screen.Customers || 
                currentScreen == Screen.History || 
                currentScreen == Screen.Settings) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Dashboard,
                        onClick = { viewModel.navigateTo(Screen.Dashboard) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Dashboard) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Customers,
                        onClick = { viewModel.navigateTo(Screen.Customers) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Customers) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "Customers"
                            )
                        },
                        label = { Text("Customers", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_customers")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.History,
                        onClick = { viewModel.navigateTo(Screen.History) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.History) Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = "History"
                            )
                        },
                        label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_history")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Settings,
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == Screen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel, settings.currencySymbol, unitLabel, settings.utilityType)
                Screen.Customers -> CustomersScreen(viewModel, settings.currencySymbol, unitLabel)
                Screen.AddCustomer -> AddCustomerScreen(viewModel, settings.currencySymbol)
                Screen.EditCustomer -> EditCustomerScreen(viewModel, settings.currencySymbol)
                Screen.ReadingScreen -> ReadingScreen(viewModel, settings.currencySymbol, unitLabel)
                Screen.History -> HistoryScreen(viewModel, settings.currencySymbol, unitLabel)
                Screen.Settings -> SettingsScreen(viewModel)
            }
        }
    }
}

// Global helper to get dynamic icons depending on utilityType
fun getUtilityIcon(type: String): ImageVector {
    return when (type) {
        "Electricity" -> Icons.Default.ElectricBolt
        "Water" -> Icons.Default.Water
        "Gas" -> Icons.Default.LocalGasStation
        "Generator" -> Icons.Default.Power
        else -> Icons.Default.Handyman
    }
}

@Composable
fun DashboardScreen(
    viewModel: UtilityViewModel,
    currencySymbol: String,
    unitLabel: String,
    utilityType: String
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Search Bar at the very top of Dashboard as requested
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.customerSearchQuery.value = it },
                placeholder = { Text("Search customers by name, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.customerSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_bar"),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        item {
            // Dashboard Summary / Statistics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = utilityType,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DashboardMetricBlock(title = "Total Meters", value = "${stats.totalCustomers}")
                        DashboardMetricBlock(title = "Due Today", value = "${stats.dueTodayCount}", highlightColor = MaterialTheme.colorScheme.error)
                        DashboardMetricBlock(title = "Overdue", value = "${stats.overdueCount}", highlightColor = MaterialTheme.colorScheme.error)
                        DashboardMetricBlock(title = "Completed", value = "${stats.readingsCompletedThisMonth}")
                    }

                    // Reading Queue Action button!
                    val totalDue = stats.dueTodayCount + stats.overdueCount
                    if (totalDue > 0) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.startReadingQueue() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_readings_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start queue")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Today's Readings ($totalDue)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Today's Due / Overdue Section
        if (stats.dueTodayList.isNotEmpty() || stats.overdueList.isNotEmpty()) {
            item {
                Text(
                    text = "Attention Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(stats.overdueList) { customer ->
                DashboardCustomerCard(customer, "Overdue", viewModel, currencySymbol, unitLabel)
            }

            items(stats.dueTodayList) { customer ->
                DashboardCustomerCard(customer, "Due Today", viewModel, currencySymbol, unitLabel)
            }
        }

        // Upcoming Readings Section
        if (stats.upcomingList.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Readings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(stats.upcomingList) { customer ->
                DashboardCustomerCard(customer, "Upcoming", viewModel, currencySymbol, unitLabel)
            }
        }

        // Recent Readings Completed Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Readings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { viewModel.navigateTo(Screen.History) }) {
                    Text("View All")
                }
            }
        }

        if (stats.recentReadings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Empty",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No readings recorded this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(stats.recentReadings) { reading ->
                RecentReadingCard(reading, currencySymbol, unitLabel)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DashboardMetricBlock(
    title: String,
    value: String,
    highlightColor: Color? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = highlightColor ?: MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun DashboardCustomerCard(
    customer: Customer,
    status: String,
    viewModel: UtilityViewModel,
    currencySymbol: String,
    unitLabel: String
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(customer.nextExpectedDate))
    val daysLabel = getBadgeStatusLabel(customer.nextExpectedDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectCustomerForReading(customer.id) }
            .testTag("dashboard_cust_card_${customer.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Previous Reading: ${customer.currentReading.toInt()} $unitLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rate: $currencySymbol${customer.unitPrice}/$unitLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(daysLabel)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Due: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentReadingCard(
    reading: Reading,
    currencySymbol: String,
    unitLabel: String
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(reading.readingDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = reading.customerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr • ${reading.actualUnits.toInt()} $unitLabel (${reading.billableUnits.toInt()} billable)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, reading.grandTotal),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Customers Screen (Customer list and search)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomersScreen(
    viewModel: UtilityViewModel,
    currencySymbol: String,
    unitLabel: String
) {
    val customersList by viewModel.customers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.customerSearchQuery.collectAsStateWithLifecycle()
    var expandedMenuCustomerId by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialogForCustomer by remember { mutableStateOf<Customer?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                // Live Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.customerSearchQuery.value = it },
                    placeholder = { Text("Search by name, meter, or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.customerSearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customers_search_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (customersList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No customers matched your search.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Click the '+' button below to add a new customer profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(customersList, key = { it.id }) { customer ->
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(customer.nextExpectedDate))
                    val daysLabel = getBadgeStatusLabel(customer.nextExpectedDate)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.selectCustomerForReading(customer.id) },
                                onLongClick = { expandedMenuCustomerId = customer.id }
                            )
                            .testTag("customer_card_${customer.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (customer.meterNumber.isNotEmpty()) {
                                        Text(
                                            text = "Meter #: ${customer.meterNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Box {
                                    IconButton(onClick = { expandedMenuCustomerId = customer.id }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                    }

                                    DropdownMenu(
                                        expanded = expandedMenuCustomerId == customer.id,
                                        onDismissRequest = { expandedMenuCustomerId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Customer") },
                                            onClick = {
                                                expandedMenuCustomerId = null
                                                viewModel.selectCustomerForEdit(customer.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicate Customer") },
                                            onClick = {
                                                expandedMenuCustomerId = null
                                                viewModel.duplicateCustomer(customer.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                expandedMenuCustomerId = null
                                                showDeleteDialogForCustomer = customer
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Last Reading: ${customer.currentReading.toInt()} $unitLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Price: $currencySymbol${customer.unitPrice} • Billing Day: ${customer.billingDay}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(daysLabel)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Expected: $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.navigateTo(Screen.AddCustomer) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_customer_fab"),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Customer")
        }

        // Delete Dialog
        if (showDeleteDialogForCustomer != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialogForCustomer = null },
                title = { Text("Delete Customer?") },
                text = { Text("Are you sure you want to permanently delete ${showDeleteDialogForCustomer?.name}? All their readings history will be deleted as well.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialogForCustomer?.let { viewModel.deleteCustomer(it.id) }
                            showDeleteDialogForCustomer = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialogForCustomer = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// Add Customer Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(viewModel: UtilityViewModel, currencySymbol: String) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var meterNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var startingReading by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var demandCharge by remember { mutableStateOf("") }
    var vatPercent by remember { mutableStateOf("") }
    var billingDay by remember { mutableStateOf("") }
    var billingMode by remember { mutableStateOf("STANDARD_30_DAY") }
    var notes by remember { mutableStateOf("") }

    // Prepopulate default settings parameters
    LaunchedEffect(settings) {
        unitPrice = settings.defaultUnitPrice.toString()
        demandCharge = settings.defaultDemandCharge.toString()
        vatPercent = settings.defaultVatPercent.toString()
        billingDay = "1"
        billingMode = settings.billingMode
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "New Customer Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Enter details once to automate all monthly calculations and reminder updates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_name_field"),
                singleLine = true
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = meterNumber,
                    onValueChange = { meterNumber = it },
                    label = { Text("Meter # (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        }

        item {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startingReading,
                    onValueChange = { startingReading = it },
                    label = { Text("Start Reading *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_start_reading_field"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = billingDay,
                    onValueChange = { billingDay = it },
                    label = { Text("Billing Day (1-31) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_billing_day_field"),
                    singleLine = true
                )
            }
        }

        item {
            Text(
                text = "Tariff Details",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Unit Price ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = demandCharge,
                    onValueChange = { demandCharge = it },
                    label = { Text("Demand ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vatPercent,
                    onValueChange = { vatPercent = it },
                    label = { Text("VAT (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Billing Mode selection
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("Billing Method", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = billingMode == "STANDARD_30_DAY",
                            onClick = { billingMode = "STANDARD_30_DAY" }
                        )
                        Text("30-Day", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { billingMode = "STANDARD_30_DAY" })
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = billingMode == "ACTUAL_CONSUMPTION",
                            onClick = { billingMode = "ACTUAL_CONSUMPTION" }
                        )
                        Text("Actual", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { billingMode = "ACTUAL_CONSUMPTION" })
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        item {
            Button(
                onClick = {
                    val sReading = startingReading.toDoubleOrNull()
                    val bDay = billingDay.toIntOrNull()
                    if (name.isNotBlank() && sReading != null && bDay != null && bDay in 1..31) {
                        viewModel.addCustomer(
                            name = name,
                            meterNumber = meterNumber,
                            phoneNumber = phoneNumber,
                            address = address,
                            startingReading = sReading,
                            unitPrice = unitPrice.toDoubleOrNull(),
                            demandCharge = demandCharge.toDoubleOrNull(),
                            vatPercent = vatPercent.toDoubleOrNull() ?: 5.0,
                            billingDay = bDay,
                            billingMode = billingMode,
                            notes = notes
                        )
                    }
                },
                enabled = name.isNotBlank() && startingReading.isNotBlank() && billingDay.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_customer_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Customer Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Edit Customer Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCustomerScreen(viewModel: UtilityViewModel, currencySymbol: String) {
    val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No customer selected")
        }
        return
    }

    val cust = customer!!

    var name by remember { mutableStateOf(cust.name) }
    var meterNumber by remember { mutableStateOf(cust.meterNumber) }
    var phoneNumber by remember { mutableStateOf(cust.phoneNumber) }
    var address by remember { mutableStateOf(cust.address) }
    var startingReading by remember { mutableStateOf(cust.startingReading.toString()) }
    var unitPrice by remember { mutableStateOf(cust.unitPrice.toString()) }
    var demandCharge by remember { mutableStateOf(cust.demandCharge.toString()) }
    var vatPercent by remember { mutableStateOf(cust.vatPercent.toString()) }
    var billingDay by remember { mutableStateOf(cust.billingDay.toString()) }
    var billingMode by remember { mutableStateOf(cust.billingMode) }
    var notes by remember { mutableStateOf(cust.notes) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Edit Profile: ${cust.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = meterNumber,
                    onValueChange = { meterNumber = it },
                    label = { Text("Meter # (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone (Optional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        }

        item {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startingReading,
                    onValueChange = { startingReading = it },
                    label = { Text("Start Reading *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = billingDay,
                    onValueChange = { billingDay = it },
                    label = { Text("Billing Day (1-31) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Unit Price ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = demandCharge,
                    onValueChange = { demandCharge = it },
                    label = { Text("Demand ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vatPercent,
                    onValueChange = { vatPercent = it },
                    label = { Text("VAT (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Billing Mode selection
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("Billing Method", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = billingMode == "STANDARD_30_DAY",
                            onClick = { billingMode = "STANDARD_30_DAY" }
                        )
                        Text("30-Day", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { billingMode = "STANDARD_30_DAY" })
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(
                            selected = billingMode == "ACTUAL_CONSUMPTION",
                            onClick = { billingMode = "ACTUAL_CONSUMPTION" }
                        )
                        Text("Actual", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { billingMode = "ACTUAL_CONSUMPTION" })
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        item {
            Button(
                onClick = {
                    val sReading = startingReading.toDoubleOrNull()
                    val bDay = billingDay.toIntOrNull()
                    if (name.isNotBlank() && sReading != null && bDay != null && bDay in 1..31) {
                        val updated = cust.copy(
                            name = name,
                            meterNumber = meterNumber,
                            phoneNumber = phoneNumber,
                            address = address,
                            startingReading = sReading,
                            unitPrice = unitPrice.toDoubleOrNull() ?: cust.unitPrice,
                            demandCharge = demandCharge.toDoubleOrNull() ?: cust.demandCharge,
                            vatPercent = vatPercent.toDoubleOrNull() ?: cust.vatPercent,
                            billingDay = bDay,
                            billingMode = billingMode,
                            notes = notes
                        )
                        viewModel.updateCustomerDetails(updated)
                    }
                },
                enabled = name.isNotBlank() && startingReading.isNotBlank() && billingDay.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Reading Screen (Signature Meter Entry page)
@Composable
fun ReadingScreen(
    viewModel: UtilityViewModel,
    currencySymbol: String,
    unitLabel: String
) {
    val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val rawInput by viewModel.currentReadingInput.collectAsStateWithLifecycle()
    val calculation by viewModel.activeBillCalculation.collectAsStateWithLifecycle()
    val queue by viewModel.readingQueue.collectAsStateWithLifecycle()
    val qIndex by viewModel.activeQueueIndex.collectAsStateWithLifecycle()

    var showAbnormalWarningDialog by remember { mutableStateOf(false) }
    var averageUsage by remember { mutableStateOf<Double?>(null) }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No customer selected for reading.")
        }
        return
    }

    val cust = customer!!
    val currentReadingVal = rawInput.toDoubleOrNull()

    // Fetch previous average usages to trigger abnormal warnings
    LaunchedEffect(cust.id) {
        averageUsage = viewModel.getCustomerAverageUsage(cust.id)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Queue progress indicator
        if (queue.isNotEmpty() && qIndex >= 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reading Queue: Customer ${qIndex + 1} of ${queue.size}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = { viewModel.cancelReadingQueue() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Quit Queue", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = cust.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (cust.meterNumber.isNotEmpty()) {
                        Text("Meter Number: ${cust.meterNumber}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (cust.notes.isNotEmpty()) {
                        Text("Notes: ${cust.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Previous Reading", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cust.currentReading.toInt()} $unitLabel", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Billing Method", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (cust.billingMode == "STANDARD_30_DAY") "Standard 30-Day" else "Actual Consumption",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Editable single input current reading
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Meter Reading",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = rawInput,
                    onValueChange = { viewModel.currentReadingInput.value = it },
                    placeholder = { Text("0", fontSize = 36.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .width(220.dp)
                        .testTag("reading_input_field"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Reading protection and Live Calculations
        if (currentReadingVal != null) {
            if (currentReadingVal < cust.currentReading) {
                // Warning reading protection
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Current reading cannot be lower than the previous reading.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                // Show live calculations
                val calc = calculation
                if (calc != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Monthly Bill Estimation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                                BreakdownItemRow("Consumed Units", "${calc.actualUnits.toInt()} $unitLabel")
                                BreakdownItemRow("Days Elapsed", "${calc.daysPassed} days")

                                if (cust.billingMode == "STANDARD_30_DAY") {
                                    val dailyAvg = if (calc.daysPassed > 0) calc.actualUnits / calc.daysPassed else 0.0
                                    BreakdownItemRow("Daily Avg Usages", String.format(Locale.getDefault(), "%.2f $unitLabel/day", dailyAvg))
                                    BreakdownItemRow("Normalized 30-Day Units", "${calc.billableUnitsRounded} $unitLabel")
                                }

                                BreakdownItemRow("Unit Charge", String.format(Locale.getDefault(), "%s%.2f", currencySymbol, calc.unitCharge))
                                BreakdownItemRow("Demand Charge", String.format(Locale.getDefault(), "%s%.2f", currencySymbol, calc.demandCharge))
                                BreakdownItemRow("VAT (${cust.vatPercent}%)", String.format(Locale.getDefault(), "%s%.2f", currencySymbol, calc.vatAmount))

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("GRAND TOTAL", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        if (calc.daysLate > 0) {
                                            Text("Late by ${calc.daysLate} Days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("No late days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                    }
                                    Text(
                                        text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, calc.grandTotal),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                // Check for Abnormal Usage (differs from average by ±40%)
                                val avg = averageUsage
                                var isAbnormal = false
                                if (avg != null && avg > 0) {
                                    val pctDiff = java.lang.Math.abs(calc.actualUnits - avg) / avg
                                    if (pctDiff > 0.40) {
                                        isAbnormal = true
                                    }
                                }

                                if (isAbnormal) {
                                    showAbnormalWarningDialog = true
                                } else {
                                    viewModel.saveReading(cust.id, currentReadingVal)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("save_reading_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Reading", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Abnormal Usage Warning Dialog
    if (showAbnormalWarningDialog) {
        val calc = calculation
        val avg = averageUsage ?: 0.0
        AlertDialog(
            onDismissRequest = { showAbnormalWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Abnormal Usage Detected!")
                }
            },
            text = {
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "Consumption is much %s than usual.\n\n" +
                        "• Current usage: %.1f %s\n" +
                        "• Previous months average: %.1f %s\n\n" +
                        "Please verify the meter reading physically before confirming.",
                        if ((calc?.actualUnits ?: 0.0) > avg) "HIGHER" else "LOWER",
                        calc?.actualUnits ?: 0.0, unitLabel,
                        avg, unitLabel
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAbnormalWarningDialog = false
                        if (currentReadingVal != null) {
                            viewModel.saveReading(cust.id, currentReadingVal)
                        }
                    }
                ) {
                    Text("Confirm & Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbnormalWarningDialog = false }) {
                    Text("Re-Verify Meter")
                }
            }
        )
    }
}

@Composable
fun BreakdownItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// History Screen ( chronological readings timeline)
@Composable
fun HistoryScreen(
    viewModel: UtilityViewModel,
    currencySymbol: String,
    unitLabel: String
) {
    val readingsList by viewModel.readings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(12.dp))

        // Tool area at top of history
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.historySearchQuery.value = it },
                placeholder = { Text("Search by customer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.historySearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_field"),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Undo last reading button
            IconButton(
                onClick = {
                    viewModel.undoLastReading { success ->
                        if (success) {
                            Toast.makeText(context, "Last reading successfully undone!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No reading to undo.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.errorContainer, shape = CircleShape)
                    .size(44.dp)
                    .testTag("undo_reading_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo reading", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Export PDF / CSV quick bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { ReportExporter.exportToPdf(context, readingsList, currencySymbol) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("export_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PDF Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { ReportExporter.exportToCsv(context, readingsList, currencySymbol) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("export_csv_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CSV Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (readingsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No history records found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(readingsList, key = { it.id }) { reading ->
                    HistoryItemCard(reading, currencySymbol, unitLabel)
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    reading: Reading,
    currencySymbol: String,
    unitLabel: String
) {
    val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(reading.readingDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reading.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, reading.grandTotal),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Date: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Reading Range", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${reading.previousReading.toInt()} ➔ ${reading.currentReading.toInt()} $unitLabel", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Actual Consumed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${reading.actualUnits.toInt()} $unitLabel", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Billable Units", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${reading.billableUnits.toInt()} $unitLabel", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (reading.daysLate > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🚨 Late by ${reading.daysLate} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// System Settings screen
@Composable
fun SettingsScreen(viewModel: UtilityViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var defaultUnitPrice by remember { mutableStateOf(settings.defaultUnitPrice.toString()) }
    var defaultDemandCharge by remember { mutableStateOf(settings.defaultDemandCharge.toString()) }
    var defaultVatPercent by remember { mutableStateOf(settings.defaultVatPercent.toString()) }
    var billingMode by remember { mutableStateOf(settings.billingMode) }
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var utilityType by remember { mutableStateOf(settings.utilityType) }
    var isDarkMode by remember { mutableStateOf(settings.isDarkMode) }

    LaunchedEffect(settings) {
        defaultUnitPrice = settings.defaultUnitPrice.toString()
        defaultDemandCharge = settings.defaultDemandCharge.toString()
        defaultVatPercent = settings.defaultVatPercent.toString()
        billingMode = settings.billingMode
        currencySymbol = settings.currencySymbol
        utilityType = settings.utilityType
        isDarkMode = settings.isDarkMode
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configure global defaults and manage offline database backup/restore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Section: System Branding & Terminology
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Branding & Utility Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    // Utility Type dropdown selector
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(getUtilityIcon(utilityType), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Utility Type: $utilityType")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            listOf("Electricity", "Water", "Gas", "Generator", "Custom").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        utilityType = type
                                        dropdownExpanded = false
                                        viewModel.updateSettings(
                                            defaultUnitPrice.toDoubleOrNull() ?: settings.defaultUnitPrice,
                                            defaultDemandCharge.toDoubleOrNull() ?: settings.defaultDemandCharge,
                                            defaultVatPercent.toDoubleOrNull() ?: settings.defaultVatPercent,
                                            billingMode,
                                            currencySymbol,
                                            type,
                                            isDarkMode
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Currency symbol selector
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = {
                            currencySymbol = it
                            viewModel.updateSettings(
                                defaultUnitPrice.toDoubleOrNull() ?: settings.defaultUnitPrice,
                                defaultDemandCharge.toDoubleOrNull() ?: settings.defaultDemandCharge,
                                defaultVatPercent.toDoubleOrNull() ?: settings.defaultVatPercent,
                                billingMode,
                                it,
                                utilityType,
                                isDarkMode
                            )
                        },
                        label = { Text("Currency Symbol") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Section: Global defaults
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Global Billing Defaults", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    OutlinedTextField(
                        value = defaultUnitPrice,
                        onValueChange = { defaultUnitPrice = it },
                        label = { Text("Default Unit Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = defaultDemandCharge,
                        onValueChange = { defaultDemandCharge = it },
                        label = { Text("Default Demand Charge") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = defaultVatPercent,
                        onValueChange = { defaultVatPercent = it },
                        label = { Text("Default VAT Percentage (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Default Billing Method Selector
                    Column {
                        Text("Default Billing Method", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = billingMode == "STANDARD_30_DAY",
                                onClick = { billingMode = "STANDARD_30_DAY" }
                            )
                            Text("Standard 30-Day", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable { billingMode = "STANDARD_30_DAY" })
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = billingMode == "ACTUAL_CONSUMPTION",
                                onClick = { billingMode = "ACTUAL_CONSUMPTION" }
                            )
                            Text("Actual Consumption", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable { billingMode = "ACTUAL_CONSUMPTION" })
                        }
                    }

                    Button(
                        onClick = {
                            val uPrice = defaultUnitPrice.toDoubleOrNull() ?: settings.defaultUnitPrice
                            val dCharge = defaultDemandCharge.toDoubleOrNull() ?: settings.defaultDemandCharge
                            val vat = defaultVatPercent.toDoubleOrNull() ?: settings.defaultVatPercent
                            viewModel.updateSettings(uPrice, dCharge, vat, billingMode, currencySymbol, utilityType, isDarkMode)
                            Toast.makeText(context, "Defaults successfully updated!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Billing Defaults", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: System Themes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Display Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("System Default", style = MaterialTheme.typography.bodyMedium)
                        RadioButton(
                            selected = isDarkMode == null,
                            onClick = {
                                isDarkMode = null
                                viewModel.updateSettings(
                                    defaultUnitPrice.toDoubleOrNull() ?: settings.defaultUnitPrice,
                                    defaultDemandCharge.toDoubleOrNull() ?: settings.defaultDemandCharge,
                                    defaultVatPercent.toDoubleOrNull() ?: settings.defaultVatPercent,
                                    billingMode,
                                    currencySymbol,
                                    utilityType,
                                    null
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Force Light Mode", style = MaterialTheme.typography.bodyMedium)
                        RadioButton(
                            selected = isDarkMode == false,
                            onClick = {
                                isDarkMode = false
                                viewModel.updateSettings(
                                    defaultUnitPrice.toDoubleOrNull() ?: settings.defaultUnitPrice,
                                    defaultDemandCharge.toDoubleOrNull() ?: settings.defaultDemandCharge,
                                    defaultVatPercent.toDoubleOrNull() ?: settings.defaultVatPercent,
                                    billingMode,
                                    currencySymbol,
                                    utilityType,
                                    false
                                )
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Force Dark Mode", style = MaterialTheme.typography.bodyMedium)
                        RadioButton(
                            selected = isDarkMode == true,
                            onClick = {
                                isDarkMode = true
                                viewModel.updateSettings(
                                    defaultUnitPrice.toDoubleOrNull() ?: settings.defaultUnitPrice,
                                    defaultDemandCharge.toDoubleOrNull() ?: settings.defaultDemandCharge,
                                    defaultVatPercent.toDoubleOrNull() ?: settings.defaultVatPercent,
                                    billingMode,
                                    currencySymbol,
                                    utilityType,
                                    true
                                )
                            }
                        )
                    }
                }
            }
        }

        // Section: Offline Database backup & restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Offline Database Maintenance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("UnitMate operates completely offline. You can safely backup database backups to your clipboard to copy to other devices or files.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                viewModel.generateBackupJsonAsync { backupStr ->
                                    clipboardManager.setText(AnnotatedString(backupStr))
                                    Toast.makeText(context, "Database backup copied to clipboard!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Backup", fontSize = 12.sp)
                        }

                        // Restore Database
                        var showRestoreDialog by remember { mutableStateOf(false) }
                        Button(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore DB", fontSize = 12.sp)
                        }

                        if (showRestoreDialog) {
                            var inputBackupText by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showRestoreDialog = false },
                                title = { Text("Restore Database Backup") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Paste your exported database backup JSON below. This will overwrite all current customer records and reading logs.", style = MaterialTheme.typography.bodySmall)
                                        OutlinedTextField(
                                            value = inputBackupText,
                                            onValueChange = { inputBackupText = it },
                                            placeholder = { Text("Paste JSON here...") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            maxLines = 10
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (inputBackupText.isNotBlank()) {
                                                viewModel.restoreBackupJson(inputBackupText) { success ->
                                                    showRestoreDialog = false
                                                    if (success) {
                                                        Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Error: Invalid backup format.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = inputBackupText.isNotBlank()
                                    ) {
                                        Text("Restore Data")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRestoreDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section: About
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About UnitMate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 1.0.0 (Production)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("UnitMate is a lightweight offline Meter Reading & Bill Calculator designed for landlords, managers, suppliers, and utility operators.\n\nPhilosophy:\nSet up once ➔ Enter one number every month ➔ Everything else happens automatically.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Utility to calculate state badges
@Composable
fun StatusBadge(daysLabel: String) {
    val (bgColor, textColor) = when {
        daysLabel.startsWith("Late") -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        daysLabel.startsWith("Due Today") -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
    }

    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = daysLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

fun getBadgeStatusLabel(expectedDateMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = expectedDateMs - now
    val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

    return when {
        diffDays < 0 -> "Late by ${-diffDays} Days"
        diffDays == 0 -> "Due Today"
        else -> "Due in $diffDays Days"
    }
}
