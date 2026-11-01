import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// `npm run dev` sends API calls straight to the services; in Docker, nginx does this instead.
const apiProxy = {
  '/api/transactions': 'http://localhost:8081',
  '/api/risk': 'http://localhost:8082',
  '/api/ledger': 'http://localhost:8083',
};

export default defineConfig({
  plugins: [react()],
  server: { port: 3000, proxy: apiProxy },
  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    coverage: {
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/test/**', 'src/**/*.test.{ts,tsx}'],
      reporter: ['text', 'html', 'json-summary'],
      thresholds: { lines: 90 },
    },
  },
});
