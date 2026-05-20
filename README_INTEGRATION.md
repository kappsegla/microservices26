# Microservices Integration Lab: Order & Stock Service

This project implements an asynchronous integration between `OrderService` and `StockService` using the Outbox Pattern and Idempotent Consumer.

## Tech Stack
- Java 26 (using Record Patterns and Switch Expressions)
- Spring Boot 4.0.6
- MySQL (Separate schemas: `orders_db`, `stock_db`)
- RabbitMQ (Topic Exchange, DLX, DLQ)
- Docker Compose

## Key Features
- **Outbox Pattern:** Orders and Outbox events are saved in a single transaction in `OrderService`.
- **Reliable Publishing:** A scheduled worker (`OutboxRelay`) publishes events and marks them `PROCESSED` only after a "Publisher Confirm".
- **Idempotent Consumer:** `StockService` uses a `processed_events` table to deduplicate incoming messages.
- **Error Handling:** Configured retries (3 attempts) with exponential backoff and routing to Dead Letter Queue (`stock.orders.dlq`).
- **Chaos Simulator:** Toggable scenarios to test system resilience.

---

## Demonstration Guide (Step-by-Step)

Follow these steps to verify the robustness of the integration. Choose the command set that matches your terminal.

### 1. Preparation
Start the infrastructure:
```bash
docker-compose up -d --build
```

### 2. Scenario: Happy Path (NORMAL)
Verify that a normal order reduces stock correctly.

#### Bash (Linux/macOS/Git Bash)
```bash
# Reset scenarios
curl -X POST http://localhost:8081/chaos/scenario -H "Content-Type: application/json" -d '"NORMAL"'
curl -X POST http://localhost:8082/chaos/scenario -H "Content-Type: application/json" -d '"NORMAL"'

# Check initial stock (Default for 'Widget' is 100)
curl http://localhost:8082/stocks

# Place an order
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product": "Widget", "quantity": 5, "price": 10.0}'

# Wait for processing
sleep 10

# Check order status (should be COMPLETED)
curl http://localhost:8081/orders/1
```

#### PowerShell (Windows)
```powershell
# Reset scenarios
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/chaos/scenario" -ContentType "application/json" -Body '"NORMAL"'
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/chaos/scenario" -ContentType "application/json" -Body '"NORMAL"'

# Check initial stock
Invoke-RestMethod -Uri "http://localhost:8082/stocks"

# Place an order
$order = @{ product = "Widget"; quantity = 5; price = 10.0 } | ConvertTo-Json
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/orders" -ContentType "application/json" -Body $order

# Wait for processing
Start-Sleep -Seconds 10

# Check order status (should be COMPLETED)
Invoke-RestMethod -Uri "http://localhost:8081/orders/$($response.id)"
```

---

### 3. Scenario: Outbox Recovery (FAIL_BEFORE_PUBLISH)
Simulates a crash *after* the database transaction succeeds but *before* the message is sent.

#### Bash
```bash
# Set OrderService to fail after DB save
curl -X POST http://localhost:8081/chaos/scenario -H "Content-Type: application/json" -d '"FAIL_BEFORE_PUBLISH"'

# Place an order (This request will return an error 500)
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product": "Widget", "quantity": 10, "price": 10.0}'

# Wait for OutboxRelay to recover and process (approx 15s)
sleep 15

# Check order status (it should eventually be COMPLETED)
curl http://localhost:8081/orders/2

# Verify stock is reduced to 85 (95 - 10)
curl http://localhost:8082/stocks

# Verify via logs that the OutboxRelay performed the recovery
docker logs microservices26-orderservice-1 | grep "Outbox Recovery"
```

#### PowerShell
```powershell
# Set OrderService to fail after DB save
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/chaos/scenario" -ContentType "application/json" -Body '"FAIL_BEFORE_PUBLISH"'

# Place an order (This will throw an exception in PS due to 500 status)
$order = @{ product = "Widget"; quantity = 10; price = 10.0 } | ConvertTo-Json
try { Invoke-RestMethod -Method Post -Uri "http://localhost:8081/orders" -ContentType "application/json" -Body $order } catch { Write-Host "Expected Failure: $($_.Exception.Message)" }

# Wait for OutboxRelay to recover and process (approx 15s)
Start-Sleep -Seconds 15

# Check order status (it should eventually be COMPLETED)
Invoke-RestMethod -Uri "http://localhost:8081/orders/2"

# Verify stock is reduced to 85 (95 - 10)
Invoke-RestMethod -Uri "http://localhost:8082/stocks"

# Verify via logs that the OutboxRelay performed the recovery
docker logs microservices26-orderservice-1 | Select-String "Outbox Recovery"
```

---

### 4. Scenario: Deduplication (DUPLICATE_MESSAGE)
Simulates the network sending the same message twice.

