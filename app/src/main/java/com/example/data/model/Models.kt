package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val meterNumber: String,
    val phoneNumber: String = "",
    val address: String = "",
    val startingReading: Double,
    val currentReading: Double,
    val unitPrice: Double,
    val demandCharge: Double,
    val vatPercent: Double,
    val billingDay: Int,
    val billingMode: String = "STANDARD_30_DAY", // "STANDARD_30_DAY" or "ACTUAL_CONSUMPTION"
    val notes: String = "",
    val nextExpectedDate: Long, // Epoch milliseconds
    val lastReadingDate: Long // Epoch milliseconds of last reading or creation
)

@Entity(tableName = "readings")
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val customerName: String, // Denormalized for easy search/reporting
    val readingDate: Long, // Epoch milliseconds
    val previousReading: Double,
    val currentReading: Double,
    val actualUnits: Double,
    val billableUnits: Double,
    val unitCharge: Double,
    val demandCharge: Double,
    val vatAmount: Double,
    val grandTotal: Double,
    val daysLate: Int
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val defaultUnitPrice: Double = 5.0,
    val defaultDemandCharge: Double = 50.0,
    val defaultVatPercent: Double = 5.0,
    val billingMode: String = "STANDARD_30_DAY", // "STANDARD_30_DAY" or "ACTUAL_CONSUMPTION"
    val currencySymbol: String = "$",
    val utilityType: String = "Electricity", // "Electricity", "Water", "Gas", "Generator", "Custom"
    val isDarkMode: Boolean? = null // null for system default
)
