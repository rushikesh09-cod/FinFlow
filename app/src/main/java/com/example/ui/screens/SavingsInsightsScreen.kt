package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategorySpend
import com.example.data.model.InsightType
import com.example.data.model.MonthlySavingsAnalysis
import com.example.data.model.SavingsInsightItem
import com.example.ui.components.EditBudgetDialog
import com.example.ui.theme.getCategoryColor
import com.example.ui.util.CurrencyUtils
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun SavingsInsightsScreen(
    viewModel: FinanceViewModel
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val analysis by viewModel.monthlyAnalysisState.collectAsStateWithLifecycle()

    var showEditBudgetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("savings_insights_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Savings Insights",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monthly Financial Intelligence for $selectedMonth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showEditBudgetDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Budget")
                }
            }
        }

        if (analysis == null || (analysis!!.totalIncome == 0.0 && analysis!!.totalExpenses == 0.0)) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Set your income to unlock smart insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter your expected monthly income and savings target to generate 50/30/20 health checks, run-rate pacing, and categorical insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showEditBudgetDialog = true }) {
                            Text("Set Monthly Budget Now")
                        }
                    }
                }
            }
        } else {
            val a = analysis!!

            // Overview Header Card
            item {
                InsightsOverviewCard(analysis = a)
            }

            // 50/30/20 Rule Analysis Section
            item {
                Rule503020Card(analysis = a)
            }

            // Actionable Insights List
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(a.actionableInsights) { insight ->
                ActionableInsightCard(insight = insight)
            }

            // Category Spend Distribution Table
            item {
                CategoryBreakdownTable(categories = a.topCategories, totalSpend = a.totalExpenses)
            }
        }
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentIncome = analysis?.totalIncome ?: 0.0,
            currentGoal = analysis?.savingsGoal ?: 0.0,
            onDismiss = { showEditBudgetDialog = false },
            onConfirm = { inc, goal ->
                viewModel.saveMonthlyBudget(inc, goal)
                showEditBudgetDialog = false
            }
        )
    }
}

@Composable
private fun InsightsOverviewCard(analysis: MonthlySavingsAnalysis) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Monthly Financial Snapshot",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = CurrencyUtils.format(analysis.netSavings),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Net Savings Accumulated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", analysis.savingsRatePercentage)}% Rate",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SnapshotPill(
                    label = "Income",
                    value = CurrencyUtils.formatCompact(analysis.totalIncome),
                    modifier = Modifier.weight(1f)
                )
                SnapshotPill(
                    label = "Expenses",
                    value = CurrencyUtils.formatCompact(analysis.totalExpenses),
                    modifier = Modifier.weight(1f)
                )
                SnapshotPill(
                    label = "Target",
                    value = CurrencyUtils.formatCompact(analysis.savingsGoal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SnapshotPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Rule503020Card(analysis: MonthlySavingsAnalysis) {
    val income = analysis.totalIncome
    val needsPct = if (income > 0) ((analysis.needsSpend / income) * 100) else 0.0
    val wantsPct = if (income > 0) ((analysis.wantsSpend / income) * 100) else 0.0
    val savingsPct = if (income > 0) ((analysis.savingsSpend / income) * 100) else 0.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "50 / 30 / 20 Budget Framework",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stacked progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (needsPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(needsPct.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Color(0xFF3B82F6))
                    )
                }
                if (wantsPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(wantsPct.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Color(0xFFF59E0B))
                    )
                }
                if (savingsPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(savingsPct.toFloat().coerceAtLeast(0.01f))
                            .fillMaxSize()
                            .background(Color(0xFF10B981))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            RatioRow(
                name = "Needs (Essential)",
                amount = analysis.needsSpend,
                actualPct = needsPct,
                targetPct = 50.0,
                color = Color(0xFF3B82F6),
                status = if (needsPct <= 50.0) "On Target" else "Over Target (+${String.format(Locale.US, "%.0f", needsPct - 50)}%)"
            )
            Spacer(modifier = Modifier.height(8.dp))
            RatioRow(
                name = "Wants (Discretionary)",
                amount = analysis.wantsSpend,
                actualPct = wantsPct,
                targetPct = 30.0,
                color = Color(0xFFF59E0B),
                status = if (wantsPct <= 30.0) "On Target" else "Over Target (+${String.format(Locale.US, "%.0f", wantsPct - 30)}%)"
            )
            Spacer(modifier = Modifier.height(8.dp))
            RatioRow(
                name = "Savings & Investments",
                amount = analysis.savingsSpend,
                actualPct = savingsPct,
                targetPct = 20.0,
                color = Color(0xFF10B981),
                status = if (savingsPct >= 20.0) "Achieved" else "Gap (${String.format(Locale.US, "%.0f", 20 - savingsPct)}%)"
            )
        }
    }
}

@Composable
private fun RatioRow(
    name: String,
    amount: Double,
    actualPct: Double,
    targetPct: Double,
    color: Color,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "$${String.format(Locale.US, "%,.2f", amount)} • Target: ${targetPct.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (status.startsWith("On") || status.startsWith("Achieved")) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
        ) {
            Text(
                text = "${String.format(Locale.US, "%.0f", actualPct)}% ($status)",
                color = if (status.startsWith("On") || status.startsWith("Achieved")) Color(0xFF047857) else Color(0xFFB91C1C),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ActionableInsightCard(insight: SavingsInsightItem) {
    val (icon, bgCol, iconCol) = when (insight.type) {
        InsightType.SUCCESS -> Triple(Icons.Default.CheckCircle, Color(0xFFD1FAE5), Color(0xFF047857))
        InsightType.WARNING -> Triple(Icons.Default.Warning, Color(0xFFFEF3C7), Color(0xFFB45309))
        InsightType.OPPORTUNITY -> Triple(Icons.Default.Lightbulb, Color(0xFFE0E7FF), Color(0xFF4338CA))
        InsightType.INFO -> Triple(Icons.Default.Info, Color(0xFFE2E8F0), Color(0xFF334155))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bgCol,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconCol, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (insight.metricHighlight != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = insight.metricHighlight,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownTable(
    categories: List<CategorySpend>,
    totalSpend: Double
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Detailed Spending Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track high-volume expense leakage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            categories.forEach { cat ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getCategoryColor(cat.category))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat.category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${cat.transactionCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", cat.totalAmount)} (${String.format(Locale.US, "%.1f", cat.percentageOfTotal)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { (cat.percentageOfTotal / 100f).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = getCategoryColor(cat.category),
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}
