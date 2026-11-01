# Bruno API collection

Requests for every API in the three services, one folder per service, ready to run against
the local Docker Compose stack.

## Set up

1. Download and install [Bruno](https://www.usebruno.com/downloads).
2. Start the stack from the repo root: `docker compose up -d --build`.
3. In Bruno, choose **Open Collection** and select this folder (`services/bruno`).
4. Select the **Local** environment in the top-right corner.
5. Run **transaction-service → Create transaction** first. It saves the new transaction's id
   and payee, which the lookup requests in the other folders use.

To run the whole collection at once, right-click it and choose **Run**.

## Authentication

The demo APIs don't require authentication, so there's nothing to set up today.

Auth is configured once for the whole collection (collection settings → **Auth**), and every
request inherits it. If you add a token, put it in a `.env` file in this folder and reference
it as `{{process.env.API_TOKEN}}`. That `.env` file is gitignored, so the token stays on your
machine. Don't paste tokens into `.bru` files, because those are committed.
