"""End-to-end smoke test for the running Docker Compose stack.

Posts one transaction to transaction-service, then checks that both consumers handled the
event: fraud-service screened it and ledger-service posted it, with the trial balance still
at zero.
"""
import stack_client as stack

PAYEE = "Smoke Test Store"


def main():
    transaction = stack.send_payment("Smoke Test Customer", PAYEE, 42.00, memo="Smoke test")
    print(f"Created transaction {transaction['id']}: published to {stack.kafka_position(transaction)}")
    assessment = stack.wait_for_assessment(transaction)
    print(f"fraud-service screened it: score {assessment['riskScore']} ({assessment['riskLevel']})")
    stack.wait_for_posting(transaction)
    print(f"ledger-service credited {PAYEE}")
    stack.wait_for_balanced_ledger()
    print("ledger-service trial balance nets to zero")


if __name__ == "__main__":
    main()
