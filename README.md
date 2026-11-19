# Kafka Order + Payment Demo (Spring Boot)

Two small Spring Boot services that talk to each other through Kafka.

```
POST /orders ──► order-service ──(orders.created)──► payment-service
                      ▲                                    │
                      └────────(payments.processed)────────┘

Bad messages: payment-service retries 3 times, then moves them to orders.created.DLT
```

- **order-service** (port 8081): REST API to create and read orders. Saves the order, then publishes an `OrderCreated` event. Listens for payment results and marks the order `PAID` or `FAILED`.
- **payment-service** (port 8082): Listens for `OrderCreated`, applies a fake rule (amount above 10,000 fails), saves the payment, publishes the result.

## What this shows

- Event-driven communication between two services (producer, consumer, consumer groups)
- Publishing to Kafka only **after** the database commit
- Idempotent consumers: the same message delivered twice does not charge or update twice
- Retry + dead-letter topic for messages that cannot be processed
- Unit tests with Mockito

## Run it

You need Java 17, Maven and Docker.

1. Start Kafka:
   ```bash
   docker compose up -d
   ```
2. Start the payment service (terminal 1):
   ```bash
   cd payment-service && mvn spring-boot:run
   ```
3. Start the order service (terminal 2):
   ```bash
   cd order-service && mvn spring-boot:run
   ```

## Try it

Create an order that will be paid:

```bash
curl -s -X POST localhost:8081/orders -H "Content-Type: application/json" \
  -d '{"customerId":"cust-1","amount":500}'
```

Copy the `id` from the response and check it a second later. Status should change from `CREATED` to `PAID`:

```bash
curl -s localhost:8081/orders/<id>
```

Create an order that will fail (amount above 10,000):

```bash
curl -s -X POST localhost:8081/orders -H "Content-Type: application/json" \
  -d '{"customerId":"cust-2","amount":15000}'
```

Its status becomes `FAILED`.

## See the dead-letter topic work

Send a message that is not valid JSON straight to the topic:

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic orders.created
```

Type `not-json` and press Enter, then Ctrl+C. In the payment-service logs you will see it fail 4 times (1 try + 3 retries). Then read the dead-letter topic:

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic orders.created.DLT --from-beginning
```

## Run the tests

```bash
cd order-service && mvn test
cd ../payment-service && mvn test
```

## Known limits and next steps

- Events are published after commit, but if the app crashes between commit and send, the event is lost. The fix is the **transactional outbox pattern**.
- Tests use mocks. Adding an integration test with **Testcontainers (Kafka)** would be the next step.
- Both services use in-memory H2, so data resets on restart. Swap in PostgreSQL for a real setup.
- Each service has its own copy of the event classes. A shared contract (or schema registry) would be the next step at scale.
