import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApproverRoleSettingComponent } from './approver-role-setting.component';
import { API_BASE_URL } from '../../core/api-base-url';
import { ApproverRoleSettingView } from '../../core/services/approver-role-setting.service';

/**
 * UI-miniloan-008 (UC-miniloan-019 · AC-miniloan-082 · AC-miniloan-083 · AC-miniloan-089).
 *
 * <p><b>What only a web test can prove.</b> That a non-Loan-Officer is refused, and that the first
 * save and a later change produce different sentences, is decided in
 * {@code ApproverRoleSettingServiceTest} and measured there against the criteria. What is measured
 * here is the half a service test cannot see: that the page renders the API's sentence rather than
 * composing its own, that BR-miniloan-040@v1's unset state is announced instead of shown as a blank
 * select, and that a failed save keeps the value the person chose.
 *
 * <p><b>The confirmation is the sharpest of those.</b> AC-miniloan-082 and AC-miniloan-089 word it
 * differently and the difference depends on whether a row already existed. Both cases below flush
 * the same shape with a different {@code message}, and the page must print each one back unchanged —
 * a component that chose between the two sentences from its own loaded state would say "ตั้งค่า …"
 * to somebody who had just changed the setting.
 */
describe('ApproverRoleSettingComponent (UI-miniloan-008)', () => {
  let httpTesting: HttpTestingController;

  const URL = `${API_BASE_URL}/settings/closed-account-approver-role`;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApproverRoleSettingComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** BR-miniloan-040@v1's state: no row yet, so the API answers with an empty setting, not a 404. */
  const UNSET: ApproverRoleSettingView = {
    approverRole: null,
    updatedBy: null,
    updatedAt: null,
  };

  const SET: ApproverRoleSettingView = {
    approverRole: 'Supervisor',
    updatedBy: 'ROLE-002',
    updatedAt: '2026-09-01T10:00:00Z',
  };

  function render(view: ApproverRoleSettingView = UNSET) {
    const fixture = TestBed.createComponent(ApproverRoleSettingComponent);
    fixture.detectChanges();
    return fixture.whenStable().then(() => {
      httpTesting.expectOne(URL).flush(view);
      fixture.detectChanges();
      return fixture;
    });
  }

  const el = (fixture: { nativeElement: unknown }, id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`);

  const text = (fixture: { nativeElement: unknown }, id: string) =>
    el(fixture, id)?.textContent?.trim();

  it('renders every control mock minted for this screen', async () => {
    const fixture = await render();

    for (const id of [
      'ui-miniloan-008-ent-011-approver-role',
      'ui-miniloan-008-ent-011-updated-by',
      'ui-miniloan-008-ent-011-updated-at',
      'ui-miniloan-008-save-setting',
    ]) {
      expect(el(fixture, id)).not.toBeNull();
    }
  });

  /**
   * ENT-011's declared enum, and only it. The three values come from datamodel.json; a fourth option
   * here would be a role nobody declared, and the empty one is the unset state rather than a choice.
   */
  it('offers exactly the three roles design declared, plus the unset option', async () => {
    const fixture = await render();
    const select = el(fixture, 'ui-miniloan-008-ent-011-approver-role') as HTMLSelectElement;

    expect([...select.options].map((option) => option.value)).toEqual([
      '',
      'LoanOfficer',
      'Supervisor',
      'Operations',
    ]);
  });

  /**
   * screens.json empty · BR-miniloan-040@v1 — "แสดงค่าว่างพร้อมข้อความเตือนว่าคำขอปรับปรุงบัญชีจะถูก
   * ปฏิเสธทั้งหมดจนกว่าจะตั้งค่า". The warning is what separates a system nobody has configured from
   * one where somebody simply has not picked yet; both look like an empty select.
   */
  it('announces the unset state instead of showing only a blank select', async () => {
    const fixture = await render(UNSET);
    const select = el(fixture, 'ui-miniloan-008-ent-011-approver-role') as HTMLSelectElement;

    expect(fixture.componentInstance.isUnset()).toBe(true);
    expect(select.value).toBe('');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'คำขอปรับปรุงบัญชีที่ปิดแล้วจะถูกปฏิเสธทั้งหมดจนกว่าจะตั้งค่า',
    );
  });

  /** A setting already in force shows its value and its audit line, both as the API sent them. */
  it('shows the value in force and who last changed it', async () => {
    const fixture = await render(SET);
    const select = el(fixture, 'ui-miniloan-008-ent-011-approver-role') as HTMLSelectElement;

    expect(fixture.componentInstance.isUnset()).toBe(false);
    expect(select.value).toBe('Supervisor');
    expect(text(fixture, 'ui-miniloan-008-ent-011-updated-by')).toBe('ROLE-002');
    expect(text(fixture, 'ui-miniloan-008-ent-011-updated-at')).toBe('2026-09-01T10:00:00Z');
  });

  /**
   * AC-miniloan-082 — the first time it is set. The sentence asserted is the API's, word for word,
   * and the page did not compose it.
   */
  it('saves the first setting and prints the API sentence for it', async () => {
    const fixture = await render(UNSET);

    fixture.componentInstance.approverRole.setValue('Supervisor');
    (el(fixture, 'ui-miniloan-008-save-setting') as HTMLButtonElement).click();

    const request = httpTesting.expectOne(URL);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ approverRole: 'Supervisor' });
    request.flush({
      approverRole: 'Supervisor',
      updatedBy: 'ROLE-002',
      updatedAt: '2026-09-07T04:00:00Z',
      message: 'ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น Supervisor เรียบร้อย',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe(
      'ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น Supervisor เรียบร้อย',
    );
    expect(fixture.componentInstance.isUnset()).toBe(false);
    expect(text(fixture, 'ui-miniloan-008-ent-011-updated-at')).toBe('2026-09-07T04:00:00Z');
  });

  /**
   * AC-miniloan-089 — a change, and a DIFFERENT sentence. Same request shape, same component, and
   * the only thing that decides which sentence appears is what the API returned. That is the point:
   * whether a row existed is not a fact the browser holds.
   */
  it('prints the change sentence, not the first-time one, when the API says so', async () => {
    const fixture = await render(SET);

    fixture.componentInstance.approverRole.setValue('Operations');
    (el(fixture, 'ui-miniloan-008-save-setting') as HTMLButtonElement).click();

    httpTesting.expectOne(URL).flush({
      approverRole: 'Operations',
      updatedBy: 'ROLE-002',
      updatedAt: '2026-09-07T04:05:00Z',
      message: 'เปลี่ยน role ผู้อนุมัติเป็น Operations เรียบร้อย',
    });
    fixture.detectChanges();

    expect(fixture.componentInstance.outcome()).toBe('เปลี่ยน role ผู้อนุมัติเป็น Operations เรียบร้อย');
    expect(fixture.componentInstance.current()).toBe('Operations');
  });

  /**
   * AC-miniloan-083 · screens.json unauthorized — "ทั้งจากหน้าจอ และด้วยการเรียก API ตั้งค่าโดยตรง …
   * ทั้งสองทางถูกปฏิเสธ". The read is issued for whoever opens the page and the API's own sentence is
   * what appears; the form is not rendered at all, so no value can be chosen to send.
   */
  it('shows the API refusal and no form when the caller is not a Loan Officer', async () => {
    const fixture = TestBed.createComponent(ApproverRoleSettingComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    httpTesting.expectOne(URL).flush(
      {
        code: 'APPROVER_ROLE_SETTING_LOAN_OFFICER_ONLY',
        message: 'ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer',
      },
      { status: 403, statusText: 'Forbidden' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.notAllowed()).toBe(
      'ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer',
    );
    expect(el(fixture, 'ui-miniloan-008-ent-011-approver-role')).toBeNull();
    expect(el(fixture, 'ui-miniloan-008-save-setting')).toBeNull();
  });

  /**
   * screens.json error — "แสดงข้อความบันทึกไม่สำเร็จ พร้อมปุ่มลองใหม่ **คงค่าที่เลือกไว้**". The last
   * clause is the one worth a test: a component that reloaded on failure would silently throw away
   * the choice and make the person pick again.
   */
  it('keeps the chosen value when the save fails, and offers a retry', async () => {
    const fixture = await render(UNSET);

    fixture.componentInstance.approverRole.setValue('Operations');
    (el(fixture, 'ui-miniloan-008-save-setting') as HTMLButtonElement).click();
    httpTesting.expectOne(URL).flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).not.toBeNull();
    expect(fixture.componentInstance.approverRole.value).toBe('Operations');
    expect(fixture.componentInstance.current()).toBeNull();
    expect(
      (el(fixture, 'ui-miniloan-008-ent-011-approver-role') as HTMLSelectElement).value,
    ).toBe('Operations');
  });

  /**
   * screens.json loading — "แสดง spinner บนปุ่มระหว่างบันทึก". The button is disabled while the save
   * is in flight, which is what stops a second PUT from being sent on a double press.
   */
  it('disables the save button while the save is in flight', async () => {
    const fixture = await render(UNSET);

    fixture.componentInstance.approverRole.setValue('Supervisor');
    (el(fixture, 'ui-miniloan-008-save-setting') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect((el(fixture, 'ui-miniloan-008-save-setting') as HTMLButtonElement).disabled).toBe(true);

    httpTesting.expectOne(URL).flush({
      approverRole: 'Supervisor',
      updatedBy: 'ROLE-002',
      updatedAt: '2026-09-07T04:00:00Z',
      message: 'ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น Supervisor เรียบร้อย',
    });
    fixture.detectChanges();

    expect((el(fixture, 'ui-miniloan-008-save-setting') as HTMLButtonElement).disabled).toBe(false);
  });

  /**
   * BR-miniloan-039@v1 is the API's rule, not this page's. An empty selection is SENT and refused
   * there — AC-miniloan-083's "การปฏิเสธเกิดที่ฝั่ง API" applies to every rule on this screen, and a
   * browser-side guard would hide the refusal the criteria measure.
   */
  it('sends an empty selection rather than blocking it, and shows the API refusal', async () => {
    const fixture = await render(UNSET);

    (el(fixture, 'ui-miniloan-008-save-setting') as HTMLButtonElement).click();
    const request = httpTesting.expectOne(URL);
    expect(request.request.body).toEqual({ approverRole: '' });
    request.flush(
      {
        code: 'APPROVER_ROLE_REQUIRED',
        message: 'ต้องระบุ role ผู้อนุมัติ — ล้างค่าที่ตั้งไว้ให้ว่างไม่ได้',
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.failed()).toBe(
      'ต้องระบุ role ผู้อนุมัติ — ล้างค่าที่ตั้งไว้ให้ว่างไม่ได้',
    );
    expect(fixture.componentInstance.current()).toBeNull();
  });
});