#### Bash
```bash
# Set OrderService to send duplicate messages
curl -X POST http://localhost:8081/chaos/scenario -H "Content-Type: application/json" -d '"DUPLICATE_MESSAGE"'

# Place an order
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product": "Widget", "quantity": 1, "price": 10.0}'

# Wait for processing
sleep 10

# Check stock (Verify stock only reduced by 1)
curl http://localhost:8082/stocks

# Check logs for deduplication
docker logs microservices26-stockservice-1 | grep "already processed. Skipping"
```

#### PowerShell
```powershell
# Set OrderService to send duplicate messages
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/chaos/scenario" -ContentType "application/json" -Body '"DUPLICATE_MESSAGE"'

# Place an order
$order = @{ product = "Widget"; quantity = 1; price = 10.0 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/orders" -ContentType "application/json" -Body $order

# Wait for processing
Start-Sleep -Seconds 10

# Check stock (Verify stock only reduced by 1)
Invoke-RestMethod -Uri "http://localhost:8082/stocks"

# Check logs for deduplication
docker logs microservices26-stockservice-1 | Select-String "already processed. Skipping"
```

---

### 5. Scenario: Retries (TRANSIENT_FAILURE)
Simulates a service that fails briefly but then recovers.

#### Bash
```bash
# Set StockService to fail the first 2 attempts
curl -X POST http://localhost:8082/chaos/scenario -H "Content-Type: application/json" -d '"TRANSIENT_FAILURE"'

# Reset OrderService to NORMAL
curl -X POST http://localhost:8081/chaos/scenario -H "Content-Type: application/json" -d '"NORMAL"'

# Place an order
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product": "Widget", "quantity": 4, "price": 10.0}'

# Wait for retries
sleep 15

# Verify stock reduction
curl http://localhost:8082/stocks

# Check logs for retry attempts
docker logs microservices26-stockservice-1 | grep -E "Simulating transient failure|Retry detected"
```

#### PowerShell
```powershell
# Set StockService to fail the first 2 attempts
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/chaos/scenario" -ContentType "application/json" -Body '"TRANSIENT_FAILURE"'

# Reset OrderService to NORMAL
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/chaos/scenario" -ContentType "application/json" -Body '"NORMAL"'

# Place an order
$order = @{ product = "Widget"; quantity = 4; price = 10.0 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/orders" -ContentType "application/json" -Body $order

# Wait for retries
Start-Sleep -Seconds 15

# Verify stock reduction
Invoke-RestMethod -Uri "http://localhost:8082/stocks"

# Check logs for retry attempts
docker logs microservices26-stockservice-1 | Select-String "Simulating transient failure", "Retry detected"
```

---

### 6. Scenario: Compensating Action (Saga Pattern)
Simulates what happens when business logic fails (e.g., out of stock).

#### Bash
```bash
# Reset OrderService to NORMAL
curl -X POST http://localhost:8081/chaos/scenario -H "Content-Type: application/json" -d '"NORMAL"'

# Check initial stock for 'ExpensiveItem'
curl http://localhost:8082/stocks

# Place an order for MORE than available stock (e.g., 101)
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product": "ExpensiveItem", "quantity": 101, "price": 999.0}'

# Wait for StockService and compensating action
sleep 10

# Check order status (it should be CANCELLED_OUT_OF_STOCK)
curl http://localhost:8081/orders/5
```

#### PowerShell
```powershell
# Reset OrderService to NORMAL
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/chaos/scenario" -ContentType "application/json" -Body '"NORMAL"'

# Check initial stock
Invoke-RestMethod -Uri "http://localhost:8082/stocks"

# Place an order for MORE than available stock
$order = @{ product = "ExpensiveItem"; quantity = 101; price = 999.0 } | ConvertTo-Json
$response = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/orders" -ContentType "application/json" -Body $order

# Wait for StockService and compensating action
Start-Sleep -Seconds 10

# Check order status (using ID from response)
Invoke-RestMethod -Uri "http://localhost:8081/orders/$($response.id)"
```

---

### 7. Scenario: Poison Message & Dead Letter Queue (DLQ)
Simulates a message that repeatedly fails processing due to invalid business content (a "poison message") and eventually gets routed to the Dead Letter Queue (DLQ).

In this scenario, we send an order with a negative quantity (`-5`). The `StockService` detects this as invalid and throws an `IllegalArgumentException` in its business logic, which triggers Spring AMQP to retry processing. Because the retry limit is configured to `3` in `application.properties`, the listener attempts to process the message 3 times before exhausting retries, rejecting the message, and letting RabbitMQ dead-letter it to the DLQ (`stock.orders.dlq`).

