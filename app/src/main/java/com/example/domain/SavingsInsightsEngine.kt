package com.example.domain

import com.example.data.model.CategorySpend
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategories
import com.example.data.model.InsightType
import com.example.data.model.MonthlyBudget
import com.example.data.model.MonthlySavingsAnalysis
import com.example.data.model.SavingsInsightItem
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

object SavingsInsightsEngine {

    fun analyze(
        monthYear: String,
        expenses: List<Expense>,
        budget: MonthlyBudget?
    ): MonthlySavingsAnalysis {
        val ym = try {
            YearMonth.parse(monthYear)
        } catch (e: Exception) {
            YearMonth.now()
        }

        val monthExpenses = expenses.filter {
            it.date.startsWith(monthYear)
        }

        val totalExpenses = monthExpenses.sumOf { it.amount }
        val totalIncome = budget?.monthlyIncome ?: 0.0
        val targetSavingsGoal = budget?.savingsGoal ?: (totalIncome * 0.20) // default 20%
        val netSavings = totalIncome - totalExpenses
        val savingsRate = if (totalIncome > 0) (netSavings / totalIncome) * 100.0 else 0.0
        val goalProgress = if (targetSavingsGoal > 0) {
            ((netSavings.coerceAtLeast(0.0)) / targetSavingsGoal) * 100.0
        } else 0.0

        // Categories aggregation
        val categoryGroups = monthExpenses.groupBy { it.category }
        val categorySpends = categoryGroups.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val pct = if (totalExpenses > 0) (sum / totalExpenses) * 100.0 else 0.0
            CategorySpend(
                category = cat,
                totalAmount = sum,
                percentageOfTotal = pct,
                transactionCount = list.size
            )
        }.sortedByDescending { it.totalAmount }

        // 50/30/20 breakdown
        var needsSpend = 0.0
        var wantsSpend = 0.0
        monthExpenses.forEach { exp ->
            if (ExpenseCategories.isNeed(exp.category)) {
                needsSpend += exp.amount
            } else {
                wantsSpend += exp.amount
            }
        }
        val savingsSpend = netSavings.coerceAtLeast(0.0)

        // Generate actionable insights
        val insights = mutableListOf<SavingsInsightItem>()

        if (totalIncome > 0) {
            val needsRatio = (needsSpend / totalIncome) * 100.0
            val wantsRatio = (wantsSpend / totalIncome) * 100.0

            if (savingsRate >= 20.0) {
                insights.add(
                    SavingsInsightItem(
                        title = "Strong Savings Rate",
                        message = "You're saving ${String.format(Locale.US, "%.1f", savingsRate)}% of your income this month, exceeding the recommended 20% benchmark!",
                        type = InsightType.SUCCESS,
                        metricHighlight = "+${String.format(Locale.US, "%.1f", savingsRate)}%"
                    )
                )
            } else if (savingsRate > 0) {
                insights.add(
                    SavingsInsightItem(
                        title = "Savings Opportunity",
                        message = "Your current savings rate is ${String.format(Locale.US, "%.1f", savingsRate)}%. Trimming discretionary expenses could help you reach the 20% milestone.",
                        type = InsightType.OPPORTUNITY,
                        metricHighlight = "${String.format(Locale.US, "%.1f", savingsRate)}%"
                    )
                )
            } else {
                insights.add(
                    SavingsInsightItem(
                        title = "Deficit Warning",
                        message = "Expenses exceed income by ₹${String.format(Locale.US, "%,.2f", -netSavings)}. Review non-essential spending to preserve your emergency fund.",
                        type = InsightType.WARNING,
                        metricHighlight = "-₹${String.format(Locale.US, "%,.2f", -netSavings)}"
                    )
                )
            }

            // 50/30/20 rule tips
            if (needsRatio > 55.0) {
                insights.add(
                    SavingsInsightItem(
                        title = "Needs Allocation High",
                        message = "Essential needs consume ${String.format(Locale.US, "%.1f", needsRatio)}% of income (50% target). Consider auditing utility bills or fixed housing costs.",
                        type = InsightType.INFO,
                        metricHighlight = "${String.format(Locale.US, "%.1f", needsRatio)}%"
                    )
                )
            }
            if (wantsRatio > 35.0) {
                insights.add(
                    SavingsInsightItem(
                        title = "Discretionary Spending Alert",
                        message = "Wants (dining, shopping, entertainment) are at ${String.format(Locale.US, "%.1f", wantsRatio)}% of income (target: 30%). Reducing dining out can save thousands.",
                        type = InsightType.WARNING,
                        metricHighlight = "${String.format(Locale.US, "%.1f", wantsRatio)}%"
                    )
                )
            }
        }

