package id.co.arista.nativeapp

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
    @PrimaryKey val id: String,
    val customer: String,
    val itemsJson: String,
    val total: Double,
    val updatedAt: Long
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)

@Entity(tableName = "cashier_shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val openingCash: Double,
    val endedAt: Long? = null,
    val closingCash: Double? = null,
    val difference: Double? = null
)

@Dao
interface AristaLocalDao {
    @Query("SELECT * FROM pending_orders ORDER BY updatedAt DESC")
    suspend fun getPendingOrders(): List<PendingOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePendingOrder(order: PendingOrderEntity)

    @Query("DELETE FROM pending_orders WHERE id = :id")
    suspend fun deletePendingOrder(id: String)

    @Query("DELETE FROM pending_orders")
    suspend fun clearPendingOrders()

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
    suspend fun getOutbox(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun removeFromOutbox(id: String)

    @Query("DELETE FROM sync_outbox")
    suspend fun clearOutbox()

    @Query("UPDATE sync_outbox SET retryCount = retryCount + 1, lastError = :message WHERE id = :id")
    suspend fun markRetry(id: String, message: String)

    @Query("SELECT * FROM cashier_shifts ORDER BY startedAt DESC")
    suspend fun getShifts(): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveShift(shift: ShiftEntity)

    @Query("DELETE FROM cashier_shifts")
    suspend fun clearShifts()
}

@Database(
    entities = [PendingOrderEntity::class, SyncOutboxEntity::class, ShiftEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AristaDatabase : RoomDatabase() {
    abstract fun localDao(): AristaLocalDao
}
