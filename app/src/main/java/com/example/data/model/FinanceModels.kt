package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val date: String = "", // Format: YYYY-MM-DD
    val paymentMethod: String = "UPI / GPay",
    val notes: String? = null,
    val isRecurring: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MonthlyBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthYear: String = "", // Format: YYYY-MM (e.g. 2026-10)
    val monthlyIncome: Double = 0.0,
    val savingsGoal: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: String = "", // Format: YYYY-MM-DD
    val category: String = "Emergency",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CategorySpend(
    val category: String,
    val totalAmount: Double,
    val percentageOfTotal: Double,
    val transactionCount: Int
)

data class MonthlySavingsAnalysis(
    val monthYear: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,
    val savingsRatePercentage: Double,
    val savingsGoal: Double,
    val goalProgressPercentage: Double,
    val topCategories: List<CategorySpend>,
    val needsSpend: Double,
    val wantsSpend: Double,
    val savingsSpend: Double,
    val actionableInsights: List<SavingsInsightItem>
)

data class SavingsInsightItem(
    val title: String,
    val message: String,
    val type: InsightType,
    val metricHighlight: String? = null
)

enum class InsightType {
    SUCCESS,
    WARNING,
    INFO,
    OPPORTUNITY
}

object ExpenseCategories {
    val ALL = listOf(
        "Housing",
        "Food & Dining",
        "Transportation",
        "Utilities",
        "Entertainment",
        "Healthcare",
        "Shopping",
        "Personal Care",
        "Education",
        "Other"
    )

    val PAYMENT_METHODS = listOf(
        "UPI / GPay",
        "Credit Card",
        "Debit Card",
        "Net Banking",
        "Cash"
    )

    fun isNeed(category: String): Boolean {
        return category in listOf("Housing", "Utilities", "Healthcare", "Food & Dining", "Transportation")
    }
}
