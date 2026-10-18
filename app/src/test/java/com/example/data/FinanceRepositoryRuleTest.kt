package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Expense
import com.example.data.repository.FinanceRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addExpense_authenticatedUser_createsDocumentSuccessfully(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore)

        val expense = Expense(
            title = "Test Grocery",
            amount = 45.50,
            category = "Food & Dining",
            date = "2026-10-15",
            paymentMethod = "Credit Card"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addExpense(expense)
        }
        assertTrue(result.isSuccess)
        val docId = result.getOrNull()
        assertNotNull(docId)
        assertTrue(docId!!.isNotEmpty())
    }

    @Test
    fun observeExpenses_authenticatedUser_emitsSavedExpenses(): Unit = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FinanceRepository(firestore)

        val title = "Test Gas ${UUID.randomUUID()}"
        val expense = Expense(
            title = title,
            amount = 35.0,
            category = "Transportation",
            date = "2026-10-16",
            paymentMethod = "Debit Card"
        )

        val addResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addExpense(expense) }
        assertTrue(addResult.isSuccess)

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExpenses().first { expenses ->
                expenses.any { it.title == title }
            }
        }
        assertTrue(list.any { it.title == title })
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceDirectDoc(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceDocId = "alice_doc_${UUID.randomUUID()}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("expenses").document(aliceDocId).set(
                mapOf(
                    "userId" to aliceUid,
                    "title" to "Alice Private Bill",
                    "amount" to 200.0,
                    "category" to "Housing",
                    "date" to "2026-10-01",
                    "paymentMethod" to "Bank Transfer",
                    "isRecurring" to true,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
        }

        // Switch to Bob
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("expenses").document(aliceDocId).get().await()
            }
            fail("Expected PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeExpenses_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repository = FinanceRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeExpenses().first()
            }
            fail("Expected IllegalStateException for unauthenticated user")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("User must be signed in") == true)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@finflow.test"
        const val BOB_EMAIL = "bob@finflow.test"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
