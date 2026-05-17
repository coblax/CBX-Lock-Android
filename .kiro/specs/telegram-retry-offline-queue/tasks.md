# Tasks: Telegram Retry + Offline Queue

## Task 1: Create RetryExecutor with Exponential Backoff

### Description
Implement the `RetryExecutor` class that wraps a single `sendTelegramTextMessage()` call with configurable exponential backoff retry logic.

### Files to Create/Modify
- Create: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramRetryExecutor.kt`

### Implementation Details
- [x] 1.1 Create `TelegramRetryExecutor` class with configurable `maxAttempts` (default 3), `baseDelayMs` (default 1000), `multiplier` (default 3.0), `jitterFraction` (default 0.2)
- [x] 1.2 Implement `suspend fun execute(token: String, chatId: String, message: String): Result<Unit>` that loops up to `maxAttempts`, calling `sendTelegramTextMessage()` each time
- [x] 1.3 Implement exponential backoff delay between attempts: `baseDelayMs * multiplier^(attempt-1)` with random jitter of ±`jitterFraction`
- [x] 1.4 Implement `isPermanentFailure()` helper that returns true for HTTP 4xx (except 429) — these should NOT be retried
- [x] 1.5 Create a `TelegramHttpException` class (or use existing error parsing) to extract HTTP status codes from `sendTelegramTextMessage()` errors for retry/permanent classification
- [x] 1.6 Ensure the function returns `Result.success(Unit)` on first successful attempt and `Result.failure(lastException)` after all retries exhausted

### Acceptance Criteria Covered
- 1.1, 1.2, 1.3, 1.4

---

## Task 2: Create Token-Bucket RateLimiter

### Description
Implement a coroutine-friendly token-bucket rate limiter that enforces max 5 messages per 10-second window.

### Files to Create/Modify
- Create: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramRateLimiter.kt`

### Implementation Details
- [x] 2.1 Create `TelegramRateLimiter` class with configurable `maxTokens` (default 5) and `refillPeriodMs` (default 10_000L)
- [x] 2.2 Implement token tracking with `Mutex` for thread safety, using `SystemClock.elapsedRealtime()` for refill timing
- [x] 2.3 Implement `suspend fun acquire()` that suspends the caller in a loop until a token is available, checking refill on each iteration
- [x] 2.4 Implement `fun tryAcquire(): Boolean` for non-suspending token check (returns false if no tokens available)
- [x] 2.5 Implement `refillTokens()` private method that resets tokens to `maxTokens` when `refillPeriodMs` has elapsed since last refill

### Acceptance Criteria Covered
- 4.1, 4.2, 4.3

---

## Task 3: Create PersistentMessageQueue

### Description
Implement a disk-backed FIFO message queue using SharedPreferences with JSON serialization, max size enforcement, and atomic operations.

### Files to Create/Modify
- Create: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramPersistentQueue.kt`

### Implementation Details
- [x] 3.1 Create `QueuedMessage` data class with fields: `id` (String/UUID), `token`, `chatId`, `message`, `enqueuedAt` (Long), `attemptCount` (Int)
- [x] 3.2 Create `TelegramPersistentQueue` class that takes `Context`, `maxSize` (default 50), and `prefsName` (default "telegram_offline_queue")
- [x] 3.3 Implement JSON serialization/deserialization for `QueuedMessage` using `org.json.JSONObject` and `org.json.JSONArray`
- [x] 3.4 Implement `fun enqueue(entry: QueuedMessage)` — loads entries, drops oldest while size >= maxSize, appends new entry, saves with `apply()`
- [x] 3.5 Implement `fun peek(count: Int): List<QueuedMessage>` — returns up to `count` entries from the head without removing
- [x] 3.6 Implement `fun remove(id: String)` and `fun removeAll(ids: List<String>)` — removes entries by ID and persists
- [x] 3.7 Implement `fun size(): Int` and `fun clear()` utility methods
- [x] 3.8 Use `synchronized(lock)` for all read/write operations to ensure thread safety

### Acceptance Criteria Covered
- 2.1, 2.2, 2.3, 2.4, 2.5

---

## Task 4: Create QueueFlusher with NetworkCallback

### Description
Implement the network-aware queue flusher that registers a ConnectivityManager callback and drains the persistent queue when connectivity is restored.

### Files to Create/Modify
- Create: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramQueueFlusher.kt`

### Implementation Details
- [x] 4.1 Create `TelegramQueueFlusher` class that takes `Context`, `TelegramPersistentQueue`, `TelegramRetryExecutor`, `TelegramRateLimiter`, and `CoroutineScope`
- [x] 4.2 Implement `fun register()` that registers a `ConnectivityManager.NetworkCallback` with `onAvailable` triggering flush
- [x] 4.3 Implement `fun unregister()` that safely unregisters the callback
- [x] 4.4 Implement `private suspend fun flushQueue()` — iterates queue in FIFO order, acquires rate limit permit for each, sends via RetryExecutor, removes on success, breaks on first failure
- [x] 4.5 Add guard to prevent concurrent flush operations (use `AtomicBoolean` or `Mutex`)
- [x] 4.6 Launch flush in the provided `CoroutineScope` so it can be cancelled on shutdown

### Acceptance Criteria Covered
- 3.1, 3.2, 3.3, 3.4

---

## Task 5: Create TelegramMessageQueue Coordinator

### Description
Implement the main entry-point class that coordinates rate limiting, retry, and offline queueing into a single API for callers.

