import { test as base, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Every spec imports `test`/`expect` from here instead of '@playwright/test' directly, so every
 * test — pass or fail — leaves a screenshot under e2e/screenshots/<spec file>/<test title>.png.
 * One folder per spec file (= one folder per test suite), unlike Playwright's own test-results/
 * dirs which are per-test, failure/trace-only, and named by the full title path rather than
 * grouped by suite.
 *
 * Set PW_SCREENSHOT=0 to run the same specs with capture switched off (see docs/manual/
 * playwright-testing.md §3.1/§4) — nothing else about the run changes.
 */
export const test = base;
export { expect };

const CAPTURE_ENABLED = process.env.PW_SCREENSHOT !== '0';

test.afterEach(async ({ page }, testInfo) => {
  if (!CAPTURE_ENABLED) {
    return;
  }

  const suiteName = path.basename(testInfo.file).replace(/\.spec\.ts$/, '');
  const safeTitle = testInfo.title.replace(/[\\/:*?"<>|]+/g, '_').trim();
  const dir = path.join(__dirname, '..', 'screenshots', suiteName);
  fs.mkdirSync(dir, { recursive: true });

  try {
    await page.screenshot({ path: path.join(dir, `${safeTitle}.png`), fullPage: true });
  } catch {
    // Page already closed/navigated away after a hard failure — nothing left to capture.
  }
});
