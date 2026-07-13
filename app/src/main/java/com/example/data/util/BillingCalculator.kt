package com.example.data.util

import java.util.Calendar
import kotlin.math.roundToInt

object BillingCalculator {
    fun calculateDaysBetween(startMs: Long, endMs: Long): Int {
        if (endMs <= startMs) return 1
        val diffMs = endMs - startMs
        val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        return maxOf(1, days)
    }

    fun calculateDaysLate(expectedDate: Long, readingDate: Long): Int {
        if (readingDate <= expectedDate) return 0
        val diffMs = readingDate - expectedDate
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }

    fun calculateBillableUnits(
        currentReading: Double,
        previousReading: Double,
        daysPassed: Int,
        billingMode: String // "STANDARD_30_DAY" or "ACTUAL_CONSUMPTION"
    ): Double {
        val actualUnits = maxOf(0.0, currentReading - previousReading)
        if (billingMode == "ACTUAL_CONSUMPTION") {
            return actualUnits
        } else {
            // STANDARD_30_DAY
            val dailyAverage = actualUnits / maxOf(1, daysPassed)
            val billableUnits = dailyAverage * 30.0
            return (billableUnits * 100).roundToInt() / 100.0 // keep 2 decimals
        }
    }

    fun roundBillableUnits(units: Double): Int {
        return units.roundToInt()
    }

    data class BillResult(
        val previousReading: Double,
        val currentReading: Double,
        val actualUnits: Double,
        val billableUnits: Double,
        val billableUnitsRounded: Int,
        val unitCharge: Double,
        val demandCharge: Double,
        val vatAmount: Double,
        val grandTotal: Double,
        val daysPassed: Int,
        val daysLate: Int
    )

    fun calculateBill(
        currentReading: Double,
        previousReading: Double,
        lastReadingDate: Long,
        readingDate: Long,
        expectedReadingDate: Long,
        unitPrice: Double,
        demandCharge: Double,
        vatPercent: Double,
        billingMode: String
    ): BillResult {
        val actualUnits = maxOf(0.0, currentReading - previousReading)
        val daysPassed = calculateDaysBetween(lastReadingDate, readingDate)
        val daysLate = calculateDaysLate(expectedReadingDate, readingDate)

        val billableUnitsRaw = calculateBillableUnits(currentReading, previousReading, daysPassed, billingMode)
        val billableUnitsRounded = roundBillableUnits(billableUnitsRaw)

        val unitCharge = billableUnitsRounded * unitPrice
        val vatAmount = (unitCharge + demandCharge) * (vatPercent / 100.0)
        val grandTotal = unitCharge + demandCharge + vatAmount

        return BillResult(
            previousReading = previousReading,
            currentReading = currentReading,
            actualUnits = actualUnits,
            billableUnits = billableUnitsRaw,
            billableUnitsRounded = billableUnitsRounded,
            unitCharge = unitCharge,
            demandCharge = demandCharge,
            vatAmount = vatAmount,
            grandTotal = grandTotal,
            daysPassed = daysPassed,
            daysLate = daysLate
        )
    }

    fun calculateNextExpectedDate(currentExpected: Long, billingDay: Int, readingDate: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentExpected }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        while (cal.timeInMillis <= readingDate) {
            cal.add(Calendar.MONTH, 1)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, minOf(billingDay, maxDay))
        }
        return cal.timeInMillis
    }

    fun calculateFirstExpectedDate(billingDay: Int, baseDateMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = baseDateMs }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        if (currentDay > billingDay) {
            cal.add(Calendar.MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, minOf(billingDay, maxDay))
        return cal.timeInMillis
    }
}
