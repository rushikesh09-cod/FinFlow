package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.data.model.SavingsGoal
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before performing database operations.")
    }

    // ------------------------------------------------------------------------
    // EXPENSES
    // ------------------------------------------------------------------------

    fun observeExpenses(): Flow<List<Expense>> = flow {
        val uid = requireUserId()
        val path = "expenses"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Expense::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.date }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addExpense(expense: Expense): Result<String> {
        val uid = requireUserId()
        val path = "expenses"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "title" to expense.title.trim(),
                "amount" to expense.amount,
                "category" to expense.category.trim(),
                "date" to expense.date.trim(),
                "paymentMethod" to expense.paymentMethod.trim(),
                "notes" to expense.notes?.trim()?.ifEmpty { null },
                "isRecurring" to expense.isRecurring,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val docRef = db.collection(path).add(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        val uid = requireUserId()
        require(expense.id.isNotBlank()) { "Expense ID must not be blank for update" }
        val docPath = "expenses/${expense.id}"
        return try {
            val payload = mapOf(
                "title" to expense.title.trim(),
                "amount" to expense.amount,
                "category" to expense.category.trim(),
                "date" to expense.date.trim(),
                "paymentMethod" to expense.paymentMethod.trim(),
                "notes" to expense.notes?.trim()?.ifEmpty { null },
                "isRecurring" to expense.isRecurring,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("expenses").document(expense.id).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        requireUserId()
        require(expenseId.isNotBlank()) { "Expense ID must not be blank" }
        val docPath = "expenses/$expenseId"
        return try {
            db.collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // MONTHLY BUDGETS
    // ------------------------------------------------------------------------

    fun observeMonthlyBudget(monthYear: String): Flow<MonthlyBudget?> = flow {
        val uid = requireUserId()
        val path = "monthly_budgets"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    val budgets = snapshot.toObjects(
                        MonthlyBudget::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    budgets.firstOrNull { it.monthYear == monthYear }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setMonthlyBudget(monthYear: String, monthlyIncome: Double, savingsGoal: Double): Result<Unit> {
        val uid = requireUserId()
        val docId = "${uid}_$monthYear"
        val docPath = "monthly_budgets/$docId"
        return try {
            val docRef = db.collection("monthly_budgets").document(docId)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val updatePayload = mapOf(
                    "monthlyIncome" to monthlyIncome,
                    "savingsGoal" to savingsGoal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "monthYear" to monthYear,
                    "monthlyIncome" to monthlyIncome,
                    "savingsGoal" to savingsGoal,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // SAVINGS GOALS
    // ------------------------------------------------------------------------

    fun observeSavingsGoals(): Flow<List<SavingsGoal>> = flow {
        val uid = requireUserId()
        val path = "savings_goals"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavingsGoal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.targetDate }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addSavingsGoal(goal: SavingsGoal): Result<String> {
        val uid = requireUserId()
        val path = "savings_goals"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "name" to goal.name.trim(),
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "targetDate" to goal.targetDate.trim(),
                "category" to goal.category.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection(path).add(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal): Result<Unit> {
        requireUserId()
        require(goal.id.isNotBlank()) { "Goal ID must not be blank" }
        val docPath = "savings_goals/${goal.id}"
        return try {
            val payload = mapOf(
                "name" to goal.name.trim(),
                "targetAmount" to goal.targetAmount,
                "currentAmount" to goal.currentAmount,
                "targetDate" to goal.targetDate.trim(),
                "category" to goal.category.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("savings_goals").document(goal.id).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavingsGoal(goalId: String): Result<Unit> {
        requireUserId()
        require(goalId.isNotBlank()) { "Goal ID must not be blank" }
        val docPath = "savings_goals/$goalId"
        return try {
            db.collection("savings_goals").document(goalId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
