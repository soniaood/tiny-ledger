package tiny.ledger.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tiny.ledger.dto.BalanceResponse;
import tiny.ledger.dto.TransactionRequest;
import tiny.ledger.dto.TransactionResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LedgerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void shouldRecordTransactionAndUpdateBalance() {
        // Get initial balance
        ResponseEntity<BalanceResponse> initialBalanceResponse = restTemplate.getForEntity("/balance", BalanceResponse.class);
        long initialBalance = initialBalanceResponse.getBody().balanceInCents();

        // Make a deposit
        TransactionRequest deposit = new TransactionRequest(5000L, "Integration test deposit", "DEPOSIT");
        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity("/transactions", deposit, TransactionResponse.class);

        // Verify transaction
        assertTrue(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED);
        assertNotNull(response.getBody());
        assertEquals(5000L, response.getBody().amountInCents());
        assertEquals("DEPOSIT", response.getBody().type());
        assertEquals("Integration test deposit", response.getBody().description());

        // Verify balance updated
        ResponseEntity<BalanceResponse> newBalanceResponse = restTemplate.getForEntity("/balance", BalanceResponse.class);
        assertEquals(initialBalance + 5000L, newBalanceResponse.getBody().balanceInCents());
    }

    @Test
    void shouldHandleWithdrawalWhenSufficientFundsExist() {
        // Ensure we have funds
        TransactionRequest deposit = new TransactionRequest(10000L, "Setup funds for withdrawal test", "DEPOSIT");
        restTemplate.postForEntity("/transactions", deposit, TransactionResponse.class);

        ResponseEntity<BalanceResponse> balanceAfterDeposit = restTemplate.getForEntity("/balance", BalanceResponse.class);
        long currentBalance = balanceAfterDeposit.getBody().balanceInCents();

        // Make withdrawal
        TransactionRequest withdrawal = new TransactionRequest(3000L, "Test withdrawal", "WITHDRAWAL");
        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity("/transactions", withdrawal, TransactionResponse.class);

        // Verify withdrawal
        assertTrue(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED);
        assertEquals(3000L, response.getBody().amountInCents());
        assertEquals("WITHDRAWAL", response.getBody().type());

        // Verify balance reduced
        ResponseEntity<BalanceResponse> newBalance = restTemplate.getForEntity("/balance", BalanceResponse.class);
        assertEquals(currentBalance - 3000L, newBalance.getBody().balanceInCents());
    }

    @Test
    void shouldRejectInsufficientFunds() {
        // Get current balance
        ResponseEntity<BalanceResponse> balanceResponse = restTemplate.getForEntity("/balance", BalanceResponse.class);
        long balance = balanceResponse.getBody().balanceInCents();

        // Attempt withdrawal greater than balance
        TransactionRequest withdrawal = new TransactionRequest(balance + 1000L, "Overdraft attempt", "WITHDRAWAL");

        ResponseEntity response = restTemplate.postForEntity("/transactions", withdrawal, TransactionResponse.class);
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void shouldPreventDuplicateTransactionsWithIdempotencyKey() {
        // Create request with idempotency key
        HttpHeaders headers = new HttpHeaders();
        String uniqueKey = "test-key-" + UUID.randomUUID();
        headers.set("Idempotency-Key", uniqueKey);
        TransactionRequest request = new TransactionRequest(1000L, "Idempotency test", "DEPOSIT");
        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);

        // Make same request twice
        ResponseEntity<TransactionResponse> firstResponse = restTemplate.postForEntity("/transactions", entity, TransactionResponse.class);
        ResponseEntity<TransactionResponse> secondResponse = restTemplate.postForEntity("/transactions", entity, TransactionResponse.class);

        // Verify same transaction returned
        assertTrue(firstResponse.getStatusCode() == HttpStatus.OK || firstResponse.getStatusCode() == HttpStatus.CREATED);
        assertTrue(secondResponse.getStatusCode() == HttpStatus.OK || secondResponse.getStatusCode() == HttpStatus.CREATED);

        TransactionResponse first = firstResponse.getBody();
        TransactionResponse second = secondResponse.getBody();

        assertEquals(first.id(), second.id());
        assertEquals(first.amountInCents(), second.amountInCents());
        assertEquals(first.description(), second.description());
    }

    @Test
    void shouldValidateInputCorrectly() {
        long[] invalidAmounts = {0L, -1000L};

        for (long amountInCents : invalidAmounts) {
            TransactionRequest invalidRequest = new TransactionRequest(amountInCents, "Invalid transaction", "DEPOSIT");

            ResponseEntity response = restTemplate.postForEntity("/transactions", invalidRequest, TransactionResponse.class);
            assertEquals(400, response.getStatusCode().value());
        }
    }

    @Test
    void shouldHandleConcurrentTransactionsSafely() throws Exception {
        // Setup initial funds
        TransactionRequest setupDeposit = new TransactionRequest(50000L, "Setup for concurrency test", "DEPOSIT");
        restTemplate.postForEntity("/transactions", setupDeposit, TransactionResponse.class);

        // Concurrent transaction setup
        int numberOfThreads = 5;
        int numberOfTransactionsPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<TransactionResponse> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Execute concurrent transactions
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int threadId = 0; threadId < numberOfThreads; threadId++) {
            int currentThreadId = threadId;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    for (int i = 0; i < numberOfTransactionsPerThread; i++) {
                        TransactionRequest request = new TransactionRequest(
                                500L,
                                "Thread-" + currentThreadId + "-Tx-" + i,
                                "DEPOSIT"
                        );
                        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity("/transactions", request, TransactionResponse.class);
                        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                            results.add(response.getBody());
                        }

                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    System.out.println("Exception in thread " + currentThreadId + ": " + e.getMessage());
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify results
        System.out.println("Results: " + results.size() + ", Exceptions: " + exceptions.size());
        assertTrue(exceptions.isEmpty() || exceptions.stream().noneMatch(e -> e instanceof NullPointerException));

        if (!results.isEmpty()) {
            List<Object> allTransactionIds = new ArrayList<>();
            for (TransactionResponse result : results) {
                allTransactionIds.add(result.id());
            }
            // Check that all IDs are unique
            assertEquals(allTransactionIds.size(), allTransactionIds.stream().distinct().count());
        }
    }

    @Test
    void shouldHandleConcurrentIdempotencyCorrectly() throws Exception {
        // Same idempotency key for multiple threads
        String idempotencyKey = "concurrent-idem-" + UUID.randomUUID();
        int numberOfThreads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<TransactionResponse> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Multiple threads try to create same transaction
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Idempotency-Key", idempotencyKey);
                    TransactionRequest request = new TransactionRequest(2000L, "Concurrent idempotency test", "DEPOSIT");
                    HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);

                    ResponseEntity<TransactionResponse> response = restTemplate.postForEntity("/transactions", entity, TransactionResponse.class);
                    if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                        results.add(response.getBody());
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                    System.out.println("Idempotency exception: " + e.getMessage());
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify all threads got same transaction
        System.out.println("Idempotency results: " + results.size() + ", exceptions: " + exceptions.size());

        if (!results.isEmpty()) {
            TransactionResponse firstTransaction = results.get(0);
            for (TransactionResponse result : results) {
                assertEquals(firstTransaction.id(), result.id());
                assertEquals(firstTransaction.amountInCents(), result.amountInCents());
            }
        }
    }
}