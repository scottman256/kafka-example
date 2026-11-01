"""End-to-end smoke test for the running Docker Compose stack.

Posts one transaction to transaction-service, then checks that both consumers handled the
event: fraud-service screened it and ledger-service posted it, with the trial balance still
at zero. The consumers work asynchronously, so each check polls until it passes.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

TRANSACTION_SERVICE = os.environ.get("TRANSACTION_SERVICE_URL", "http://localhost:8081")
FRAUD_SERVICE = os.environ.get("FRAUD_SERVICE_URL", "http://localhost:8082")
LEDGER_SERVICE = os.environ.get("LEDGER_SERVICE_URL", "http://localhost:8083")
PAYEE = "Smoke Test Store"
TIMEOUT_SECONDS = 30


def main():
    transaction = create_transaction()
    print(f"Created transaction {transaction['id']}: {describe(transaction['kafka'])}")
    assessment = wait_for(f"{FRAUD_SERVICE}/api/risk/assessments/{transaction['id']}", lambda body: True)
    print(f"fraud-service screened it: score {assessment['riskScore']} ({assessment['riskLevel']})")
    wait_for(account_entries_url(PAYEE), lambda lines: any(line["transactionId"] == transaction["id"] for line in lines))
    print(f"ledger-service credited {PAYEE}")
    wait_for(f"{LEDGER_SERVICE}/api/ledger/trial-balance", lambda trial_balance: trial_balance["balanced"])
    print("ledger-service trial balance nets to zero")


def create_transaction():
    body = {"transactionDate": datetime.now(timezone.utc).date().isoformat(), "payer": "Smoke Test Customer",
            "payee": PAYEE, "amount": 42.00, "memo": "Smoke test", "sourceBank": "First National Bank"}
    request = urllib.request.Request(f"{TRANSACTION_SERVICE}/api/transactions", data=json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request) as response:
        return json.load(response)


def wait_for(url, condition):
    deadline = time.monotonic() + TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        body = get_json_or_none(url)
        if body is not None and condition(body):
            return body
        time.sleep(1)
    sys.exit(f"Timed out after {TIMEOUT_SECONDS}s waiting for {url}")


def get_json_or_none(url):
    try:
        with urllib.request.urlopen(url) as response:
            return json.load(response)
    except urllib.error.URLError:
        return None


def account_entries_url(account_name):
    return f"{LEDGER_SERVICE}/api/ledger/accounts/{urllib.parse.quote(account_name)}/entries"


def describe(kafka):
    return f"{kafka['status']} to {kafka['topic']} partition {kafka['partition']} offset {kafka['offset']}"


if __name__ == "__main__":
    main()
