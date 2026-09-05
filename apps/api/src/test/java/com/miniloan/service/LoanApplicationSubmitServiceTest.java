package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.domain.CreditAssessment.Band;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationSubmitService.DuplicateCommandException;
import com.miniloan.service.LoanApplicationSubmitService.IncompleteApplicationException;
import com.miniloan.service.LoanApplicationSubmitService.OutOfRangeException;
import com.miniloan.service.LoanApplicationSubmitService.ViewNotPermittedException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ยื่นใบสมัคร (UC-miniloan-002) end to end against a real database, because two of the things under
 * test here only exist there: the unique constraint that refuses a repeat (AC-miniloan-134) and the
 * rollback that lets a rejected submission be retried.
 */
@SpringBootTest
class LoanApplicationSubmitServiceTest {

    private static final String APPLICANT = "ROLE-001";

    @Autowired private LoanApplicationSubmitService submitService;
    @Autowired private LoanApplicationDraftService draftService;
    @Autowired private LoanApplicationRepository applications;
    @Autowired private CreditAssessmentRepository assessments;
    @Autowired private IdempotencyKeyRepository idempotencyKeys;

    @BeforeEach
    void clean() {
        assessments.deleteAll();
        idempotencyKeys.deleteAll();
        applications.deleteAll();
    }

    private UUID draft(DraftFields fields) {
        return draftService.saveNewDraft(APPLICANT, fields).getId();
    }

    /**
     * AC-miniloan-015/019/029/034: complete, eligible, assessed, and moved on to UnderReview with
     * nobody pressing anything (AC-miniloan-063). GD-miniloan-003 row 1 puts this profile at 66.68%,
     * which is Band B — over the 50% line but inside the 70% one.
     */
    @Test
    void submittingACompleteApplicationAssessesItAndMovesItOn() {
        UUID id = draft(complete());

        var result = submitService.submit(id, APPLICANT);

        assertThat(result.application().getStatus()).isEqualTo(LoanApplication.Status.UnderReview);
        assertThat(result.application().getSubmittedAt()).isNotNull();
        assertThat(result.assessment().getBand()).isEqualTo(Band.B);
        assertThat(result.assessment().getMaxApprovableAmount()).isEqualByComparingTo("150000.00");
        assertThat(result.assessment().getReasonLines())
                .anyMatch(r -> r.contains("อายุ 35 ปี ✓"))
                .anyMatch(r -> r.contains("วงเงินที่อนุมัติได้ 150,000 บาท (5 เท่าของรายได้ 30,000 บาท/เดือน)"));
    }

    /** AC-miniloan-036/044: a Band C is stored too, and stops at Submitted — nobody auto-rejects. */
    @Test
    void aBandCIsStoredAndStaysSubmitted() {
        UUID id = draft(new DraftFields(
                "ทดสอบ อายุน้อย",
                19,
                new BigDecimal("30000.00"),
                24,
                new BigDecimal("1000.00"),
                new BigDecimal("100000.00"),
                12));

        var result = submitService.submit(id, APPLICANT);

        assertThat(result.assessment().getBand()).isEqualTo(Band.C);
        assertThat(result.application().getStatus()).isEqualTo(LoanApplication.Status.Submitted);
        assertThat(assessments.findByApplicationId(id)).isPresent();
    }

    /** AC-miniloan-030: every missing field is named, the draft stays a draft, and it stays editable. */
    @Test
    void anIncompleteApplicationIsRefusedWithEveryMissingFieldAndStaysADraft() {
        UUID id = draft(new DraftFields(
                "ทดสอบ ไม่ครบ", 35, new BigDecimal("30000.00"), null, new BigDecimal("1000.00"),
                new BigDecimal("100000.00"), null));

        assertThatThrownBy(() -> submitService.submit(id, APPLICANT))
                .isInstanceOf(IncompleteApplicationException.class)
                .satisfies(ex -> assertThat(((IncompleteApplicationException) ex).getMissingFields())
                        .containsExactly("อายุงาน", "จำนวนงวด"));

        assertThat(applications.findById(id)).get().extracting(LoanApplication::getStatus)
                .isEqualTo(LoanApplication.Status.Draft);
        // The refused command released its key with the rollback, so the corrected draft can be sent.
        assertThat(idempotencyKeys.count()).isZero();
    }