### Files to Create/Modify
- Create: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramMessageQueue.kt`

### Implementation Details
- [x] 5.1 Create `TelegramMessageQueue` class that takes `Context` and internally creates `TelegramRetryExecutor`, `TelegramRateLimiter`, `TelegramPersistentQueue`, and `TelegramQueueFlusher`
- [x] 5.2 Implement `suspend fun send(token: String, chatId: String, message: String): Result<Unit>` — acquires rate limit, executes with retry, enqueues on failure
- [x] 5.3 Implement `fun sendAsync(token: String, chatId: String, message: String): Job` — launches `send()` in the internal CoroutineScope, returns the Job
- [x] 5.4 Implement `fun startNetworkListener()` — registers the QueueFlusher and triggers an initial flush if queue is non-empty and network is available
- [x] 5.5 Implement `fun shutdown()` — unregisters network callback, cancels the CoroutineScope (which cancels any in-progress flush)
- [x] 5.6 Use `SupervisorJob()` in the internal scope so individual send failures don't cancel the entire scope

### Acceptance Criteria Covered
- 5.1, 5.2, 8.1, 8.2, 8.3

---

## Task 6: Add Retry Constants to RuntimeThresholds

### Description
Add the retry and queue configuration constants to the existing `RuntimeThresholds.kt` file for centralized configuration.

### Files to Create/Modify
- Modify: `app/src/main/java/com/example/coblaxexamlock/config/RuntimeThresholds.kt`

### Implementation Details
- [x] 6.1 Add `TelegramRetryMaxAttempts = 3`
- [x] 6.2 Add `TelegramRetryBaseDelayMs = 1000L`
- [x] 6.3 Add `TelegramRetryMultiplier = 3.0`
- [x] 6.4 Add `TelegramRetryJitterFraction = 0.2`
- [x] 6.5 Add `TelegramRateLimitMaxTokens = 5`
- [x] 6.6 Add `TelegramRateLimitRefillPeriodMs = 10_000L`
- [x] 6.7 Add `TelegramOfflineQueueMaxSize = 50`

### Acceptance Criteria Covered
- 1.1, 1.2, 4.1, 2.3

---

## Task 7: Integrate TelegramMessageQueue into Existing Senders

### Description
Update `sendTelegramSectionReport()` and `sendTelegramAlarmAcknowledge()` to route message chunks through the new `TelegramMessageQueue` instead of calling `sendTelegramTextMessage()` directly.

### Files to Create/Modify
- Modify: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramSectionReport.kt`
- Modify: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramAlarmAcknowledge.kt`

### Implementation Details
- [x] 7.1 Create a singleton or top-level accessor for `TelegramMessageQueue` instance (e.g., `TelegramMessageQueueHolder` object with lazy initialization taking application Context)
- [x] 7.2 In `sendTelegramSectionReport()`, replace the `buildTelegramMessageChunks(message).forEach { chunk -> sendTelegramTextMessage(...) }` block with `TelegramMessageQueueHolder.instance.send(token, chatId, chunk)` calls
- [x] 7.3 In `sendTelegramAlarmAcknowledge()`, replace the same pattern with queue-based sends
- [x] 7.4 Ensure `startNetworkListener()` is called during app initialization (e.g., in Application.onCreate or exam session start)
- [x] 7.5 Ensure `shutdown()` is called when the exam session ends or app is being destroyed

### Acceptance Criteria Covered
- 6.1, 6.2, 6.3, 8.1

---

## Task 8: Update sendTelegramTextMessage Error Reporting

### Description
Modify `sendTelegramTextMessage()` to throw a typed exception (`TelegramHttpException`) that includes the HTTP status code, enabling the RetryExecutor to distinguish retryable from permanent errors.

### Files to Create/Modify
- Modify: `app/src/main/java/com/example/coblaxexamlock/runtime/TelegramTransport.kt`

### Implementation Details
- [x] 8.1 Create `TelegramHttpException(val statusCode: Int, message: String) : IOException(message)` class in TelegramTransport.kt
- [x] 8.2 Update the error handling in `sendTelegramTextMessage()` to throw `TelegramHttpException(responseCode, errorMessage)` instead of calling `error(errorMessage)`
- [x] 8.3 Ensure IOExceptions from connection/timeout failures propagate naturally (they are already retryable)

### Acceptance Criteria Covered
- 1.3, 1.1

---

## Task 9: Write Unit Tests

### Description
Write unit tests for all new components to verify retry logic, rate limiting, queue behavior, and integration.

### Files to Create/Modify
- Create: `app/src/test/java/com/example/coblaxexamlock/runtime/TelegramRetryExecutorTest.kt`
- Create: `app/src/test/java/com/example/coblaxexamlock/runtime/TelegramRateLimiterTest.kt`
- Create: `app/src/test/java/com/example/coblaxexamlock/runtime/TelegramPersistentQueueTest.kt`
- Create: `app/src/test/java/com/example/coblaxexamlock/runtime/TelegramQueueFlusherTest.kt`

### Implementation Details
- [ ] 9.1 Test RetryExecutor: verify 3 attempts on transient failure, immediate return on success, no retry on 4xx, correct backoff timing (within tolerance)
- [ ] 9.2 Test RateLimiter: verify >5 rapid acquires cause suspension, verify refill after period, verify tryAcquire returns false when exhausted
- [ ] 9.3 Test PersistentQueue: verify enqueue/dequeue FIFO order, max size enforcement (drop oldest), serialization round-trip, remove by ID, clear
- [ ] 9.4 Test QueueFlusher: verify flush triggers on network available, stops on first failure, removes successful sends, respects rate limiter
- [ ] 9.5 Test TelegramMessageQueue: verify end-to-end flow — success path, retry-then-queue path, flush-on-reconnect path

### Acceptance Criteria Covered
- All requirements verified through tests
