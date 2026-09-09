import type { APIRequestContext } from '@playwright/test';

import { ROLE, TOKEN, type RoleId } from './roles';

/**
 * Direct calls to apps/api, bypassing the UI, used ONLY as test setup/arrange for scenarios whose
 * UI path is unreachable today — supervisor assignment (UI-miniloan-010) has no Loan-Officer picker
 * and always fails from the screen (GAP-miniloan-017/018 are open on that; see
 * assignment-queue-known-gap.spec.ts for the characterization test of that actual behaviour). The
 * *act and assert* half of every scenario below still happens through the UI — this file only fast
 * forwards state a real user's clicks cannot reach yet.
 */
const API_BASE_URL = 'http://localhost:5000';

function authHeaders(roleId: RoleId): Record<string, string> {
  return { Authorization: `Bearer ${TOKEN[roleId]}` };
}

export interface ApplicantFields {
  fullName: string;
  age: number;
  monthlyIncome: number;
  currentEmploymentMonths: number;
  existingMonthlyDebt: number;
  requestedAmount: number;
  requestedTermMonths: number;
}

/** Comfortably inside every BR-miniloan-001/002/003/004 bound and lands Band A (DTI well under 50%). */
export const BAND_A_APPLICANT: ApplicantFields = {
  fullName: 'ผู้ทดสอบอัตโนมัติ',
  age: 30,
  monthlyIncome: 30000,
  currentEmploymentMonths: 24,
  existingMonthlyDebt: 0,
  requestedAmount: 60000,
  requestedTermMonths: 12,
};

export async function createDraft(request: APIRequestContext, fields: ApplicantFields): Promise<string> {
  const response = await request.post(`${API_BASE_URL}/applications`, {
    headers: authHeaders(ROLE.APPLICANT),
    data: fields,
  });
  if (!response.ok()) {
    throw new Error(`createDraft failed: ${response.status()} ${await response.text()}`);
  }
  const body = (await response.json()) as { id: string };
  return body.id;
}

export async function submit(request: APIRequestContext, applicationId: string): Promise<void> {
  const response = await request.post(`${API_BASE_URL}/applications/${applicationId}/submit`, {
    headers: authHeaders(ROLE.APPLICANT),
  });
  if (!response.ok()) {
    throw new Error(`submit failed: ${response.status()} ${await response.text()}`);
  }
}

/**
 * The UI's own assign-officer button always sends '' and is refused (GAP-miniloan-017) — this sends
 * the one demo Loan Officer identity directly, standing in for the picker no screen or endpoint has.
 */
export async function assignToLoanOfficer(request: APIRequestContext, applicationId: string): Promise<void> {
  const response = await request.post(`${API_BASE_URL}/applications/${applicationId}/assign`, {
    headers: authHeaders(ROLE.SUPERVISOR),
    data: { loanOfficerId: ROLE.LOAN_OFFICER },
  });
  if (!response.ok()) {
    throw new Error(`assign failed: ${response.status()} ${await response.text()}`);
  }
}

/** A Band-A applicant with a run-unique name, so a queue row can be found among other runs' leftovers. */
export function uniqueApplicant(label: string): ApplicantFields {
  return { ...BAND_A_APPLICANT, fullName: `${label} ${Date.now()}` };
}

/** Creates and submits (no assignment), returning its id — lands UnderReview via Band A. */
export async function createSubmittedApplication(
  request: APIRequestContext,
  fields: ApplicantFields,
): Promise<string> {
  const id = await createDraft(request, fields);
  await submit(request, id);
  return id;
}

/** Creates, submits and assigns a Band-A application, returning its id ready for review by the UI. */
export async function createAssignedApplication(
  request: APIRequestContext,
  fields: ApplicantFields = BAND_A_APPLICANT,
): Promise<string> {
  const id = await createDraft(request, fields);
  await submit(request, id);
  await assignToLoanOfficer(request, id);
  return id;
}

export interface DisbursedAccount {
  applicationId: string;
  accountNumber: string;
}

/**
 * Carries a fresh application all the way to Disbursed via the API, for specs whose subject is a
 * screen *downstream* of approval (repayment schedule, recording a payment) rather than the review
 * screen itself — loan-officer-review.spec.ts is what actually drives approve/reject/disburse
 * through the UI.
 */
export async function createDisbursedAccount(
  request: APIRequestContext,
  fields: ApplicantFields = BAND_A_APPLICANT,
): Promise<DisbursedAccount> {
  const applicationId = await createAssignedApplication(request, fields);

  const approveResponse = await request.post(`${API_BASE_URL}/applications/${applicationId}/approve`, {
    headers: authHeaders(ROLE.LOAN_OFFICER),
    data: { approvedAmount: fields.requestedAmount },
  });
  if (!approveResponse.ok()) {
    throw new Error(`approve failed: ${approveResponse.status()} ${await approveResponse.text()}`);
  }

  const disburseResponse = await request.post(`${API_BASE_URL}/applications/${applicationId}/disburse`, {
    headers: authHeaders(ROLE.LOAN_OFFICER),
  });
  if (!disburseResponse.ok()) {
    throw new Error(`disburse failed: ${disburseResponse.status()} ${await disburseResponse.text()}`);
  }
  const body = (await disburseResponse.json()) as { accountNumber: string };

  return { applicationId, accountNumber: body.accountNumber };
}