        // Top category analysis
        if (categorySpends.isNotEmpty()) {
            val top = categorySpends.first()
            insights.add(
                SavingsInsightItem(
                    title = "Primary Expense: ${top.category}",
                    message = "${top.category} is your largest expense at ₹${String.format(Locale.US, "%,.2f", top.totalAmount)} (${String.format(Locale.US, "%.1f", top.percentageOfTotal)}% of total monthly spend across ${top.transactionCount} transactions).",
                    type = InsightType.INFO,
                    metricHighlight = "₹${String.format(Locale.US, "%,.2f", top.totalAmount)}"
                )
            )
        }

        // Recurring commitments
        val recurringSum = monthExpenses.filter { it.isRecurring }.sumOf { it.amount }
        if (recurringSum > 0) {
            insights.add(
                SavingsInsightItem(
                    title = "Recurring Commitments",
                    message = "Fixed monthly recurring bills & subscriptions total ₹${String.format(Locale.US, "%,.2f", recurringSum)}. Regularly cancel unused subscriptions to preserve liquid savings.",
                    type = InsightType.OPPORTUNITY,
                    metricHighlight = "₹${String.format(Locale.US, "%,.2f", recurringSum)}/mo"
                )
            )
        }

        // Pace and end-of-month projection
        val now = LocalDate.now()
        val isCurrentMonth = ym == YearMonth.from(now)
        if (isCurrentMonth && now.dayOfMonth > 1 && totalIncome > 0) {
            val daysPassed = now.dayOfMonth
            val totalDays = ym.lengthOfMonth()
            val dailyAvg = totalExpenses / daysPassed
            val projectedExpenses = dailyAvg * totalDays
            val projectedSavings = totalIncome - projectedExpenses

            if (projectedSavings >= targetSavingsGoal) {
                insights.add(
                    SavingsInsightItem(
                        title = "Month-End Forecast: On Target",
                        message = "At your current pace of ₹${String.format(Locale.US, "%,.2f", dailyAvg)}/day, you're projected to save ₹${String.format(Locale.US, "%,.2f", projectedSavings)} by end of month.",
                        type = InsightType.SUCCESS,
                        metricHighlight = "₹${String.format(Locale.US, "%,.0f", projectedSavings)}"
                    )
                )
            } else {
                insights.add(
                    SavingsInsightItem(
                        title = "Month-End Forecast: Pacing Gap",
                        message = "Pacing suggests ₹${String.format(Locale.US, "%,.2f", projectedExpenses)} total spending. To hit your ₹${String.format(Locale.US, "%,.0f", targetSavingsGoal)} goal, keep daily spend below ₹${String.format(Locale.US, "%,.2f", ((totalIncome - targetSavingsGoal - totalExpenses) / (totalDays - daysPassed)).coerceAtLeast(0.0))}/day.",
                        type = InsightType.WARNING,
                        metricHighlight = "Pacing"
                    )
                )
            }
        }

        return MonthlySavingsAnalysis(
            monthYear = monthYear,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netSavings = netSavings,
            savingsRatePercentage = savingsRate,
            savingsGoal = targetSavingsGoal,
            goalProgressPercentage = goalProgress,
            topCategories = categorySpends,
            needsSpend = needsSpend,
            wantsSpend = wantsSpend,
            savingsSpend = savingsSpend,
            actionableInsights = insights
        )
    }
}
