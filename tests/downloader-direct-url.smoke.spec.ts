import { expect, test, type Page } from '@playwright/test';

type Account = {
  email: string;
  password: string;
};

type AuthSession = {
  authenticated: boolean;
  email?: string | null;
  dailyQuota?: number | null;
  jobsToday?: number | null;
};

type AuthCsrfPayload = {
  token: string;
  headerName: string;
};

type JobSummary = {
  id: string;
};

type SourceRequest = {
  id: string;
  jobs: JobSummary[];
};

type JobDetail = {
  id: string;
  state: string;
  progressPercent: number;
  downloadSpeed?: string | null;
  eta?: string | null;
  errorMessage?: string | null;
  logs: string[];
};

type JobFile = {
  name: string;
  downloadUrl?: string | null;
  size: number;
};

type ApiEnvelope<T> = {
  success: boolean;
  message?: string | null;
  data: T;
  error?: {
    message?: string | null;
  } | null;
};

const APP_PATH = process.env.E2E_APP_PATH ?? '/app/downloader';
const SOURCE_URL = process.env.E2E_TEST_URL ?? 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';
const PRIMARY_ACCOUNT: Account = {
  email: process.env.E2E_EMAIL ?? 'user@test.com',
  password: process.env.E2E_PASSWORD ?? 'user',
};
const FALLBACK_ACCOUNT: Account = {
  email: process.env.E2E_PUBLISHER_EMAIL ?? 'pub@test.com',
  password: process.env.E2E_PUBLISHER_PASSWORD ?? 'pub',
};
const TERMINAL_STATES = new Set(['COMPLETED', 'FAILED', 'BLOCKED']);

test('smoke: submit direct URL and reach a terminal job state', async ({ page }, testInfo) => {
  test.slow();

  await openWorkspace(page);

  const submitter = await pickSubmissionAccount(page);
  await testInfo.attach('active-account.json', {
    body: JSON.stringify(submitter, null, 2),
    contentType: 'application/json',
  });

  const submission = await submitDirectUrl(page, SOURCE_URL);
  await testInfo.attach('source-request-response.json', {
    body: JSON.stringify(submission.envelope, null, 2),
    contentType: 'application/json',
  });

  expect(submission.envelope.success).toBeTruthy();
  expect(submission.envelope.data.id).toBeTruthy();
  expect(submission.envelope.data.jobs.length).toBeGreaterThan(0);

  const sourceRequestId = submission.envelope.data.id;
  const jobId = submission.envelope.data.jobs[0]?.id;
  expect(jobId).toBeTruthy();

  await expect(page.getByText(`Request: ${sourceRequestId.slice(0, 10)}`)).toBeVisible();
  await expect(page.getByText(`Job: ${jobId!.slice(0, 10)}`)).toBeVisible();

  await page.locator('button', { hasText: `Job: ${jobId!.slice(0, 10)}` }).first().click();
  await expect(page.getByText(`ID: ${jobId!.slice(0, 8)}`)).toBeVisible();

  const finalJob = await waitForTerminalJob(page, jobId!, 3 * 60 * 1000);
  await testInfo.attach('final-job.json', {
    body: JSON.stringify(finalJob, null, 2),
    contentType: 'application/json',
  });

  if (finalJob.state === 'COMPLETED') {
    await expect(page.getByText('COMPLETED')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'File tải xuống' })).toBeVisible({ timeout: 15_000 });

    const files = await fetchJobFiles(page, jobId!);
    await testInfo.attach('job-files.json', {
      body: JSON.stringify(files, null, 2),
      contentType: 'application/json',
    });

    expect(files.length).toBeGreaterThan(0);
    expect(files.some((file) => Boolean(file.downloadUrl))).toBeTruthy();

    const primaryDownload = page.getByTestId('job-primary-download');
    await expect(primaryDownload).toBeVisible();

    const [download] = await Promise.all([page.waitForEvent('download'), primaryDownload.click()]);

    await testInfo.attach('download-suggested-filename.txt', {
      body: download.suggestedFilename(),
      contentType: 'text/plain',
    });

    expect(download.suggestedFilename()).toBeTruthy();
    return;
  }

  const highlightedLogs = finalJob.logs.slice(-10);
  await testInfo.attach('terminal-log-tail.txt', {
    body: highlightedLogs.join('\n'),
    contentType: 'text/plain',
  });

  const failurePrefix = finalJob.state === 'BLOCKED' ? 'Job was blocked' : 'Job failed';
  throw new Error(
    `${failurePrefix}: ${finalJob.errorMessage ?? 'No error message returned'}\n\n${highlightedLogs.join('\n')}`,
  );
});

async function openWorkspace(page: Page) {
  const response = await page.goto(APP_PATH, { waitUntil: 'domcontentloaded' });
  expect(response, `Cannot open ${APP_PATH}. Is the frontend dev server running?`).not.toBeNull();
  expect(response?.ok(), `Cannot open ${APP_PATH}. HTTP ${response?.status()}`).toBeTruthy();
  await expect(page.getByRole('heading', { name: 'Bàn làm việc Tải Video' })).toBeVisible();
}

