package com.miniloan.service;

import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * บันทึกร่างใบสมัคร (API-001 · API-002). BR-miniloan-008@v1: draft save accepts partial and
 * out-of-range values as-is — it never rejects on completeness (BR-miniloan-007@v1) or on the
 * BR-miniloan-004@v1 amount/tenor range. Both are enforced only when the application is
 * submitted (FE-miniloan-005).
 */
@Service
public class LoanApplicationDraftService {

    private final LoanApplicationRepository repository;

    public LoanApplicationDraftService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    public record DraftFields(
            String fullName,
            Integer age,
            BigDecimal monthlyIncome,
            Integer currentEmploymentMonths,
            BigDecimal existingMonthlyDebt,
            BigDecimal requestedAmount,
            Integer requestedTermMonths) {}

    public static class DraftNotFoundException extends RuntimeException {
        public DraftNotFoundException(UUID id) {
            super("ไม่พบใบสมัครร่าง " + id);
        }
    }

    public static class DraftNotEditableException extends RuntimeException {
        public DraftNotEditableException(UUID id) {
            super("ใบสมัคร " + id + " ไม่ใช่สถานะร่างแล้ว แก้ไขไม่ได้");
        }
    }

    @Transactional
    public LoanApplication saveNewDraft(String applicantId, DraftFields fields) {
        LoanApplication application = new LoanApplication(applicantId);
        applyFields(application, fields);
        return repository.save(application);
    }

    @Transactional
    public LoanApplication updateDraft(UUID id, String applicantId, DraftFields fields) {
        LoanApplication application =
                repository
                        .findByIdAndApplicantId(id, applicantId)
                        .orElseThrow(() -> new DraftNotFoundException(id));
        if (application.getStatus() != LoanApplication.Status.Draft) {
            throw new DraftNotEditableException(id);
        }
        applyFields(application, fields);
        return repository.save(application);
    }

    private void applyFields(LoanApplication application, DraftFields fields) {
        application.applyDraftFields(
                fields.fullName(),
                fields.age(),
                fields.monthlyIncome(),
                fields.currentEmploymentMonths(),
                fields.existingMonthlyDebt(),
                fields.requestedAmount(),
                fields.requestedTermMonths());
    }
}
