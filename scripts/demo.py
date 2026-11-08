"""Scripted demo: sends a set of payments that between them trip every fraud rule, then shows
how fraud-service and ledger-service each handled every one of them.

Run it against the running stack: python scripts/demo.py
"""
from datetime import date, timedelta
from typing import NamedTuple

import stack_client as stack


class Payment(NamedTuple):
    scenario: str
    payer: str
    payee: str
    amount: float
    source_bank: str
    days_from_today: int = 0


PAYMENTS = [
    Payment("Everyday purchase", "Alice Smith", "Bob's Hardware", 125.50, "First National Bank"),
    Payment("Just under $10,000", "Nina Patel", "Cash Exchange Co", 9500.00, "Harbor Savings Bank"),
    Payment("Large payment", "Grace Hopper", "Acme Motors", 15000.00, "Summit Credit Union"),
    Payment("Self-payment", "Mallory Black", "Mallory Black", 9500.00, "Pioneer Trust"),
    Payment("Future-dated", "Frank Ocean", "Landlord LLC", 1200.00, "Evergreen Community Bank", 21),
    *[Payment("Velocity burst", "Victor Quick", "Coffee Shop", 4.50, "Metro Federal Bank")] * 4,
]


def main():
    print(f"Sending {len(PAYMENTS)} payments to transaction-service...")
    sent = [(payment, send(payment)) for payment in PAYMENTS]
    print("Waiting for fraud-service and ledger-service to process each one...\n")
    rows = [describe(payment, transaction) for payment, transaction in sent]
    print_table(["Scenario", "Payment", "Published to Kafka", "Risk", "Rules triggered"], rows)
    print_ledger_summary()


def send(payment):
    transaction_date = date.today() + timedelta(days=payment.days_from_today)
    return stack.send_payment(payment.payer, payment.payee, payment.amount, payment.source_bank, "Demo",
                              transaction_date)


def describe(payment, transaction):
    """One table row, once both consumers have handled the transaction."""
    assessment = stack.wait_for_assessment(transaction)
    stack.wait_for_posting(transaction)
    rules = ", ".join(rule["rule"] for rule in assessment["triggeredRules"]) or "-"
    return [payment.scenario, f"{payment.payer} -> {payment.payee}  ${payment.amount:,.2f}",
            stack.kafka_position(transaction), f"{assessment['riskLevel']} {assessment['riskScore']}", rules]


def print_table(headers, rows):
    widths = [max(len(cell) for cell in column) for column in zip(headers, *rows)]
    for row in [headers, ["-" * width for width in widths], *rows]:
        print("  ".join(cell.ljust(width) for cell, width in zip(row, widths)))


def print_ledger_summary():
    trial_balance = stack.wait_for_balanced_ledger()
    print(f"\nLedger: {trial_balance['accountCount']} accounts; debit and credit balances both "
          f"${trial_balance['totalDebits']:,.2f}, so the trial balance nets to zero.")
    for bank in stack.get_json_or_none(f"{stack.LEDGER_SERVICE}/api/ledger/banks") or []:
        count = bank["transactionCount"]
        print(f"  {bank['bankName']}: ${bank['totalOutflow']:,.2f} paid out in {count} payment{'' if count == 1 else 's'}")


if __name__ == "__main__":
    main()