async function pickSubmissionAccount(page: Page) {
  const accounts: Account[] = [PRIMARY_ACCOUNT];
  if (
    PRIMARY_ACCOUNT.email === 'user@test.com' &&
    PRIMARY_ACCOUNT.password === 'user' &&
    FALLBACK_ACCOUNT.email &&
    FALLBACK_ACCOUNT.password
  ) {
    accounts.push(FALLBACK_ACCOUNT);
  }

  for (const account of accounts) {
    await loginViaApi(page, account);
    await page.goto(APP_PATH, { waitUntil: 'domcontentloaded' });

    const session = await fetchSession(page);
    expect(session.authenticated, `${account.email} did not create an authenticated session`).toBeTruthy();
    await expect(page.getByRole('banner').getByText(account.email)).toBeVisible();

    const jobsToday = session.jobsToday ?? 0;
    const dailyQuota = session.dailyQuota ?? 0;
    if (dailyQuota > 0 && jobsToday >= dailyQuota && account !== accounts[accounts.length - 1]) {
      await logoutViaApi(page);
      continue;
    }

    return account;
  }

  throw new Error('No account is currently available for smoke submission.');
}

async function loginViaApi(page: Page, account: Account) {
  const result = await page.evaluate(async (credentials) => {
    const csrfResponse = await fetch('/api/v1/auth/csrf', {
      credentials: 'include',
    });

    if (!csrfResponse.ok) {
      return {
        ok: false,
        status: csrfResponse.status,
        message: 'Unable to fetch CSRF token',
      };
    }

    const csrfEnvelope = (await csrfResponse.json()) as ApiEnvelope<AuthCsrfPayload>;
    const csrf = csrfEnvelope.data;

    const loginResponse = await fetch('/api/v1/auth/login', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({
        email: credentials.email,
        password: credentials.password,
      }),
    });

    const loginEnvelope = (await loginResponse.json().catch(() => null)) as ApiEnvelope<AuthSession> | null;

    return {
      ok: loginResponse.ok && Boolean(loginEnvelope?.success),
      status: loginResponse.status,
      message: loginEnvelope?.error?.message ?? loginEnvelope?.message ?? null,
    };
  }, account);

  expect(result.ok, `Login failed for ${account.email}. ${result.message ?? `HTTP ${result.status}`}`).toBeTruthy();
}

async function logoutViaApi(page: Page) {
  await page.evaluate(async () => {
    const csrfResponse = await fetch('/api/v1/auth/csrf', {
      credentials: 'include',
    });

    if (!csrfResponse.ok) {
      return;
    }

    const csrfEnvelope = (await csrfResponse.json()) as ApiEnvelope<AuthCsrfPayload>;
    const csrf = csrfEnvelope.data;

    await fetch('/api/v1/auth/logout', {
      method: 'POST',
      credentials: 'include',
      headers: {
        [csrf.headerName]: csrf.token,
      },
    });
  });
}

async function fetchSession(page: Page) {
  const sessionEnvelope = await page.evaluate(async () => {
    const response = await fetch('/api/v1/auth/me', {
      credentials: 'include',
    });
    return (await response.json()) as ApiEnvelope<AuthSession>;
  });

  return sessionEnvelope.data;
}

async function submitDirectUrl(page: Page, sourceUrl: string) {
  await page.getByTestId('source-url-input').fill(sourceUrl);
  await page.getByTestId('platform-select').selectOption('AUTO');
  await page.getByTestId('source-type-select').selectOption('AUTO');
  await page.getByTestId('download-type-select').selectOption('VIDEO');
  await page.getByTestId('format-select').selectOption('mp4');
  await page.getByTestId('quality-select').selectOption('best');

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/api/v1/source-requests') && response.request().method() === 'POST',
  );

  await page.getByTestId('submit-source-request').click();
  const response = await responsePromise;
  const envelope = (await response.json()) as ApiEnvelope<SourceRequest>;

  if (!response.ok || !envelope.success) {
    throw new Error(
      `Source request submission failed. ${envelope.error?.message ?? envelope.message ?? `HTTP ${response.status()}`}`,
    );
  }

  return {
    response,
    envelope,
  };
}

async function waitForTerminalJob(page: Page, jobId: string, timeoutMs: number) {
  const startedAt = Date.now();
  let lastSeen: JobDetail | null = null;

  while (Date.now() - startedAt < timeoutMs) {
    lastSeen = await fetchJob(page, jobId);
    if (TERMINAL_STATES.has(lastSeen.state)) {
      return lastSeen;
    }

    await page.waitForTimeout(2_000);
  }

  throw new Error(
    `Job ${jobId} did not reach a terminal state within ${Math.round(timeoutMs / 1000)}s. Last state: ${lastSeen?.state ?? 'unknown'}`,
  );
}

async function fetchJob(page: Page, jobId: string) {
  const jobEnvelope = await page.evaluate(async (currentJobId) => {
    const response = await fetch(`/api/v1/jobs/${currentJobId}`, {
      credentials: 'include',
    });
    return (await response.json()) as ApiEnvelope<JobDetail>;
  }, jobId);

  if (!jobEnvelope.success) {
    throw new Error(jobEnvelope.error?.message ?? `Cannot fetch job ${jobId}`);
  }

  return jobEnvelope.data;
}

async function fetchJobFiles(page: Page, jobId: string) {
  const filesEnvelope = await page.evaluate(async (currentJobId) => {
    const response = await fetch(`/api/v1/jobs/${currentJobId}/files`, {
      credentials: 'include',
    });
    return (await response.json()) as ApiEnvelope<JobFile[]>;
  }, jobId);

  if (!filesEnvelope.success) {
    throw new Error(filesEnvelope.error?.message ?? `Cannot fetch files for job ${jobId}`);
  }

  return filesEnvelope.data;
}
