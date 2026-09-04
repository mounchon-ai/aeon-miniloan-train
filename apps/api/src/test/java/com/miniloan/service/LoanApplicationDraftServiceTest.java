package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanApplicationDraftServiceTest {

    @Mock private LoanApplicationRepository repository;

    // AC-miniloan-032: only fullName filled in — saves successfully as Draft, no field-missing
    // rejection (BR-miniloan-008@v1 — the draft stage does not check completeness).
    @Test
    void savesANewDraftWithOnlyOneFieldFilledIn() {
        LoanApplicationDraftService service = new LoanApplicationDraftService(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LoanApplicationDraftService.DraftFields fields =
                new LoanApplicationDraftService.DraftFields(
                        "สมชาย ใจดี", null, null, null, null, null, null);

        LoanApplication saved = service.saveNewDraft("ROLE-001", fields);

        assertThat(saved.getStatus()).isEqualTo(LoanApplication.Status.Draft);
        assertThat(saved.getFullName()).isEqualTo("สมชาย ใจดี");
        assertThat(saved.getAge()).isNull();
        assertThat(saved.getRequestedAmount()).isNull();
    }

    // AC-miniloan-033: a requestedAmount below BR-miniloan-004@v1's 10,000 minimum is still
    // saved as entered — the draft stage does not check the full business rule, only submit does.
    @Test
    void savesANewDraftWithAnOutOfRangeAmountAsEntered() {
        LoanApplicationDraftService service = new LoanApplicationDraftService(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LoanApplicationDraftService.DraftFields fields =
                new LoanApplicationDraftService.DraftFields(
                        null, null, null, null, null, new BigDecimal("5000"), null);

        LoanApplication saved = service.saveNewDraft("ROLE-001", fields);

        assertThat(saved.getStatus()).isEqualTo(LoanApplication.Status.Draft);
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void updatingAnExistingDraftAppliesTheNewFieldsAndRoundsMoneyHalfUp() {
        LoanApplicationDraftService service = new LoanApplicationDraftService(repository);
        LoanApplication existing = new LoanApplication("ROLE-001");
        UUID id = existing.getId();
        when(repository.findByIdAndApplicantId(id, "ROLE-001")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LoanApplicationDraftService.DraftFields fields =
                new LoanApplicationDraftService.DraftFields(
                        "สมหญิง มีสุข",
                        30,
                        new BigDecimal("20000.005"),
                        24,
                        new BigDecimal("1500"),
                        new BigDecimal("100000"),
                        12);

        LoanApplication saved = service.updateDraft(id, "ROLE-001", fields);

        assertThat(saved.getFullName()).isEqualTo("สมหญิง มีสุข");
        // round-half-up at the point it occurs (CLAUDE.md) — 20000.005 rounds up to 20000.01.
        assertThat(saved.getMonthlyIncome()).isEqualByComparingTo("20000.01");
    }

    @Test
    void updatingAMissingDraftThrowsNotFound() {
        LoanApplicationDraftService service = new LoanApplicationDraftService(repository);
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndApplicantId(id, "ROLE-001")).thenReturn(Optional.empty());
        LoanApplicationDraftService.DraftFields fields =
                new LoanApplicationDraftService.DraftFields(
                        "x", null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateDraft(id, "ROLE-001", fields))
                .isInstanceOf(LoanApplicationDraftService.DraftNotFoundException.class);
    }

    // API-002 is scoped to editing a Draft ("แก้ไขร่างใบสมัคร ... สถานะ Draft") — once submitted,
    // editing through this endpoint is refused rather than silently mutating a locked application.
    @Test
    void updatingAnApplicationThatIsNoLongerADraftThrowsNotEditable() {
        LoanApplicationDraftService service = new LoanApplicationDraftService(repository);
        LoanApplication submitted = new LoanApplication("ROLE-001");
        submitted.applyDraftFields("x", 30, null, null, null, null, null);
        setStatus(submitted, LoanApplication.Status.Submitted);
        UUID id = submitted.getId();
        when(repository.findByIdAndApplicantId(id, "ROLE-001")).thenReturn(Optional.of(submitted));
        LoanApplicationDraftService.DraftFields fields =
                new LoanApplicationDraftService.DraftFields(
                        "y", null, null, null, null, null, null);

        assertThatThrownBy(() -> service.updateDraft(id, "ROLE-001", fields))
                .isInstanceOf(LoanApplicationDraftService.DraftNotEditableException.class);
    }

    @Test
    void savingCapturesTheApplicantIdOnTheNewRow() {
        LoanApplicationDraftService service = new LoanApplicationDraftService(repository);
        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        LoanApplicationDraftService.DraftFields fields =
                new LoanApplicationDraftService.DraftFields(
                        null, null, null, null, null, null, null);

        service.saveNewDraft("ROLE-001", fields);

        verify(repository).save(any());
        assertThat(captor.getValue().getApplicantId()).isEqualTo("ROLE-001");
    }

    // AC-miniloan-115: the shared BR-miniloan-004@v1 contract FE-miniloan-005's submit flow
    // enforces — a machine-readable code plus the exact Thai message, never a bare code.
    @Test
    void requestedAmountBelowTheMinimumReturnsTheMachineReadableAndThaiMessageContract() {
        Optional<LoanApplication.RangeViolation> violation =
                LoanApplication.validateRequestedAmount(new BigDecimal("5000"));

        assertThat(violation).isPresent();
        assertThat(violation.get().code()).isEqualTo("LOAN_AMOUNT_OUT_OF_RANGE");
        assertThat(violation.get().message())
                .isEqualTo("จำนวนเงินกู้ที่ขอต้องอยู่ระหว่าง 10,000 – 1,000,000 บาท");
    }

    // AC-miniloan-023/024/025: within range, and both boundaries inclusive, are not violations.
    @Test
    void requestedAmountWithinRangeIncludingBothBoundariesIsNotAViolation() {
        assertThat(LoanApplication.validateRequestedAmount(new BigDecimal("100000"))).isEmpty();
        assertThat(LoanApplication.validateRequestedAmount(LoanApplication.MIN_REQUESTED_AMOUNT))
                .isEmpty();
        assertThat(LoanApplication.validateRequestedAmount(LoanApplication.MAX_REQUESTED_AMOUNT))
                .isEmpty();
    }

    private static void setStatus(LoanApplication application, LoanApplication.Status status) {
        try {
            var field = LoanApplication.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(application, status);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
