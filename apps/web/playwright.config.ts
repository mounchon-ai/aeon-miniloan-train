import { defineConfig, devices } from '@playwright/test';

/**
 * Points at the docker-compose stack (docker/docker-compose.yml), not `ng serve` (port 4200) —
 * CLAUDE.md's Angular/Docker gotcha: the API base URL is baked into the web bundle at build time,
 * so these specs only work against `docker compose up`'s web (3000) + api (5000) + db, never a
 * dev-server instance pointed at a different API_URL.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    /**
     * `npm run e2e:demo` — the same specs, but headed and slowed down so a person watching the
     * screen can actually follow each fill/click, instead of chromium (headless, full speed) which
     * is what every other npm script and CI run uses. Kept as its own project rather than an env var
     * so "watch it run" is one command, not a flag to remember.
     */
    {
      name: 'demo',
      use: { ...devices['Desktop Chrome'], headless: false, launchOptions: { slowMo: 900 } },
    },
  ],
});
