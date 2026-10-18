package com.warrantywallet.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.model.SyncStatus
import kotlinx.coroutines.tasks.await

class FirestoreSyncService {

    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (_: Exception) {
        null
    }

    suspend fun syncProductToCloud(userId: String, product: Product): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection("users")
                .document(userId)
                .collection("products")
                .document(product.id)

            val payload = hashMapOf(
                "id" to product.id,
                "name" to product.name,
                "brand" to (product.brand ?: ""),
                "categoryId" to product.category.id,
                "purchaseDateMillis" to (product.purchaseDateMillis ?: 0L),
                "warrantyStartMillis" to (product.warrantyStartMillis ?: 0L),
                "warrantyEndMillis" to (product.warrantyEndMillis ?: 0L),
                "warrantyDurationMonths" to (product.warrantyDurationMonths ?: 0),
                "seller" to (product.seller ?: ""),
                "price" to (product.price ?: 0.0),
                "currency" to product.currency,
                "serialNumber" to (product.serialNumber ?: ""),
                "modelNumber" to (product.modelNumber ?: ""),
                "invoiceCloudUrl" to (product.invoiceCloudUrl ?: ""),
                "notes" to (product.notes ?: ""),
                "updatedAtMillis" to product.updatedAtMillis
            )

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProductFromCloud(userId: String, productId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("users")
                .document(userId)
                .collection("products")
                .document(productId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProductsFromCloud(userId: String): Result<List<Product>> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("products")
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val brand = doc.getString("brand")
                val categoryId = doc.getString("categoryId")
                val purchaseDateMillis = doc.getLong("purchaseDateMillis")?.takeIf { it > 0 }
                val warrantyStartMillis = doc.getLong("warrantyStartMillis")?.takeIf { it > 0 }
                val warrantyEndMillis = doc.getLong("warrantyEndMillis")?.takeIf { it > 0 }
                val warrantyDurationMonths = doc.getLong("warrantyDurationMonths")?.toInt()?.takeIf { it > 0 }
                val seller = doc.getString("seller")
                val price = doc.getDouble("price")
                val currency = doc.getString("currency") ?: "INR"
                val serialNumber = doc.getString("serialNumber")
                val modelNumber = doc.getString("modelNumber")
                val invoiceCloudUrl = doc.getString("invoiceCloudUrl")
                val notes = doc.getString("notes")
                val updatedAtMillis = doc.getLong("updatedAtMillis") ?: System.currentTimeMillis()

                Product(
                    id = id,
                    name = name,
                    brand = brand,
                    category = Category.fromId(categoryId),
                    purchaseDateMillis = purchaseDateMillis,
                    warrantyStartMillis = warrantyStartMillis,
                    warrantyEndMillis = warrantyEndMillis,
                    warrantyDurationMonths = warrantyDurationMonths,
                    seller = seller,
                    price = price,
                    currency = currency,
                    serialNumber = serialNumber,
                    modelNumber = modelNumber,
                    invoiceCloudUrl = invoiceCloudUrl,
                    notes = notes,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAtMillis = updatedAtMillis
                )
            }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
