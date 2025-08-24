package tiny.ledger.controller

import spock.lang.Specification
import tiny.ledger.dto.TransactionRequest
import tiny.ledger.entity.Movement
import tiny.ledger.service.LedgerService

import java.time.Instant

class LedgerControllerTest extends Specification {
    def ledgerService = Mock(LedgerService)

    def target = new LedgerController(ledgerService)

    def "Get transactions without pagination"() {
        given: "a list of transactions"
        def movement = new Movement(1, Movement.MovementType.DEPOSIT, 10000L, Instant.now(), "Test transaction", null)

        when: "get transactions is requested"
        def result = target.getTransactions(null, null)

        then: "the ledger service is called with the correct parameters"
        1 * ledgerService.getMovementHistory(null, null) >> [movement]

        and: "the result contains the expected transaction"
        result.data().size() == 1
        result.data().asList().get(0).id() == movement.id()
        result.count() == 1

        and: "no more interactions are present"
        0 * _
    }

    def "Get transactions with pagination"() {
        given: "a list of transactions"
        def movement = new Movement(1, Movement.MovementType.WITHDRAWAL, 5000L, Instant.now(), "Test transaction", null)

        when: "get transactions is requested with pagination"
        def result = target.getTransactions(10, 0)

        then: "the ledger service is called with the correct parameters"
        1 * ledgerService.getMovementHistory(10, 0) >> [movement]

        and: "the result contains the expected transaction"
        result.data().size() == 1
        result.data().asList().get(0).id() == movement.id()
        result.count() == 1

        and: "no more interactions are present"
        0 * _
    }

    def "Get transaction history with invalid pagination"() {
        when: "get transactions is requested with invalid pagination"
        target.getTransactions(-1, -1)

        then: "an exception is thrown"
        def e = thrown(IllegalArgumentException)
        e.message == "Invalid pagination parameters: limit must be > 0 and offset must be >= 0."

        and: "no more interactions are present"
        0 * _
    }

    def "Record valid deposit transaction with no idempotency key"() {
        given: "a valid deposit transaction"
        def transactionRequest = new TransactionRequest(10000, "Test deposit", "DEPOSIT")

        when: "the deposit transaction is recorded"
        def result = target.recordTransaction(null, transactionRequest)

        then: "the ledger service is called with the correct parameters"
        1 * ledgerService.recordMovement(10000L, Movement.MovementType.DEPOSIT, "Test deposit", null) >> new Movement(1L, Movement.MovementType.DEPOSIT, 10000L, Instant.now(), "Test deposit", null)

        and: "the result is the expected transaction"
        result.id() == 1L

        and: "no more interactions are present"
        0 * _
    }

    def "Record valid withdrawal transaction with idempotency key"() {
        given: "a valid withdrawal transaction"
        def transactionRequest = new TransactionRequest(5000, "Test withdrawal", "WITHDRAWAL")
        def idempotencyKey = "unique-idempotency-key"

        when: "the withdrawal transaction is recorded"
        def result = target.recordTransaction(idempotencyKey, transactionRequest)

        then: "the ledger service is called with the correct parameters"
        1 * ledgerService.recordMovement(5000L, Movement.MovementType.WITHDRAWAL, "Test withdrawal", idempotencyKey) >> new Movement(2L, Movement.MovementType.DEPOSIT, 10000L, Instant.now(), "Test deposit", null)

        and: "the result is the expected transaction"
        result.id() == 2L

        and: "no more interactions are present"
        0 * _
    }

    def "Get balance returns balance result"() {
        given: "a balance of 15000"
        def balance = 15000L

        when: "the balance is requested"
        def result = target.getCurrentBalance()

        then: "the ledger service is called to get the balance"
        1 * ledgerService.getCurrentBalanceInCents() >> balance

        and: "the result contains the expected balance"
        result.balanceInCents() == balance
        result.date() != null

        and: "no more interactions are present"
        0 * _
    }

}
