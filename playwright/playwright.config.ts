import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: 'tests',
  // The tests share one running stack, and each panel shows only its newest rows.
  fullyParallel: false,
  workers: 1,
  forbidOnly: Boolean(process.env.CI),
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  // The consumers handle each event asynchronously and the panels poll every 2 seconds.
  expect: { timeout: 15_000 },
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
