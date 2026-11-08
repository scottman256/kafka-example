"""Resilience demo: Kafka keeps a consumer's place while it is down.

Stops ledger-service, sends payments that fraud-service screens straight away, shows that
ledger-service's committed offset has fallen behind, then starts it again and times how long it
takes to catch up. Exits with an error if it doesn't, so CI runs it as a check too.

Run it from the machine running the stack: python scripts/resilience_demo.py
"""
import subprocess
import sys
import time
import uuid
from pathlib import Path

import stack_client as stack

REPO_ROOT = Path(__file__).resolve().parent.parent
CONSUMER = "ledger-service"
TOPIC = "transactions.created"
PAYMENTS_WHILE_DOWN = 3
CATCH_UP_TIMEOUT_SECONDS = 120


def main():
    payee = f"Resilience Store {uuid.uuid4().hex[:6]}"
    print(f"1. Stopping {CONSUMER}. fraud-service keeps running.")
    compose("stop", CONSUMER)
    try:
        transactions = send_payments_while_down(payee)
    finally:
        print(f"3. Starting {CONSUMER} again")
        restarted_at = time.monotonic()
        compose("start", CONSUMER)
    wait_for_catch_up(transactions, restarted_at)


def send_payments_while_down(payee):
    print(f"2. Sending {PAYMENTS_WHILE_DOWN} payments to {payee} while it is down")
    transactions = [stack.send_payment(f"Resilience Customer {number}", payee, 10.0 * number)
                    for number in range(1, PAYMENTS_WHILE_DOWN + 1)]
    for transaction in transactions:
        assessment = stack.wait_for_assessment(transaction)
        print(f"   {stack.kafka_position(transaction)}: screened by fraud-service ({assessment['riskLevel']})")
    lag = consumer_lag()
    print(f"   {CONSUMER} is {lag} events behind: Kafka holds them until it comes back")
    if lag < len(transactions):
        sys.exit(f"Expected {CONSUMER} to be at least {len(transactions)} events behind, but it is {lag}")
    return transactions


def wait_for_catch_up(transactions, restarted_at):
    for transaction in transactions:
        stack.wait_for_posting(transaction, CATCH_UP_TIMEOUT_SECONDS)
    seconds = time.monotonic() - restarted_at
    wait_until(lambda: consumer_lag() == 0, f"{CONSUMER} to commit its offsets")
    print(f"4. {CONSUMER} caught up from its committed offset in {seconds:.0f}s, including startup:")
    print(f"   all {len(transactions)} payments posted, lag 0")
    stack.wait_for_balanced_ledger()
    print("   The trial balance still nets to zero.")


def consumer_lag():
    """Events on the topic the consumer group hasn't committed yet. Kafka's group tool reports "-"
    for a partition the group has never committed on, so lag is worked out from the topic's end
    offsets instead. Such a partition then counts in full, since the consumers start from the
    earliest offset."""
    end_offsets = kafka_tool("kafka-get-offsets.sh", "--topic", TOPIC)
    total_end_offset = sum(int(line.rsplit(":", 1)[1]) for line in end_offsets.split())
    return total_end_offset - committed_offset()


def committed_offset():
    output = kafka_tool("kafka-consumer-groups.sh", "--describe", "--group", CONSUMER)
    rows = [line.split() for line in output.splitlines()]
    column = next(row for row in rows if "CURRENT-OFFSET" in row).index("CURRENT-OFFSET")
    return sum(int(row[column]) for row in rows if row and row[0] == CONSUMER and row[column].isdigit())


def kafka_tool(script, *arguments):
    return compose("exec", "-T", "kafka", f"/opt/kafka/bin/{script}", "--bootstrap-server", "kafka:9092", *arguments)


def wait_until(condition, description, timeout_seconds=stack.DEFAULT_TIMEOUT_SECONDS):
    deadline = time.monotonic() + timeout_seconds
    while not condition():
        if time.monotonic() > deadline:
            sys.exit(f"Timed out after {timeout_seconds}s waiting for {description}")
        time.sleep(1)


def compose(*arguments):
    result = subprocess.run(["docker", "compose", *arguments], cwd=REPO_ROOT, check=True,
                            capture_output=True, text=True)
    return result.stdout


if __name__ == "__main__":
    main()
