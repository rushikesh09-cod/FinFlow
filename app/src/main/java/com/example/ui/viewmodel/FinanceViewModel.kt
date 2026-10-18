package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.data.model.MonthlySavingsAnalysis
import com.example.data.model.SavingsGoal
import com.example.data.repository.FinanceRepository
import com.example.domain.SavingsInsightsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val initialMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    private val _selectedMonth = MutableStateFlow(initialMonth)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    val expensesState: StateFlow<UiState<List<Expense>>> = repository.observeExpenses()
        .map<List<Expense>, UiState<List<Expense>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load expenses")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val savingsGoalsState: StateFlow<List<SavingsGoal>> = repository.observeSavingsGoals()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val currentBudgetState: StateFlow<MonthlyBudget?> = _selectedMonth
        .flatMapLatest { ym -> repository.observeMonthlyBudget(ym) }
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val monthlyAnalysisState: StateFlow<MonthlySavingsAnalysis?> = combine(
        _selectedMonth,
        expensesState,
        currentBudgetState
    ) { ym, expState, budget ->
        val expenses = if (expState is UiState.Success) expState.data else emptyList()
        SavingsInsightsEngine.analyze(ym, expenses, budget)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    fun selectMonth(monthYear: String) {
        _selectedMonth.value = monthYear
    }

    fun nextMonth() {
        try {
            val current = YearMonth.parse(_selectedMonth.value)
            _selectedMonth.value = current.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
        } catch (_: Exception) {}
    }

    fun previousMonth() {
        try {
            val current = YearMonth.parse(_selectedMonth.value)
            _selectedMonth.value = current.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
        } catch (_: Exception) {}
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        date: String,
        paymentMethod: String,
        notes: String?,
        isRecurring: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val expense = Expense(
                title = title,
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes,
                isRecurring = isRecurring
            )
            val result = repository.addExpense(expense)
            if (result.isSuccess) {
                _userMessage.value = "Expense added successfully"
                onSuccess()
            } else {
                _userMessage.value = "Failed to add expense: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateExpense(
        expense: Expense,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateExpense(expense)
            if (result.isSuccess) {
                _userMessage.value = "Expense updated"
                onSuccess()
            } else {
                _userMessage.value = "Update failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val result = repository.deleteExpense(expenseId)
            if (result.isSuccess) {
                _userMessage.value = "Expense deleted"
            } else {
                _userMessage.value = "Failed to delete: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun saveMonthlyBudget(monthlyIncome: Double, savingsGoal: Double) {
        viewModelScope.launch {
            val result = repository.setMonthlyBudget(_selectedMonth.value, monthlyIncome, savingsGoal)
            if (result.isSuccess) {
                _userMessage.value = "Monthly budget & savings target saved"
            } else {
                _userMessage.value = "Failed to save budget: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addSavingsGoal(
        name: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String,
        category: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                name = name,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                category = category
            )
            val result = repository.addSavingsGoal(goal)
            if (result.isSuccess) {
                _userMessage.value = "Savings goal added"
                onSuccess()
            } else {
                _userMessage.value = "Failed to create goal: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateGoalProgress(goal: SavingsGoal, newAmount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = newAmount)
            val result = repository.updateSavingsGoal(updated)
            if (result.isSuccess) {
                _userMessage.value = "Goal progress updated"
            }
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            val result = repository.deleteSavingsGoal(goalId)
            if (result.isSuccess) {
                _userMessage.value = "Savings goal removed"
            }
        }
    }

    fun seedStarterData() {
        viewModelScope.launch {
            val currentYm = _selectedMonth.value
            val today = LocalDate.now().toString()
            val sampleIncome = 75000.0
            val sampleSavingsTarget = 20000.0

            repository.setMonthlyBudget(currentYm, sampleIncome, sampleSavingsTarget)

            val samples = listOf(
                Expense(title = "House Rent", amount = 22000.0, category = "Housing", date = "$currentYm-01", paymentMethod = "UPI / GPay", isRecurring = true),
                Expense(title = "Blinkit & BigBasket Groceries", amount = 6500.0, category = "Food & Dining", date = "$currentYm-03", paymentMethod = "UPI / GPay"),
                Expense(title = "Electricity & Piped Gas Bill", amount = 2150.0, category = "Utilities", date = "$currentYm-05", paymentMethod = "UPI / GPay", isRecurring = true),
                Expense(title = "Fuel & Metro Card", amount = 3200.0, category = "Transportation", date = "$currentYm-06", paymentMethod = "UPI / GPay", isRecurring = true),
                Expense(title = "Swiggy & Weekend Dining", amount = 2800.0, category = "Food & Dining", date = "$currentYm-08", paymentMethod = "UPI / GPay"),
                Expense(title = "Netflix & Hotstar Subscriptions", amount = 899.0, category = "Entertainment", date = "$currentYm-10", paymentMethod = "Credit Card", isRecurring = true),
                Expense(title = "Apollo Pharmacy & Meds", amount = 750.0, category = "Healthcare", date = "$currentYm-12", paymentMethod = "UPI / GPay"),
                Expense(title = "Online Upskilling Course", amount = 1499.0, category = "Education", date = "$currentYm-14", paymentMethod = "UPI / GPay")
            )

            samples.forEach { repository.addExpense(it) }

            repository.addSavingsGoal(
                SavingsGoal(name = "Emergency Fund (6 Months)", targetAmount = 300000.0, currentAmount = 145000.0, targetDate = "2027-04-30", category = "Emergency")
            )
            repository.addSavingsGoal(
                SavingsGoal(name = "Goa & Ladakh Holiday Fund", targetAmount = 60000.0, currentAmount = 28500.0, targetDate = "2027-07-15", category = "Travel")
            )

            _userMessage.value = "Sample INR budget & expenses loaded!"
        }
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FinanceViewModel(repository) as T
        }
    }
}
