package com.warrantywallet.app.core.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.warrantywallet.app.core.model.Category
import com.warrantywallet.app.core.model.ExtractedBillData
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

object BillOcrParser {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    // Known common retailers and stores (India & Global)
    private val KNOWN_RETAILERS = listOf(
        "Amazon", "Flipkart", "Croma", "Reliance Digital", "Vijay Sales",
        "Apple Store", "Samsung Smart Café", "Poorvika", "Sangeetha",
        "IKEA", "Broma", "Best Buy", "Walmart", "Target"
    )

    // Common electronic and appliance brands
    private val KNOWN_BRANDS = listOf(
        "Samsung", "Apple", "LG", "Sony", "Whirlpool", "Bosch", "OnePlus",
        "Xiaomi", "Dell", "HP", "Lenovo", "Asus", "Acer", "Philips",
        "Bajaj", "Havells", "Voltas", "Daikin", "Panasonic", "Godrej",
        "Prestige", "Pigeon", "Boat", "Noise", "Realme", "Motorola"
    )

    /**
     * Extracts text asynchronously from an image Uri using Google ML Kit On-Device Text Recognition.
     */
    suspend fun parseBillImage(context: Context, imageUri: Uri): ExtractedBillData {
        return suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val extracted = parseRawText(visionText.text)
                        continuation.resume(extracted)
                    }
                    .addOnFailureListener { error ->
                        // Fallback gracefully without crashing
                        continuation.resume(ExtractedBillData())
                    }
            } catch (e: Exception) {
                continuation.resume(ExtractedBillData())
            }
        }
    }

    /**
     * Parses raw extracted lines with regex heuristics to extract product, date, price, seller, and warranty.
     */
    fun parseRawText(rawText: String): ExtractedBillData {
        if (rawText.isBlank()) return ExtractedBillData()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var detectedBrand: String? = null
        var detectedSeller: String? = null
        var detectedProductName: String? = null
        var detectedDateMillis: Long? = null
        var detectedPrice: Double? = null
        var detectedInvoiceNo: String? = null
        var detectedWarrantyMonths: Int? = null

        var confidencePoints = 0

        // 1. Detect Retailer / Seller (Often in the top 5 lines)
        val headerLines = lines.take(6)
        for (line in headerLines) {
            val matchedRetailer = KNOWN_RETAILERS.find { line.contains(it, ignoreCase = true) }
            if (matchedRetailer != null) {
                detectedSeller = matchedRetailer
                confidencePoints += 25
                break
            }
        }
        if (detectedSeller == null && headerLines.isNotEmpty()) {
            // First prominent line that isn't TAX INVOICE or GST
            val candidate = headerLines.firstOrNull {
                !it.contains("INVOICE", ignoreCase = true) &&
                        !it.contains("GST", ignoreCase = true) &&
                        !it.contains("BILL", ignoreCase = true) &&
                        it.length > 3
            }
            if (candidate != null) {
                detectedSeller = candidate
                confidencePoints += 10
            }
        }

        // 2. Detect Brand
        for (line in lines) {
            val matchedBrand = KNOWN_BRANDS.find { line.contains(it, ignoreCase = true) }
            if (matchedBrand != null) {
                detectedBrand = matchedBrand
                confidencePoints += 20
                break
            }
        }

        // 3. Detect Purchase Date
        detectedDateMillis = extractDate(lines)
        if (detectedDateMillis != null) {
            confidencePoints += 25
        }

        // 4. Detect Total Price
        detectedPrice = extractPrice(lines)
        if (detectedPrice != null) {
            confidencePoints += 20
        }

        // 5. Detect Invoice Number
        detectedInvoiceNo = extractInvoiceNumber(lines)

        // 6. Detect Explicit Warranty Mentions (e.g. "1 Year Warranty", "2 Years", "24 Months")
        detectedWarrantyMonths = extractWarrantyDuration(lines)
        if (detectedWarrantyMonths != null) {
            confidencePoints += 10
        }

        // 7. Detect Product Name candidate
        detectedProductName = extractProductNameCandidate(lines, detectedBrand, detectedSeller)

        val finalScore = (confidencePoints / 100f).coerceIn(0.1f, 1.0f)

        return ExtractedBillData(
            productName = detectedProductName,
            brand = detectedBrand,
            seller = detectedSeller,
            purchaseDateMillis = detectedDateMillis,
            price = detectedPrice,
            invoiceNumber = detectedInvoiceNo,
            warrantyMonthsSuggested = detectedWarrantyMonths,
            confidenceScore = finalScore,
            rawExtractedLines = lines
        )
    }

    private fun extractDate(lines: List<String>): Long? {
        val datePatterns = listOf(
            // DD/MM/YYYY or DD-MM-YYYY
            Pattern.compile("""\b(0?[1-9]|[12][0-9]|3[01])[-/.](0?[1-9]|1[012])[-/.](20\d\d)\b"""),
            // YYYY-MM-DD
            Pattern.compile("""\b(20\d\d)[-/.](0?[1-9]|1[012])[-/.](0?[1-9]|[12][0-9]|3[01])\b"""),
            // 15 Oct 2024 or 15-Oct-2024
            Pattern.compile("""\b(0?[1-9]|[12][0-9]|3[01])[\s-](Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[\s-](20\d\d)\b""", Pattern.CASE_INSENSITIVE)
        )

        for (line in lines) {
            if (line.contains("Date", ignoreCase = true) ||
                line.contains("Dt", ignoreCase = true) ||
                line.contains("Invoice", ignoreCase = true) ||
                line.contains("Bill", ignoreCase = true)) {

                for (pattern in datePatterns) {
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        val parsed = parseDateString(matcher.group())
                        if (parsed != null) return parsed
                    }
                }
            }
        }

        // If not found near "Date", search full text
        for (line in lines) {
            for (pattern in datePatterns) {
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val parsed = parseDateString(matcher.group())
                    if (parsed != null) return parsed
                }
            }
        }
        return null
    }

    private fun parseDateString(dateStr: String): Long? {
        val cleaned = dateStr.replace('/', '-').replace('.', '-')
        val formats = listOf(
            DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-M-d", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        )
        for (format in formats) {
            try {
                val localDate = LocalDate.parse(cleaned, format)
                return DateUtils.localDateToMillis(localDate)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun extractPrice(lines: List<String>): Double? {
        // Look for lines containing "Total", "Grand Total", "Net Amount", "Amount Paid"
        val totalKeywords = listOf("GRAND TOTAL", "TOTAL AMOUNT", "TOTAL", "NET AMOUNT", "PAID", "AMOUNT")
        val priceRegex = Pattern.compile("""(?:₹|Rs\.?|INR|\$)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?|[0-9]+(?:\.[0-9]{2})?)""")

        for (line in lines) {
            val upper = line.uppercase()
            if (totalKeywords.any { upper.contains(it) }) {
                val matcher = priceRegex.matcher(line)
                var lastFound: Double? = null
                while (matcher.find()) {
                    val raw = matcher.group(1)?.replace(",", "")
                    val candidate = raw?.toDoubleOrNull()
                    if (candidate != null && candidate > 10.0 && candidate < 5000000.0) {
                        lastFound = candidate
                    }
                }
                if (lastFound != null) return lastFound
            }
        }
        return null
    }

    private fun extractInvoiceNumber(lines: List<String>): String? {
        val invoicePattern = Pattern.compile(
            """(?:INVOICE\s*(?:NO|NUMBER)?|INV\s*NO?|BILL\s*(?:NO|NUMBER)?|REC(?:EIPT)?\s*NO?)[\s#:.-]*([A-Z0-9/-]{5,25})""",
            Pattern.CASE_INSENSITIVE
        )
        for (line in lines) {
            if (line.contains("Date", ignoreCase = true) || line.contains("Dt", ignoreCase = true)) {
                continue
            }
            val matcher = invoicePattern.matcher(line)
            if (matcher.find()) {
                val inv = matcher.group(1)?.trim() ?: continue
                val upper = inv.uppercase()
                if (!upper.contains("INVOICE") && !upper.contains("BILL") && !upper.contains("NUMBER") && !upper.contains("DATE")) {
                    return inv
                }
            }
        }
        return null
    }

    private fun extractWarrantyDuration(lines: List<String>): Int? {
        val durationRegex = Pattern.compile("""(\d+)\s*(YEAR|YEARS|YR|YRS|MONTH|MONTHS|MO|MOS)\s*(?:WARRANTY|GUARANTEE)?""", Pattern.CASE_INSENSITIVE)
        for (line in lines) {
            val matcher = durationRegex.matcher(line)
            if (matcher.find()) {
                val count = matcher.group(1)?.toIntOrNull() ?: continue
                val unit = matcher.group(2)?.uppercase() ?: ""
                return if (unit.startsWith("Y")) {
                    count * 12
                } else {
                    count
                }
            }
        }
        return null
    }

    private fun extractProductNameCandidate(lines: List<String>, brand: String?, seller: String?): String? {
        // Look for a line containing the brand name or line after seller header
        if (brand != null) {
            val brandLine = lines.find { it.contains(brand, ignoreCase = true) && !it.contains("STORE", ignoreCase = true) }
            if (brandLine != null) {
                return brandLine.take(45)
            }
        }

        // Otherwise pick the most informative line that isn't an address, tax, or legal boilerplate
        val skippedKeywords = listOf("GSTIN", "TAX", "INVOICE", "TEL", "PHONE", "ADDRESS", "TERMS", "THANK YOU", "SIGNATURE", "SUBTOTAL")
        for (line in lines.drop(2).take(10)) {
            val upper = line.uppercase()
            if (skippedKeywords.none { upper.contains(it) } && line.length in 6..50) {
                return line
            }
        }
        return brand
    }
}
