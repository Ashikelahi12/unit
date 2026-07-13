package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.Reading
import com.example.data.model.Settings
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersDirect(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerFlow(id: Int): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Int)

    @Query("DELETE FROM customers")
    suspend fun clearAll()
}

@Dao
interface ReadingDao {
    @Query("SELECT * FROM readings ORDER BY readingDate DESC")
    fun getAllReadings(): Flow<List<Reading>>

    @Query("SELECT * FROM readings ORDER BY readingDate DESC")
    suspend fun getAllReadingsDirect(): List<Reading>

    @Query("SELECT * FROM readings WHERE customerId = :customerId ORDER BY readingDate DESC")
    fun getReadingsForCustomer(customerId: Int): Flow<List<Reading>>

    @Query("SELECT * FROM readings WHERE customerId = :customerId ORDER BY readingDate DESC")
    suspend fun getReadingsForCustomerDirect(customerId: Int): List<Reading>

    @Query("SELECT * FROM readings WHERE customerName LIKE '%' || :query || '%' ORDER BY readingDate DESC")
    fun searchReadings(query: String): Flow<List<Reading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: Reading): Long

    @Query("SELECT * FROM readings ORDER BY id DESC LIMIT 1")
    suspend fun getLastReading(): Reading?

    @Query("DELETE FROM readings WHERE id = :id")
    suspend fun deleteReadingById(id: Int)

    @Query("DELETE FROM readings WHERE customerId = :customerId")
    suspend fun deleteReadingsForCustomer(customerId: Int)

    @Query("DELETE FROM readings")
    suspend fun clearAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingsFlow(): Flow<Settings?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: Settings)
}

@Database(
    entities = [Customer::class, Reading::class, Settings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun readingDao(): ReadingDao
    abstract fun settingsDao(): SettingsDao
}
