# Playwright end-to-end tests

Browser tests that drive the web app against the whole running stack: sending payments,
using the demo scenarios, and checking each one reaches all three live panels.

## Run them

1. Start the stack from the repo root and wait until every container is healthy:

   ```bash
   docker compose up -d --build --wait
   ```

2. Install the test runner and its browser (first time only; needs Node.js 22.12 or later):

   ```bash
   cd playwright
   npm ci
   npx playwright install chromium
   ```

3. Run the tests:

   ```bash
   npx playwright test
   ```

They test http://localhost:3000 by default. To point them somewhere else, set `BASE_URL`, for
example `BASE_URL=http://localhost:3001 npx playwright test`.

Each run adds a few transactions to the stack. The tests use unique names, so they pass on a
stack that already has data. To start from a clean stack, run `docker compose down -v` first.

## When a test fails

- `npx playwright test --headed` runs the tests in a visible browser.
- `npx playwright test --ui` opens Playwright's interactive mode.
- A failed test keeps a screenshot and a trace in `test-results/`. Open the trace with
  `npx playwright show-trace <path to trace.zip>`.

## In CI

The **End-to-end (Playwright)** job runs these tests against a freshly built stack for every
pull request that changes anything other than docs. When it fails, the HTML report and traces
are attached to the run as the `playwright-report` artifact.