    /** BR-miniloan-004@v1 — the range draft save (BR-miniloan-008@v1) deliberately let through. */
    @Test
    void anOutOfRangeAmountOrTenorIsRefusedOnSubmit() {
        UUID tooSmall = draft(new DraftFields(
                "ทดสอบ วงเงินต่ำ", 35, new BigDecimal("30000.00"), 24, new BigDecimal("1000.00"),
                new BigDecimal("9999.00"), 12));
        UUID tooShort = draft(new DraftFields(
                "ทดสอบ งวดสั้น", 35, new BigDecimal("30000.00"), 24, new BigDecimal("1000.00"),
                new BigDecimal("100000.00"), 5));

        assertThatThrownBy(() -> submitService.submit(tooSmall, APPLICANT))
                .isInstanceOf(OutOfRangeException.class)
                .hasMessage(LoanApplication.AMOUNT_OUT_OF_RANGE_MESSAGE);
        assertThatThrownBy(() -> submitService.submit(tooShort, APPLICANT))
                .isInstanceOf(OutOfRangeException.class)
                .hasMessage(LoanApplication.TERM_OUT_OF_RANGE_MESSAGE);
    }

    /**
     * AC-miniloan-133/134: the repeat is refused out loud rather than answered with the first
     * result, and the database is left holding one submitted application and one command record.
     */
    @Test
    void aSecondSubmitOfTheSameApplicationIsRefusedByTheDatabase() {
        UUID id = draft(complete());
        submitService.submit(id, APPLICANT);

        assertThatThrownBy(() -> submitService.submit(id, APPLICANT))
                .isInstanceOf(DuplicateCommandException.class);

        assertThat(applications.count()).isEqualTo(1);
        assertThat(idempotencyKeys.count()).isEqualTo(1);
        assertThat(assessments.count()).isEqualTo(1);
    }

    /**
     * AC-miniloan-031: the lock this unit puts on holds against the edit endpoint too (API-002), so
     * calling it directly with the same id gets the same refusal a screen would (BR-miniloan-025@v1),
     * and the requested amount is left as it was.
     */
    @Test
    void aSubmittedApplicationCanNoLongerBeEdited() {
        UUID id = draft(complete());
        submitService.submit(id, APPLICANT);

        assertThatThrownBy(() -> draftService.updateDraft(
                        id,
                        APPLICANT,
                        new DraftFields(
                                "ทดสอบ แก้หลังยื่น",
                                35,
                                new BigDecimal("30000.00"),
                                24,
                                new BigDecimal("10500.00"),
                                new BigDecimal("500000.00"),
                                12)))
                .isInstanceOf(LoanApplicationDraftService.DraftNotEditableException.class);

        assertThat(applications.findById(id)).get()
                .extracting(LoanApplication::getRequestedAmount)
                .isEqualTo(new BigDecimal("100000.00"));
    }

    /** AC-miniloan-035: an assessment belongs to a submission, not to a save. */
    @Test
    void aDraftHasNoAssessmentToShow() {
        UUID id = draft(complete());

        var detail = submitService.findDetail(id, APPLICANT);

        assertThat(detail.application().getStatus()).isEqualTo(LoanApplication.Status.Draft);
        assertThat(detail.assessment()).isNull();
    }

    /** ACL-031 (scope=all) against ACL-024 (scope=own), with everyone else on rbac.json's deny. */
    @Test
    void detailScopeFollowsTheRoleThatAsked() {
        UUID id = draft(complete());
        submitService.submit(id, APPLICANT);

        assertThat(submitService.findDetail(id, "ROLE-003").assessment()).isNotNull();
        assertThatThrownBy(() -> submitService.findDetail(id, "ROLE-004"))
                .isInstanceOf(ViewNotPermittedException.class);
    }

    /** GD-miniloan-003 row 1's inputs — an applicant clear of every boundary. */
    private static DraftFields complete() {
        return new DraftFields(
                "ทดสอบ ผู้สมัคร",
                35,
                new BigDecimal("30000.00"),
                24,
                new BigDecimal("10500.00"),
                new BigDecimal("100000.00"),
                12);
    }
}
