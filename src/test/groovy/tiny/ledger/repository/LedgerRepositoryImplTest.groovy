package tiny.ledger.repository

import spock.lang.Specification
import tiny.ledger.entity.Movement

import java.time.Instant

class LedgerRepositoryImplTest extends Specification {
    def repository = new LedgerRepositoryImpl()

    def "save movement generates incremental id"() {
        given:
        def movement = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L,
                Instant.now(), "Test deposit", "key123")

        when:
        def savedMovement = repository.save(movement)

        then:
        savedMovement.id() == 1L
        savedMovement.type() == Movement.MovementType.DEPOSIT
        savedMovement.amountInCents() == 1000L
        savedMovement.description() == "Test deposit"
        savedMovement.idempotencyKey() == "key123"
        savedMovement.createdOn() != null
    }

    def "save multiple movements with incremental ids"() {
        given:
        def movement1 = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L,
                Instant.now(), "First", "key123")
        def movement2 = new Movement(0L, Movement.MovementType.WITHDRAWAL, 500L,
                Instant.now(), "Second", "key124")

        when:
        def saved1 = repository.save(movement1)
        def saved2 = repository.save(movement2)

        then:
        saved1.id() == 1L
        saved2.id() == 2L
    }

    def "save only one movement with the same idempotency key (handles idempotency)"() {
        given:
        def movement1 = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L,
                Instant.now(), "First", "key123")
        def movement2 = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L,
                Instant.now(), "Second", "key123")

        when:
        def saved1 = repository.save(movement1)
        def saved2 = repository.save(movement2)

        then:
        saved1.id() == saved2.id()
    }

    def "save movements without idempotency key"() {
        given:
        def movement = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L,
                Instant.now(), "No key", null)

        when:
        def savedMovement = repository.save(movement)

        then:
        savedMovement.id() == 1L
        savedMovement.idempotencyKey() == null
    }

    def "find movements sorted by creation time descending"() {
        given:
        def now = Instant.now()
        def movement1 = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L, now.minusSeconds(10), "First", null)
        def movement2 = new Movement(0L, Movement.MovementType.DEPOSIT, 2000L, now.minusSeconds(5), "Second", null)
        def movement3 = new Movement(0L, Movement.MovementType.DEPOSIT, 3000L, now, "Third", null)

        repository.save(movement1)
        repository.save(movement2)
        repository.save(movement3)

        when:
        def movements = repository.findMovements(null, null)

        then:
        movements.size() == 3
        movements[0].description() == "Third"
        movements[1].description() == "Second"
        movements[2].description() == "First"
    }

    def "get movements with pagination"() {
        given:
        5.times { i ->
            def movement = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L,
                    Instant.now(), "Movement ${i}", null)
            repository.save(movement)
        }

        when:
        def movements = repository.findMovements(3, 3)

        then:
        movements.size() == 2
    }

    def "get current balance in cents returns balance with deposits and withdrawals successfully"() {
        given:
        def deposit1 = new Movement(0L, Movement.MovementType.DEPOSIT, 1000L,
                Instant.now(), "Deposit 1", null)
        def deposit2 = new Movement(0L, Movement.MovementType.DEPOSIT, 500L,
                Instant.now(), "Deposit 2", null)
        def withdrawal = new Movement(0L, Movement.MovementType.WITHDRAWAL, 300L,
                Instant.now(), "Withdrawal 1", null)

        repository.save(deposit1)
        repository.save(deposit2)
        repository.save(withdrawal)

        when:
        def balance = repository.getCurrentBalanceInCents()

        then:
        balance == 1200L
    }

    def "get current balance in cents returns zero balance when no movements exist"() {
        when:
        def balance = repository.getCurrentBalanceInCents()

        then:
        balance == 0L
    }

    def "ledger repository in memory implementation handles concurrent saves safely"() {
        given:
        def movements = []
        10.times { i ->
            movements << new Movement(0L, Movement.MovementType.DEPOSIT, 100L * i,
                    Instant.now(), "Concurrent ${i}", null)
        }

        when:
        def savedMovements = movements.parallelStream()
                .map { movement -> repository.save((Movement) movement) }
                .toList()

        then:
        savedMovements.size() == 10
        savedMovements.collect { it.id() }.sort() == (1..10).toList()
        repository.getCurrentBalanceInCents() == 4500L
    }
}
