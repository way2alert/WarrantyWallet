package com.warrantywallet.app.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.warrantywallet.app.core.model.Product
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ExportUtils {

    fun exportToJson(products: List<Product>): String {
        val root = JSONObject()
        root.put("exportDate", System.currentTimeMillis())
        root.put("totalProducts", products.size)

        val array = JSONArray()
        for (p in products) {
            val item = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("brand", p.brand ?: "")
                put("category", p.category.displayName)
                put("purchaseDate", p.purchaseDateMillis?.let { DateUtils.formatDisplayDate(it) } ?: "")
                put("warrantyStart", p.warrantyStartMillis?.let { DateUtils.formatDisplayDate(it) } ?: "")
                put("warrantyEnd", p.warrantyEndMillis?.let { DateUtils.formatDisplayDate(it) } ?: "")
                put("durationMonths", p.warrantyDurationMonths ?: 0)
                put("seller", p.seller ?: "")
                put("price", p.price ?: 0.0)
                put("currency", p.currency)
                put("serialNumber", p.serialNumber ?: "")
                put("modelNumber", p.modelNumber ?: "")
                put("notes", p.notes ?: "")
            }
            array.put(item)
        }
        root.put("products", array)
        return root.toString(2)
    }

    fun exportToCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("Product Name,Brand,Category,Purchase Date,Warranty Expiry,Duration (Months),Seller,Price,Currency,Serial Number,Model\n")

        for (p in products) {
            sb.append("\"${escapeCsv(p.name)}\",")
            sb.append("\"${escapeCsv(p.brand ?: "")}\",")
            sb.append("\"${escapeCsv(p.category.displayName)}\",")
            sb.append("\"${escapeCsv(p.purchaseDateMillis?.let { DateUtils.formatDisplayDate(it) } ?: "")}\",")
            sb.append("\"${escapeCsv(p.warrantyEndMillis?.let { DateUtils.formatDisplayDate(it) } ?: "")}\",")
            sb.append("${p.warrantyDurationMonths ?: ""},")
            sb.append("\"${escapeCsv(p.seller ?: "")}\",")
            sb.append("${p.price ?: ""},")
            sb.append("\"${p.currency}\",")
            sb.append("\"${escapeCsv(p.serialNumber ?: "")}\",")
            sb.append("\"${escapeCsv(p.modelNumber ?: "")}\"\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    fun shareExportFile(context: Context, content: String, fileName: String, mimeType: String) {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Warranty Wallet Export: $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Export Warranty Data"))
    }
}
