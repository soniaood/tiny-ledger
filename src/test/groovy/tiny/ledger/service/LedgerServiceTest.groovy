package tiny.ledger.service

import spock.lang.Specification
import tiny.ledger.entity.Movement
import tiny.ledger.repository.LedgerRepository

import java.time.Instant

class LedgerServiceTest extends Specification {

    def ledgerRepository = Mock(LedgerRepository)

    def target = new LedgerService(ledgerRepository)

    def "Get movement history without pagination"() {
        given: "movement history is present"
        def movement = new Movement(1L, Movement.MovementType.DEPOSIT, 10000L, Instant.now(), "Test transaction", null)
        def movement2 = new Movement(2L, Movement.MovementType.DEPOSIT, 5000L, Instant.now(), "Test transaction", null)

        when: "getMovementHistory is called without pagination"
        def result = target.getMovementHistory(null, null)

        then: "the ledger repository is called"
        1 * ledgerRepository.findMovements(null,null) >> [movement, movement2]

        then: "the result contains the expected movements"
        result.size() == 2
        result[0].id() == 1L
        result[1].id() == 2L

        and: "no more interactions are present"
        0 * _
    }

    def "Get movement history with pagination"() {
        given: "movement history is present"
        def movement = new Movement(1L, Movement.MovementType.WITHDRAWAL, 5000L, Instant.now(), "Test transaction", null)

        when: "getMovementHistory is called with pagination"
        def result = target.getMovementHistory(1, 0)

        then: "the ledger repository is called with pagination parameters"
        1 * ledgerRepository.findMovements(1, 0) >> [movement]

        then: "the result contains the expected movement"
        result.size() == 1
        result[0].id() == 1L

        and: "no more interactions are present"
        0 * _
    }

    def "Record movement with valid deposit transaction"() {
        when: "recordMovement is called with a valid deposit transaction"
        def result = target.recordMovement(10000L, Movement.MovementType.DEPOSIT, "Test deposit", null)

        then: "the movement is created with the correct parameters"
        1 * ledgerRepository.save(_ as Movement) >> { args ->
            def savedMovement = (Movement) args[0]
            savedMovement.idempotencyKey() == null
            savedMovement.type() == Movement.MovementType.DEPOSIT
            savedMovement.amountInCents() == 10000L
            savedMovement.description() == "Test deposit"
            return savedMovement
        }

        and: "the result contains the saved movement"
        result.type() == Movement.MovementType.DEPOSIT
        result.amountInCents() == 10000L
        result.description() == "Test deposit"
        result.idempotencyKey() == null

        and: "no more interactions are present"
        0 * _
    }

    def "Record movement with valid withdrawal transaction"() {
        when: "recordMovement is called with a valid withdrawal transaction"
        def result = target.recordMovement(5000L, Movement.MovementType.WITHDRAWAL, "Test withdrawal", null)

        then: "the balance allows it"
        1 * ledgerRepository.getCurrentBalanceInCents() >> 10000L

        and: "the movement is created with the correct parameters"
        1 * ledgerRepository.save(_ as Movement) >> { args ->
            def savedMovement = (Movement) args[0]
            savedMovement.idempotencyKey() == null
            savedMovement.type() == Movement.MovementType.WITHDRAWAL
            savedMovement.amountInCents() == 5000L
            savedMovement.description() == "Test withdrawal"
            return savedMovement
        }

        and: "the result contains the saved movement"
        result.type() == Movement.MovementType.WITHDRAWAL
        result.amountInCents() == 5000L
        result.description() == "Test withdrawal"
        result.idempotencyKey() == null

        and: "no more interactions are present"
        0 * _
    }

    def "Record movement with invalid withdrawal"() {
        when: "recordMovement is called with an invalid transaction type"
        target.recordMovement(10000L, Movement.MovementType.WITHDRAWAL, "Invalid transaction", "key")

        then: "the balance doesn't allow it"
        1 * ledgerRepository.getCurrentBalanceInCents() >> 5000L

        and: "an exception is thrown"
        def e = thrown(IllegalStateException)
        e.message == "Insufficient funds for this transaction."

        and: "no more interactions are present"
        0 * _
    }

    def "Record movement with negative amount"() {
        when: "recordMovement is called with an invalid transaction type"
        target.recordMovement(-10000L, Movement.MovementType.DEPOSIT, "Invalid transaction", "key")

        then: "an exception is thrown"
        def e = thrown(IllegalArgumentException)
        e.message == "Transaction amount must be greater than zero."

        and: "no interactions with the ledger service"
        0 * _
    }

}
