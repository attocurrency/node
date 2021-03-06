package org.atto.node.transaction

import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.atto.commons.*
import org.atto.node.PropertyHolder
import org.atto.node.Waiter.waitUntilNonNull
import org.atto.node.account.AccountRepository
import org.atto.node.network.InboundNetworkMessage
import org.atto.node.network.NetworkMessagePublisher
import org.atto.protocol.AttoNode
import org.atto.protocol.transaction.AttoTransactionPush
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TransactionStepDefinition(
    private val thisNode: AttoNode,
    private val messagePublisher: NetworkMessagePublisher,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    private val defaultSendAmount = AttoAmount(4_500_000_000_000_000_000u)

    @When("^send transaction (\\w+) from (\\w+) account to (\\w+) account$")
    fun sendTransaction(transactionShortId: String, shortId: String, receiverShortId: String) = runTest {
        val privateKey = PropertyHolder.get(AttoPrivateKey::class.java, shortId)
        val publicKey = PropertyHolder.get(AttoPublicKey::class.java, shortId)
        val account = accountRepository.findByPublicKey(publicKey)!!

        val receiverPublicKey = PropertyHolder.get(AttoPublicKey::class.java, receiverShortId)

        val sendBlock = account.toAttoAccount().send(receiverPublicKey, defaultSendAmount)
        val sendTransaction = Transaction(
            block = sendBlock,
            signature = privateKey.sign(sendBlock.hash.value),
            work = AttoWork.Companion.work(account.lastHash, thisNode.network)
        )
        messagePublisher.publish(
            InboundNetworkMessage(
                thisNode.socketAddress,
                AttoTransactionPush(sendTransaction.toAttoTransaction())
            )
        )

        PropertyHolder.add(transactionShortId, sendTransaction)
    }

    @When("^change transaction (\\w+) from (\\w+) account to (\\w+) representative$")
    fun changeTransaction(transactionShortId: String, shortId: String, representativeShortId: String) = runTest {
        val privateKey = PropertyHolder.get(AttoPrivateKey::class.java, shortId)
        val publicKey = PropertyHolder.get(AttoPublicKey::class.java, shortId)
        val account = accountRepository.findByPublicKey(publicKey)!!

        val representative = PropertyHolder.get(AttoPublicKey::class.java, representativeShortId)

        val changeBlock = account.toAttoAccount().change(representative)
        val changeTransaction = Transaction(
            block = changeBlock,
            signature = privateKey.sign(changeBlock.hash.value),
            work = AttoWork.Companion.work(account.lastHash, thisNode.network),
        )
        messagePublisher.publish(
            InboundNetworkMessage(
                thisNode.socketAddress,
                AttoTransactionPush(changeTransaction.toAttoTransaction())
            )
        )

        PropertyHolder.add(transactionShortId, changeTransaction)
    }

    @Then("^transaction (\\w+) is confirmed$")
    fun checkConfirmed(transactionShortId: String) = runTest {
        val expectedTransaction = PropertyHolder.get(Transaction::class.java, transactionShortId)
        val transaction = waitUntilNonNull {
            runBlocking {
                transactionRepository.findByHash(expectedTransaction.hash)
            }
        }
        assertEquals(expectedTransaction, transaction)
    }

    @Then("^matching open or receive transaction for transaction (\\w+) is confirmed$")
    fun checkMatchingConfirmed(transactionShortId: String) = runTest {
        val sendTransaction = PropertyHolder.get(Transaction::class.java, transactionShortId)
        val sendBlock = sendTransaction.block as AttoSendBlock

        val receiverPublicKey = sendBlock.receiverPublicKey

        val transaction = waitUntilNonNull {
            val transaction = runBlocking {
                val account = accountRepository.getByPublicKey(receiverPublicKey)
                transactionRepository.findByHash(account.lastHash)
            }

            val block = transaction?.block
            if (block !is AttoReceiveBlock) {
                return@waitUntilNonNull null
            }

            if (block.sendHash == sendBlock.hash) {
                return@waitUntilNonNull transaction
            }

            return@waitUntilNonNull null
        }
        assertNotNull(transaction)
    }
}