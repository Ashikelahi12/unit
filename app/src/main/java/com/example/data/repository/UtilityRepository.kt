package com.example.data.repository

import com.example.data.db.CustomerDao
import com.example.data.db.ReadingDao
import com.example.data.db.SettingsDao
import com.example.data.model.Customer
import com.example.data.model.Reading
import com.example.data.model.Settings
import com.example.data.util.BillingCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class UtilityRepository(
    private val customerDao: CustomerDao,
    private val readingDao: ReadingDao,
    private val settingsDao: SettingsDao
) {
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allReadings: Flow<List<Reading>> = readingDao.getAllReadings()
    val settingsFlow: Flow<Settings?> = settingsDao.getSettingsFlow()

    suspend fun getCustomerById(id: Int): Customer? {
        return customerDao.getCustomerById(id)
    }

    fun getCustomerFlow(id: Int): Flow<Customer?> {
        return customerDao.getCustomerFlow(id)
    }

    fun getReadingsForCustomer(customerId: Int): Flow<List<Reading>> {
        return readingDao.getReadingsForCustomer(customerId)
    }

    fun searchReadings(query: String): Flow<List<Reading>> {
        if (query.isBlank()) return allReadings
        return readingDao.searchReadings(query)
    }

    suspend fun getOrInitializeSettings(): Settings {
        val existing = settingsDao.getSettings()
        if (existing != null) return existing
        val defaultSettings = Settings()
        settingsDao.saveSettings(defaultSettings)
        return defaultSettings
    }

    suspend fun saveSettings(settings: Settings) {
        settingsDao.saveSettings(settings)
    }

    suspend fun insertCustomer(
        name: String,
        meterNumber: String,
        phoneNumber: String,
        address: String,
        startingReading: Double,
        unitPrice: Double,
        demandCharge: Double,
        vatPercent: Double,
        billingDay: Int,
        billingMode: String,
        notes: String,
        creationDate: Long = System.currentTimeMillis()
    ): Long {
        val firstExpected = BillingCalculator.calculateFirstExpectedDate(billingDay, creationDate)
        val customer = Customer(
            name = name,
            meterNumber = meterNumber,
            phoneNumber = phoneNumber,
            address = address,
            startingReading = startingReading,
            currentReading = startingReading,
            unitPrice = unitPrice,
            demandCharge = demandCharge,
            vatPercent = vatPercent,
            billingDay = billingDay,
            billingMode = billingMode,
            notes = notes,
            nextExpectedDate = firstExpected,
            lastReadingDate = creationDate
        )
        return customerDao.insertCustomer(customer)
    }

    suspend fun duplicateCustomer(customerId: Int): Boolean {
        val customer = customerDao.getCustomerById(customerId) ?: return false
        val duplicated = customer.copy(
            id = 0,
            name = "${customer.name} (Copy)",
            meterNumber = "",
            phoneNumber = "",
            address = "",
            startingReading = customer.currentReading,
            currentReading = customer.currentReading,
            nextExpectedDate = BillingCalculator.calculateFirstExpectedDate(customer.billingDay),
            lastReadingDate = System.currentTimeMillis()
        )
        customerDao.insertCustomer(duplicated)
        return true
    }

    suspend fun undoLastReading(): Boolean {
        val lastReading = readingDao.getLastReading() ?: return false
        val customer = customerDao.getCustomerById(lastReading.customerId) ?: return false

        // Delete the last reading
        readingDao.deleteReadingById(lastReading.id)

        // Find remaining readings for this customer
        val remainingReadings = readingDao.getReadingsForCustomerDirect(customer.id)
        val (restoredReadingValue, restoredReadingDate) = if (remainingReadings.isNotEmpty()) {
            val prev = remainingReadings.first()
            Pair(prev.currentReading, prev.readingDate)
        } else {
            Pair(customer.startingReading, customer.nextExpectedDate - 30 * 24 * 60 * 60 * 1000L)
        }

        val restoredExpected = BillingCalculator.calculateFirstExpectedDate(customer.billingDay, restoredReadingDate)

        val restoredCustomer = customer.copy(
            currentReading = restoredReadingValue,
            lastReadingDate = restoredReadingDate,
            nextExpectedDate = restoredExpected
        )
        customerDao.updateCustomer(restoredCustomer)
        return true
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    suspend fun getReadingsForCustomerDirect(customerId: Int): List<Reading> {
        return readingDao.getReadingsForCustomerDirect(customerId)
    }

    suspend fun deleteCustomer(customerId: Int) {
        readingDao.deleteReadingsForCustomer(customerId)
        customerDao.deleteCustomerById(customerId)
    }

    suspend fun saveReading(
        customerId: Int,
        currentReading: Double,
        readingDate: Long = System.currentTimeMillis()
    ): Boolean {
        val customer = customerDao.getCustomerById(customerId) ?: return false
        val previousReading = customer.currentReading

        // Perform bill calculations
        val billResult = BillingCalculator.calculateBill(
            currentReading = currentReading,
            previousReading = previousReading,
            lastReadingDate = customer.lastReadingDate,
            readingDate = readingDate,
            expectedReadingDate = customer.nextExpectedDate,
            unitPrice = customer.unitPrice,
            demandCharge = customer.demandCharge,
            vatPercent = customer.vatPercent,
            billingMode = customer.billingMode
        )

        // Save reading record
        val reading = Reading(
            customerId = customerId,
            customerName = customer.name,
            readingDate = readingDate,
            previousReading = previousReading,
            currentReading = currentReading,
            actualUnits = billResult.actualUnits,
            billableUnits = billResult.billableUnits,
            unitCharge = billResult.unitCharge,
            demandCharge = billResult.demandCharge,
            vatAmount = billResult.vatAmount,
            grandTotal = billResult.grandTotal,
            daysLate = billResult.daysLate
        )
        readingDao.insertReading(reading)

        // Advance expected billing date and update customer status
        val nextExpected = BillingCalculator.calculateNextExpectedDate(
            currentExpected = customer.nextExpectedDate,
            billingDay = customer.billingDay,
            readingDate = readingDate
        )

        val updatedCustomer = customer.copy(
            currentReading = currentReading,
            lastReadingDate = readingDate,
            nextExpectedDate = nextExpected
        )
        customerDao.updateCustomer(updatedCustomer)
        return true
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    suspend fun backupData(): String {
        val customers = customerDao.getAllCustomersDirect()
        val readings = readingDao.getAllReadingsDirect()
        val builder = StringBuilder()
        builder.append("{\n  \"customers\": [\n")
        customers.forEachIndexed { idx, c ->
            builder.append("    {\n")
            builder.append("      \"id\": ${c.id},\n")
            builder.append("      \"name\": \"${escapeJson(c.name)}\",\n")
            builder.append("      \"meterNumber\": \"${escapeJson(c.meterNumber)}\",\n")
            builder.append("      \"phoneNumber\": \"${escapeJson(c.phoneNumber)}\",\n")
            builder.append("      \"address\": \"${escapeJson(c.address)}\",\n")
            builder.append("      \"startingReading\": ${c.startingReading},\n")
            builder.append("      \"currentReading\": ${c.currentReading},\n")
            builder.append("      \"unitPrice\": ${c.unitPrice},\n")
            builder.append("      \"demandCharge\": ${c.demandCharge},\n")
            builder.append("      \"vatPercent\": ${c.vatPercent},\n")
            builder.append("      \"billingDay\": ${c.billingDay},\n")
            builder.append("      \"billingMode\": \"${c.billingMode}\",\n")
            builder.append("      \"notes\": \"${escapeJson(c.notes)}\",\n")
            builder.append("      \"nextExpectedDate\": ${c.nextExpectedDate},\n")
            builder.append("      \"lastReadingDate\": ${c.lastReadingDate}\n")
            builder.append("    }${if (idx == customers.lastIndex) "" else ","}\n")
        }
        builder.append("  ],\n  \"readings\": [\n")
        readings.forEachIndexed { idx, r ->
            builder.append("    {\n")
            builder.append("      \"customerId\": ${r.customerId},\n")
            builder.append("      \"customerName\": \"${escapeJson(r.customerName)}\",\n")
            builder.append("      \"readingDate\": ${r.readingDate},\n")
            builder.append("      \"previousReading\": ${r.previousReading},\n")
            builder.append("      \"currentReading\": ${r.currentReading},\n")
            builder.append("      \"actualUnits\": ${r.actualUnits},\n")
            builder.append("      \"billableUnits\": ${r.billableUnits},\n")
            builder.append("      \"unitCharge\": ${r.unitCharge},\n")
            builder.append("      \"demandCharge\": ${r.demandCharge},\n")
            builder.append("      \"vatAmount\": ${r.vatAmount},\n")
            builder.append("      \"grandTotal\": ${r.grandTotal},\n")
            builder.append("      \"daysLate\": ${r.daysLate}\n")
            builder.append("    }${if (idx == readings.lastIndex) "" else ","}\n")
        }
        builder.append("  ]\n}")
        return builder.toString()
    }

    suspend fun restoreData(jsonStr: String): Boolean {
        try {
            val root = org.json.JSONObject(jsonStr)

            // Wipe existing data
            customerDao.clearAll()
            readingDao.clearAll()

            val customersArray = root.optJSONArray("customers")
            val customerIdMap = mutableMapOf<Int, Int>()
            if (customersArray != null) {
                for (i in 0 until customersArray.length()) {
                    val obj = customersArray.getJSONObject(i)
                    val oldId = obj.optInt("id", i)
                    val customer = Customer(
                        name = obj.getString("name"),
                        meterNumber = obj.optString("meterNumber", ""),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        address = obj.optString("address", ""),
                        startingReading = obj.getDouble("startingReading"),
                        currentReading = obj.getDouble("currentReading"),
                        unitPrice = obj.getDouble("unitPrice"),
                        demandCharge = obj.getDouble("demandCharge"),
                        vatPercent = obj.getDouble("vatPercent"),
                        billingDay = obj.getInt("billingDay"),
                        billingMode = obj.optString("billingMode", "STANDARD_30_DAY"),
                        notes = obj.optString("notes", ""),
                        nextExpectedDate = obj.getLong("nextExpectedDate"),
                        lastReadingDate = obj.getLong("lastReadingDate")
                    )
                    val newId = customerDao.insertCustomer(customer).toInt()
                    customerIdMap[oldId] = newId
                }
            }

            val readingsArray = root.optJSONArray("readings")
            if (readingsArray != null) {
                for (i in 0 until readingsArray.length()) {
                    val obj = readingsArray.getJSONObject(i)
                    val oldCustId = obj.getInt("customerId")
                    val newCustId = customerIdMap[oldCustId] ?: oldCustId
                    val reading = Reading(
                        customerId = newCustId,
                        customerName = obj.getString("customerName"),
                        readingDate = obj.getLong("readingDate"),
                        previousReading = obj.getDouble("previousReading"),
                        currentReading = obj.getDouble("currentReading"),
                        actualUnits = obj.getDouble("actualUnits"),
                        billableUnits = obj.getDouble("billableUnits"),
                        unitCharge = obj.getDouble("unitCharge"),
                        demandCharge = obj.getDouble("demandCharge"),
                        vatAmount = obj.getDouble("vatAmount"),
                        grandTotal = obj.getDouble("grandTotal"),
                        daysLate = obj.getInt("daysLate")
                    )
                    readingDao.insertReading(reading)
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun clearAllData() {
        customerDao.clearAll()
        readingDao.clearAll()
    }

    suspend fun insertCustomerDirect(customer: Customer) {
        customerDao.insertCustomer(customer)
    }

    suspend fun insertReadingDirect(reading: Reading) {
        readingDao.insertReading(reading)
    }
}
