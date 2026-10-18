package com.warrantywallet.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.warrantywallet.app.core.database.entity.DocumentEntity
import com.warrantywallet.app.core.database.entity.ProductEntity
import com.warrantywallet.app.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAtMillis DESC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY createdAtMillis DESC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductByIdFlow(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE warrantyEndMillis IS NOT NULL AND warrantyEndMillis > :nowMillis AND warrantyEndMillis <= :expiringThresholdMillis ORDER BY warrantyEndMillis ASC")
    fun getExpiringSoonProductsFlow(nowMillis: Long, expiringThresholdMillis: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR seller LIKE '%' || :query || '%' OR modelNumber LIKE '%' || :query || '%' ORDER BY createdAtMillis DESC")
    fun searchProductsFlow(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(products: List<ProductEntity>)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Query("SELECT COUNT(*) FROM products")
    fun getProductCountFlow(): Flow<Int>
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY targetTimeMillis ASC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE productId = :productId")
    fun getRemindersForProductFlow(productId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE productId = :productId")
    suspend fun getRemindersForProduct(productId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isTriggered = 0 AND targetTimeMillis >= :nowMillis ORDER BY targetTimeMillis ASC")
    suspend fun getActivePendingReminders(nowMillis: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM reminders WHERE productId = :productId")
    suspend fun deleteRemindersForProduct(productId: String)

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAtMillis DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE productId = :productId")
    fun getDocumentsForProductFlow(productId: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("DELETE FROM documents WHERE productId = :productId")
    suspend fun deleteDocumentsForProduct(productId: String)

    @Query("DELETE FROM documents")
    suspend fun clearAllDocuments()
}
