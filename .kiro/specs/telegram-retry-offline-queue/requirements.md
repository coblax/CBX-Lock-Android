# Requirements: Telegram Retry + Offline Queue

## Requirement 1: Retry with Exponential Backoff

### Acceptance Criteria

1.1. When a Telegram HTTP POST fails with a retryable error (IOException, HTTP 5xx, HTTP 429), the system retries up to 3 total attempts before giving up.

1.2. The delay between retry attempts follows exponential backoff: approximately 1 second after the first failure, 3 seconds after the second failure, and 9 seconds after the third failure, each with ±20% random jitter.

1.3. When a permanent client error occurs (HTTP 400, 401, 403, or any 4xx except 429), the system does NOT retry and returns failure immediately.

1.4. When any retry attempt succeeds (HTTP 2xx response), the system returns success immediately without further attempts.

## Requirement 2: Offline Message Queue

### Acceptance Criteria

2.1. When all retry attempts for a message fail, the message is persisted to a disk-backed queue that survives app process death and device restart.

2.2. The persistent queue stores messages in FIFO order and delivers them in the same order when flushed.

2.3. The persistent queue has a maximum capacity of 50 messages. When the queue is full and a new message arrives, the oldest message is dropped to make room.

2.4. The queue uses SharedPreferences for persistence with no additional database dependencies (no Room, no SQLite).

2.5. Each queued message stores: a unique ID, bot token, chat ID, message text, enqueue timestamp, and attempt count.

## Requirement 3: Network-Aware Queue Flush

### Acceptance Criteria

3.1. When network connectivity is restored (ConnectivityManager.NetworkCallback.onAvailable), the system automatically begins flushing queued messages.

3.2. During flush, messages are sent in FIFO order, each respecting the rate limiter before sending.

3.3. If a message fails to send during flush, the flush stops immediately. Successfully sent messages are removed from the queue; remaining messages stay for the next flush attempt.

3.4. The network listener is passive (callback-based, no polling) to minimize battery usage.

## Requirement 4: Rate Limiting

### Acceptance Criteria

4.1. The system enforces a maximum of 5 messages sent to the Telegram API within any 10-second window.

4.2. When the rate limit is exhausted, callers are suspended (not blocked) until tokens refill — no messages are dropped due to rate limiting alone.

4.3. Rate limiting applies to both immediate sends and queue flush operations equally.

## Requirement 5: Non-Blocking Main Thread

### Acceptance Criteria

5.1. All HTTP calls, retry delays, queue I/O, and flush operations execute on Dispatchers.IO, never on the Android main thread.

5.2. Callers can use a fire-and-forget `sendAsync()` method that returns a Job immediately without suspending.

5.3. SharedPreferences writes use `apply()` (async) rather than `commit()` (synchronous) to avoid blocking.

## Requirement 6: Integration with Existing Transport

### Acceptance Criteria

6.1. The existing `sendTelegramSectionReport()` and `sendTelegramAlarmAcknowledge()` functions are updated to route message chunks through the new TelegramMessageQueue instead of calling `sendTelegramTextMessage()` directly.

6.2. The existing `sendTelegramTextMessage()` function remains unchanged as the low-level HTTP transport used internally by RetryExecutor.

6.3. The existing message chunking logic (`buildTelegramMessageChunks()`) remains unchanged — each chunk is treated as an independent message for retry/queue purposes.

## Requirement 7: Low-Resource Device Compatibility

### Acceptance Criteria

7.1. No new third-party dependencies are introduced. The implementation uses only kotlinx.coroutines, Android system APIs (ConnectivityManager, SharedPreferences), and org.json (built into Android).

7.2. Maximum memory footprint of the queue is bounded: 50 messages × ~4KB max = ~200KB in SharedPreferences.

7.3. No dedicated background threads are created. All work uses the shared Dispatchers.IO coroutine pool.

## Requirement 8: Lifecycle Management

### Acceptance Criteria

8.1. The TelegramMessageQueue provides a `startNetworkListener()` method to register the ConnectivityManager callback and a `shutdown()` method to unregister it and cancel pending coroutines.

8.2. On app startup, if the queue contains persisted messages and network is available, a flush is triggered automatically.

8.3. The system handles the case where `shutdown()` is called while a flush is in progress — the flush is cancelled gracefully without corrupting the queue.
