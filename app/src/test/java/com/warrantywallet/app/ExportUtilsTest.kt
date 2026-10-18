package com.warrantywallet.app

import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.Product
import com.warrantywallet.app.core.util.DateUtils
import com.warrantywallet.app.core.util.ExportUtils
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExportUtilsTest {

    @Test
    fun testExportToJson() {
        val dateMillis = DateUtils.localDateToMillis(LocalDate.of(2024, 5, 20))
        val product = Product(
            id = "test-1",
            name = "Sony Bravia 65",
            brand = "Sony",
            category = Category.ELECTRONICS,
            purchaseDateMillis = dateMillis,
            warrantyEndMillis = DateUtils.calculateWarrantyEndDate(dateMillis, 24),
            warrantyDurationMonths = 24,
            seller = "Reliance Digital",
            price = 85000.0,
            currency = "INR",
            serialNumber = "SN-998811"
        )

        val jsonStr = ExportUtils.exportToJson(listOf(product))
        val json = JSONObject(jsonStr)

        assertEquals(1, json.getInt("totalProducts"))
        val array = json.getJSONArray("products")
        val item = array.getJSONObject(0)
        assertEquals("Sony Bravia 65", item.getString("name"))
        assertEquals("Sony", item.getString("brand"))
        assertEquals(85000.0, item.getDouble("price"), 0.01)
    }

    @Test
    fun testExportToCsv() {
        val product = Product(
            id = "test-2",
            name = "Whirlpool Refrigerator, Double Door",
            brand = "Whirlpool",
            category = Category.APPLIANCES,
            price = 32000.0
        )

        val csv = ExportUtils.exportToCsv(listOf(product))
        assertTrue(csv.contains("Product Name,Brand,Category"))
        // Checks CSV comma escaping
        assertTrue(csv.contains("\"Whirlpool Refrigerator, Double Door\""))
    }
}
