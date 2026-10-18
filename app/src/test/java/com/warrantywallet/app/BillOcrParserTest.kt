package com.warrantywallet.app

import com.warrantywallet.app.core.util.BillOcrParser
import com.warrantywallet.app.core.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillOcrParserTest {

    @Test
    fun testParseCromaReceiptSample() {
        val sampleInvoice = """
            CROMA ELECTRONICS STORE
            TAX INVOICE / CASH MEMO
            GSTIN: 27AAACT2727Q1ZB
            Invoice No: CR-MUM-2024-98432
            Date: 15/10/2024
            
            Customer: Rahul Sharma
            Item Description:
            Samsung 55 Inch 4K Smart OLED TV
            Model: QA55S90CAKLXL
            Serial: 094892018492
            
            Warranty: 2 Years Comprehensive Warranty
            
            Qty: 1
            Price: 89,990.00
            CGST 9%: 8,099.10
            SGST 9%: 8,099.10
            GRAND TOTAL: ₹106,188.20
            
            Thank You for Shopping with Croma!
        """.trimIndent()

        val result = BillOcrParser.parseRawText(sampleInvoice)

        assertEquals("Croma", result.seller)
        assertEquals("Samsung", result.brand)
        assertNotNull(result.purchaseDateMillis)
        val parsedDate = DateUtils.millisToLocalDate(result.purchaseDateMillis!!)
        assertEquals(LocalDate.of(2024, 10, 15), parsedDate)

        assertEquals(24, result.warrantyMonthsSuggested)
        assertNotNull(result.price)
        assertTrue(result.price!! > 100000.0)
        assertEquals("CR-MUM-2024-98432", result.invoiceNumber)
        assertTrue(result.confidenceScore >= 0.7f)
    }

    @Test
    fun testParseAmazonInvoiceSample() {
        val sampleAmazon = """
            Amazon.in Retail India
            Tax Invoice
            Order Date: 2024-08-22
            Invoice Date: 2024-08-22
            Invoice Number: IN-92841029
            
            Item:
            Apple MacBook Air M3 16GB 512GB SSD Space Grey
            Warranty: 1 Year Manufacturer Warranty
            
            TOTAL AMOUNT: Rs. 124900.00
            Authorized Signatory
        """.trimIndent()

        val result = BillOcrParser.parseRawText(sampleAmazon)

        assertEquals("Amazon", result.seller)
        assertEquals("Apple", result.brand)
        assertNotNull(result.purchaseDateMillis)
        assertEquals(LocalDate.of(2024, 8, 22), DateUtils.millisToLocalDate(result.purchaseDateMillis!!))
        assertEquals(12, result.warrantyMonthsSuggested)
        assertEquals("IN-92841029", result.invoiceNumber)
    }

    @Test
    fun testEmptyOrUnrelatedTextReturnsGracefully() {
        val emptyResult = BillOcrParser.parseRawText("")
        assertEquals(0.0f, emptyResult.confidenceScore, 0.01f)

        val gibberish = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
        val gibberishResult = BillOcrParser.parseRawText(gibberish)
        assertTrue(gibberishResult.confidenceScore <= 0.2f)
    }
}
