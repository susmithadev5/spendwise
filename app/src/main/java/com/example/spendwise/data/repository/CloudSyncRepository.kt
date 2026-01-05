package com.example.spendwise.data.repository

import com.example.spendwise.data.local.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class CloudExpense(
    val id: Long? = null,
    val amount: Double? = null,
    val category: String? = null,
    val date: String? = null,
    val note: String? = null,
    val createdAt: Long? = null,
    val timestamp: Long? = null
)

data class CloudBudget(
    val id: Long? = null,
    val month: Int? = null,
    val year: Int? = null,
    val amount: Double? = null
)

sealed class CloudSyncResult {
    data object Success : CloudSyncResult()
    data class Error(val message: String) : CloudSyncResult()
}

interface CloudSyncRepository {
    suspend fun backupAllData(): CloudSyncResult
    suspend fun restoreAllData(overwriteLocal: Boolean = true): CloudSyncResult
}

class CloudSyncRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val firebaseAuth: FirebaseAuth
) : CloudSyncRepository {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun backupAllData(): CloudSyncResult = withContext(Dispatchers.IO) {
        val user = firebaseAuth.currentUser ?: return@withContext CloudSyncResult.Error(
            "User not authenticated"
        )

        return@withContext try {
            val userDoc = firestore.collection("users").document(user.uid)
            val expensesRef = userDoc.collection("expenses")
            val budgetsRef = userDoc.collection("budgets")

            val expenses = expenseRepository.getAllExpenses().first()
            val budget = budgetRepository.getBudget()

            val batch = firestore.batch()

            val existingExpenses = expensesRef.get().await()
            existingExpenses.documents.forEach { batch.delete(it.reference) }

            val existingBudgets = budgetsRef.get().await()
            existingBudgets.documents.forEach { batch.delete(it.reference) }

            expenses.forEach { expense ->
                val doc = expensesRef.document(expense.id.toString())
                val cloudExpense = CloudExpense(
                    id = expense.id.toLong(),
                    amount = expense.amount,
                    category = expense.category,
                    date = formatDate(expense.date),
                    note = expense.note,
                    createdAt = System.currentTimeMillis(),
                    timestamp = expense.date
                )
                batch.set(doc, cloudExpense)
            }

            budget?.let {
                val budgetDocId = if (it.year != null && it.month != null) {
                    "${it.year}_${String.format("%02d", it.month)}"
                } else {
                    "current"
                }
                val cloudBudget = CloudBudget(
                    id = it.id.toLong(),
                    month = it.month,
                    year = it.year,
                    amount = it.amount
                )
                val doc = budgetsRef.document(budgetDocId)
                batch.set(doc, cloudBudget)
            }

            batch.commit().await()
            CloudSyncResult.Success
        } catch (e: Exception) {
            CloudSyncResult.Error(e.localizedMessage ?: "Failed to back up data")
        }
    }

    override suspend fun restoreAllData(overwriteLocal: Boolean): CloudSyncResult =
        withContext(Dispatchers.IO) {
            val user = firebaseAuth.currentUser ?: return@withContext CloudSyncResult.Error(
                "User not authenticated"
            )

            return@withContext try {
                val userDoc = firestore.collection("users").document(user.uid)
                val expensesSnapshot = userDoc.collection("expenses").get().await()
                val budgetsSnapshot = userDoc.collection("budgets").get().await()

                if (overwriteLocal) {
                    expenseRepository.clearAllExpenses()
                    budgetRepository.clearBudget()
                }

                expensesSnapshot.documents.forEach { doc ->
                    val data = doc.toObject(CloudExpense::class.java)
                    val localExpense = data?.toLocalExpense(doc.id)
                    if (localExpense != null) {
                        expenseRepository.insertExpense(localExpense)
                    }
                }

                val budgetDoc = budgetsSnapshot.documents.firstOrNull()
                budgetDoc?.let { doc ->
                    val data = doc.toObject(CloudBudget::class.java)
                    data?.let {
                        budgetRepository.setBudget(
                            amount = it.amount ?: 0.0,
                            month = it.month,
                            year = it.year
                        )
                    }
                }

                CloudSyncResult.Success
            } catch (e: Exception) {
                CloudSyncResult.Error(e.localizedMessage ?: "Failed to restore data")
            }
        }

    private fun CloudExpense.toLocalExpense(documentId: String): Expense {
        val parsedId = (id ?: documentId.toLongOrNull() ?: 0L).toInt()
        val parsedDate = timestamp ?: parseDate(date)
        return Expense(
            id = parsedId,
            amount = amount ?: 0.0,
            category = category.orEmpty(),
            date = parsedDate,
            note = note.orEmpty()
        )
    }

    private fun formatDate(millis: Long): String = dateFormatter.format(Date(millis))

    private fun parseDate(dateString: String?): Long {
        if (dateString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            dateFormatter.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
