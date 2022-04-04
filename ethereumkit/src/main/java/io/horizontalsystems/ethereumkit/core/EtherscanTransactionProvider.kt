package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.ProviderInternalTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTokenTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTransaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single

class EtherscanTransactionProvider(
        private val etherscanService: EtherscanService,
        private val address: Address
): ITransactionProvider {

    override fun getTransactions(startBlock: Long): Single<List<ProviderTransaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map { response ->
                    response.result.distinctBy { it["hash"] }.mapNotNull { tx ->
                        try {
                            val blockNumber = tx.getValue("blockNumber").toLong()
                            val timestamp = tx.getValue("timeStamp").toLong()
                            val hash = tx.getValue("hash").hexStringToByteArray()
                            val nonce = tx.getValue("nonce").toLong()
                            val blockHash = tx["blockHash"]?.hexStringToByteArray()
                            val transactionIndex = tx.getValue("transactionIndex").toInt()
                            val from = Address(tx.getValue("from"))
                            val to = Address(tx.getValue("to"))
                            val value = tx.getValue("value").toBigInteger()
                            val gasLimit = tx.getValue("gas").toLong()
                            val gasPrice = tx.getValue("gasPrice").toLong()
                            val isError = tx["isError"]?.toIntOrNull()
                            val txReceiptStatus = tx["txreceipt_status"]?.toIntOrNull()
                            val input = tx.getValue("input").hexStringToByteArray()
                            val cumulativeGasUsed = tx["cumulativeGasUsed"]?.toLongOrNull()
                            val gasUsed = tx["gasUsed"]?.toLongOrNull()

                            ProviderTransaction(
                                blockNumber, timestamp, hash, nonce, blockHash, transactionIndex, from, to, value, gasLimit, gasPrice,
                                isError, txReceiptStatus, input, cumulativeGasUsed, gasUsed
                            )

                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

    override fun getInternalTransactions(startBlock: Long): Single<List<ProviderInternalTransaction>> {
        return etherscanService.getInternalTransactionList(address, startBlock)
                .map { response ->
                    response.result.mapNotNull { internalTx ->
                        try {
                            val hash = internalTx.getValue("hash").hexStringToByteArray()
                            val blockNumber = internalTx.getValue("blockNumber").toLong()
                            val timestamp = internalTx.getValue("timeStamp").toLong()
                            val from = Address(internalTx.getValue("from"))
                            val to = Address(internalTx.getValue("to"))
                            val value = internalTx.getValue("value").toBigInteger()
                            val traceId = internalTx.getValue("traceId")

                            ProviderInternalTransaction(hash, blockNumber, timestamp, from, to, value, traceId)
                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

    override fun getInternalTransactionsAsync(hash: ByteArray): Single<List<ProviderInternalTransaction>> {
        return etherscanService.getInternalTransactionsAsync(hash)
                .map { response ->
                    response.result.mapNotNull { internalTx ->
                        try {
                            val blockNumber = internalTx.getValue("blockNumber").toLong()
                            val timestamp = internalTx.getValue("timeStamp").toLong()
                            val from = Address(internalTx.getValue("from"))
                            val to = Address(internalTx.getValue("to"))
                            val value = internalTx.getValue("value").toBigInteger()
                            val traceId = internalTx.getValue("traceId")

                            ProviderInternalTransaction(hash, blockNumber, timestamp, from, to, value, traceId)
                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

    override fun getTokenTransactions(startBlock: Long): Single<List<ProviderTokenTransaction>> {
        return etherscanService.getTokenTransactions(address, startBlock)
                .map { response ->
                    response.result.mapNotNull { tx ->
                        try {
                            val blockNumber = tx.getValue("blockNumber").toLong()
                            val timestamp = tx.getValue("timeStamp").toLong()
                            val hash = tx.getValue("hash").hexStringToByteArray()
                            val nonce = tx.getValue("nonce").toLong()
                            val blockHash = tx.getValue("blockHash").hexStringToByteArray()
                            val from = Address(tx.getValue("from"))
                            val contractAddress = Address(tx.getValue("contractAddress"))
                            val to = Address(tx.getValue("to"))
                            val value = tx.getValue("value").toBigInteger()
                            val tokenName = tx.getValue("tokenName")
                            val tokenSymbol = tx.getValue("tokenSymbol")
                            val tokenDecimal = tx.getValue("tokenDecimal").toInt()
                            val transactionIndex = tx.getValue("transactionIndex").toInt()
                            val gasLimit = tx.getValue("gas").toLong()
                            val gasPrice = tx.getValue("gasPrice").toLong()
                            val gasUsed = tx.getValue("gasUsed").toLong()
                            val cumulativeGasUsed = tx.getValue("cumulativeGasUsed").toLong()

                            ProviderTokenTransaction(
                                blockNumber, timestamp, hash, nonce, blockHash, from, contractAddress, to, value, tokenName, tokenSymbol, tokenDecimal,
                                transactionIndex, gasLimit, gasPrice, gasUsed, cumulativeGasUsed
                            )

                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

}
