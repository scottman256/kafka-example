"""Helpers shared by the scripts that drive the running Docker Compose stack.

The consumers handle each event asynchronously, so every check polls until it passes or times
out. Set TRANSACTION_SERVICE_URL, FRAUD_SERVICE_URL and LEDGER_SERVICE_URL to reach the services
somewhere else, for example through the web app's proxy at http://localhost:3000.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import date

TRANSACTION_SERVICE = os.environ.get("TRANSACTION_SERVICE_URL", "http://localhost:8081")
FRAUD_SERVICE = os.environ.get("FRAUD_SERVICE_URL", "http://localhost:8082")
LEDGER_SERVICE = os.environ.get("LEDGER_SERVICE_URL", "http://localhost:8083")
DEFAULT_TIMEOUT_SECONDS = 30


def send_payment(payer, payee, amount, source_bank="First National Bank", memo="", transaction_date=None):
    """Posts a transaction and returns transaction-service's answer, which includes its Kafka position."""
    body = {"transactionDate": (transaction_date or date.today()).isoformat(), "payer": payer, "payee": payee,
            "amount": amount, "memo": memo, "sourceBank": source_bank}
    request = urllib.request.Request(f"{TRANSACTION_SERVICE}/api/transactions", data=json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(request) as response:
        return json.load(response)


def wait_for_assessment(transaction, timeout_seconds=DEFAULT_TIMEOUT_SECONDS):
    """Waits until fraud-service has screened the transaction and returns its assessment."""
    return wait_for(f"{FRAUD_SERVICE}/api/risk/assessments/{transaction['id']}", lambda _: True, timeout_seconds)


def wait_for_posting(transaction, timeout_seconds=DEFAULT_TIMEOUT_SECONDS):
    """Waits until ledger-service has credited the transaction to its payee."""
    entries_url = f"{LEDGER_SERVICE}/api/ledger/accounts/{urllib.parse.quote(transaction['payee'])}/entries"
    is_posted = lambda lines: any(line["transactionId"] == transaction["id"] for line in lines)
    return wait_for(entries_url, is_posted, timeout_seconds)


def wait_for_balanced_ledger():
    """Waits until the trial balance nets to zero and returns it."""
    return wait_for(f"{LEDGER_SERVICE}/api/ledger/trial-balance", lambda trial_balance: trial_balance["balanced"])


def kafka_position(transaction):
    kafka = transaction["kafka"]
    return f"partition {kafka['partition']}, offset {kafka['offset']}"


def wait_for(url, condition, timeout_seconds=DEFAULT_TIMEOUT_SECONDS):
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        body = get_json_or_none(url)
        if body is not None and condition(body):
            return body
        time.sleep(1)
    sys.exit(f"Timed out after {timeout_seconds}s waiting for {url}")


def get_json_or_none(url):
    """The parsed response, or None while the service is down or returns an error."""
    try:
        with urllib.request.urlopen(url) as response:
            return json.load(response)
    except urllib.error.URLError:
        return None
