package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.logging.Logger

class TransactionManager(
    private val storage: ITransactionStorage,
    private val decorationManager: DecorationManager,
    private val tagGenerator: TagGenerator
) {
    val lastTransaction: Transaction?
        get() = storage.getLastTransaction()

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val fullTransactionsSubject = PublishSubject.create<List<FullTransaction>>()
    private val fullTransactionsWithTagsSubject = PublishSubject.create<List<TransactionWithTags>>()

    val fullTransactionsAsync: Flowable<List<FullTransaction>> = fullTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun getFullTransactionsFlowable(tags: List<List<String>>): Flowable<List<FullTransaction>> {
        return fullTransactionsWithTagsSubject.toFlowable(BackpressureStrategy.BUFFER)
            .map { transactions ->
                transactions.mapNotNull { transactionWithTags ->
                    for (andTags in tags) {
                        if (transactionWithTags.tags.all { !andTags.contains(it) }) {
                            return@mapNotNull null
                        }
                    }
                    return@mapNotNull transactionWithTags.transaction
                }
            }
            .filter { it.isNotEmpty() }
    }

    fun getFullTransactionsAsync(tags: List<List<String>>, fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> =
        storage.getTransactionsBeforeAsync(tags, fromHash, limit)
            .map { transactions ->
                decorationManager.decorateTransactions(transactions)
            }

    fun getPendingFullTransactions(tags: List<List<String>>): List<FullTransaction> =
        decorationManager.decorateTransactions(storage.getPendingTransactions(tags))

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> =
        decorationManager.decorateTransactions(storage.getTransactions(hashes))

    fun handle(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return

        storage.save(transactions)
        val failedTransactions = failPendingTransactions()
        val decoratedTransactions = decorationManager.decorateTransactions(transactions + failedTransactions)

        val transactionWithTags = mutableListOf<TransactionWithTags>()
        val allTags: MutableList<TransactionTag> = mutableListOf()

        decoratedTransactions.forEach { transaction ->
            val tags = tagGenerator.generate(transaction)
            allTags.addAll(tags)
            transactionWithTags.add(TransactionWithTags(transaction, tags.map { it.name }))
        }

        storage.saveTags(allTags)

        fullTransactionsSubject.onNext(decoratedTransactions)
        fullTransactionsWithTagsSubject.onNext(transactionWithTags)
    }

    fun etherTransferTransactionData(address: Address, value: BigInteger): TransactionData {
        return TransactionData(address, value, byteArrayOf())
    }

    private fun failPendingTransactions(): List<Transaction> {
        val pendingTransactions = storage.getPendingTransactions()

        if (pendingTransactions.isEmpty()) return listOf()

        val pendingTransactionNonces = pendingTransactions.mapNotNull { it.nonce }.toSet().toList()
        val nonPendingTransactions = storage.getNonPendingTransactionsByNonces(pendingTransactionNonces)
        val processedTransactions: MutableList<Transaction> = mutableListOf()

        for (nonPendingTransaction in nonPendingTransactions) {
            val duplicateTransactions = pendingTransactions.filter { it.nonce == nonPendingTransaction.nonce }
            for (transaction in duplicateTransactions) {
                transaction.isFailed = true
                transaction.replacedWith = nonPendingTransaction.hash
                processedTransactions.add(transaction)
            }
        }

        storage.save(processedTransactions)
        return processedTransactions
    }

    data class TransactionWithTags(
        val transaction: FullTransaction,
        val tags: List<String>
    )

}
