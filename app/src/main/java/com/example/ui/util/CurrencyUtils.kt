package com.example.ui.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val inrFormat: NumberFormat = try {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    } catch (_: Exception) {
        NumberFormat.getCurrencyInstance(Locale.US)
    }

    private val inrCompactFormat: NumberFormat = try {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    } catch (_: Exception) {
        NumberFormat.getCurrencyInstance(Locale.US)
    }

    fun format(amount: Double): String {
        return try {
            inrFormat.format(amount)
        } catch (_: Exception) {
            "₹${String.format(Locale.US, "%,.2f", amount)}"
        }
    }

    fun formatCompact(amount: Double): String {
        return try {
            inrCompactFormat.format(amount)
        } catch (_: Exception) {
            "₹${String.format(Locale.US, "%,.0f", amount)}"
        }
    }
}