#### Bash
```bash
# Reset scenarios to NORMAL
curl -X POST http://localhost:8081/chaos/scenario -H "Content-Type: application/json" -d '"NORMAL"'
curl -X POST http://localhost:8082/chaos/scenario -H "Content-Type: application/json" -d '"NORMAL"'

# Place an order with negative quantity
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{"product": "Widget", "quantity": -5, "price": 10.0}'

# Wait for retries to exhaust (approx 10s)
sleep 10

# Check stockservice logs to see the retries and the failure
docker logs microservices26-stockservice-1 | grep "Negative quantity detected"
```

#### PowerShell
```powershell
# Reset scenarios to NORMAL
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/chaos/scenario" -ContentType "application/json" -Body '"NORMAL"'
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/chaos/scenario" -ContentType "application/json" -Body '"NORMAL"'

# Place an order with negative quantity
$order = @{ product = "Widget"; quantity = -5; price = 10.0 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/orders" -ContentType "application/json" -Body $order

# Wait for retries to exhaust (approx 10s)
Start-Sleep -Seconds 10

# Check stockservice logs to see the retries and the failure
docker logs microservices26-stockservice-1 | Select-String "Negative quantity detected"
```

#### Checking the DLQ in RabbitMQ Web Manager
To verify that the poison message has successfully been moved to the Dead Letter Queue:

1. Open your web browser and navigate to the **RabbitMQ Management Console**: [http://localhost:15672](http://localhost:15672)
2. Log in using the default credentials:
   - **Username**: `guest`
   - **Password**: `guest`
3. Click on the **Queues** (or **Queues and Streams**) tab.
4. You will see a list of configured queues. Locate the Dead Letter Queue: `stock.orders.dlq`.
5. Under the **Overview** columns, notice that `stock.orders.dlq` has `1` message in the **Ready** state, and the message rate graphs show activity.
6. Click on the `stock.orders.dlq` queue name to open its detail page.
7. Scroll down to the **Get messages** section:
   - Set **Requeue** to `Yes` (this keeps the message in the DLQ for future analysis/troubleshooting).
   - Set **Encoding** to `Auto string/utf-8`.
   - Click the **Get Message(s)** button.
8. Expand the fetched message to inspect its **Payload** and **Headers**:
   - **Payload**: Contains the raw JSON payload with `"quantity": -5`.
   - **Headers**: Contains metadata about the failures, such as `x-death` (an array documenting where, when, and why the message was dead-lettered, e.g. with reason `rejected`).

---

### 8. Scenario: Message Metadata Headers & Authentication
Illustrates how to enrich published events with metadata headers (such as `X-Sender-App` and `X-Auth-Token`) and inspect/validate them on the receiving service (`StockService`) to identify and authenticate the message source.

#### Implementation Details
- **Publisher (`OrderService`):** In `OutboxRelay.java`, the `RabbitTemplate` uses a custom `MessagePostProcessor` to dynamically attach application identity and authorization tokens to the outgoing message properties:
  ```java
  rabbitTemplate.convertAndSend(
      RabbitConfig.EXCHANGE_NAME,
      "order.placed",
      messagePayload,
      message -> {
          message.getMessageProperties().setHeader("X-Sender-App", "OrderService");
          message.getMessageProperties().setHeader("X-Auth-Token", "d2lkZ2V0X3NlY3JldF90b2tlbg==");
          return message;
      },
      correlationData
  );
  ```

- **Consumer (`StockService`):** In `StockListener.java`, the message headers are extracted dynamically using the Spring `@Header` annotation:
  ```java
  @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
  public void handleOrderPlaced(
          OrderPlacedEvent event,
          @Header(required = false, name = "x-delivery-attempt") Integer deliveryAttempt,
          @Header(required = false, name = "X-Sender-App") String senderApp,
          @Header(required = false, name = "X-Auth-Token") String authToken) {
      
      // Sender verification / authentication simulation
      if (authToken != null && !authToken.equals("d2lkZ2V0X3NlY3JldF90b2tlbg==")) {
          logger.warn("Authentication failed! Invalid token received from {}: {}", senderApp, authToken);
          throw new SecurityException("Invalid authentication token");
      } else if (senderApp != null) {
          logger.info("Authentication successful! Message received from trusted source: {} (Auth Token: {})", senderApp, authToken);
      }
      ...
  }
  ```

#### How to Verify
1. Place a normal order via `OrderService`.
2. Inspect the logs of the `stockservice`:
   - **Bash:**
     ```bash
     docker logs microservices26-stockservice-1 | grep "Authentication successful"
     ```
   - **PowerShell:**
     ```powershell
     docker logs microservices26-stockservice-1 | Select-String "Authentication successful"
     ```
   *Expected Log Output:*
   ```text
   INFO [...] o.e.s.service.StockListener : Authentication successful! Message received from trusted source: OrderService (Auth Token: d2lkZ2V0X3NlY3JldF90b2tlbg==)
   ```

---

## Observability
Logs include Trace IDs and scenario information:
`INFO [orderservice,5f2e...,a1b2...] ...`

