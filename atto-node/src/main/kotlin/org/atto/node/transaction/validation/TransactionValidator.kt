package org.atto.node.transaction.validation

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.atto.node.EventPublisher
import org.atto.node.account.Account
import org.atto.node.transaction.Transaction
import org.atto.node.transaction.TransactionRejected
import org.atto.node.transaction.TransactionStarted
import org.atto.node.transaction.TransactionValidated
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class TransactionValidator(
    private val validators: List<TransactionValidationSupport>,
    private val eventPublisher: EventPublisher,
) {

    @EventListener
    fun process(event: TransactionStarted) = runBlocking {
        launch {
            validate(event.account, event.payload)
        }
    }

    private suspend fun validate(account: Account, change: Transaction) {
        val rejectionReason = validators.asFlow()
            .filter { it.supports(change) }
            .mapNotNull { it.validate(account, change) }
            .firstOrNull()

        if (rejectionReason != null) {
            eventPublisher.publish(TransactionRejected(rejectionReason, account, change))
        } else {
            eventPublisher.publish(TransactionValidated(account, change))
        }
    }
}