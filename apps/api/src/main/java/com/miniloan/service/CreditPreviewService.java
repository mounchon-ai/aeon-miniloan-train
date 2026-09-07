package com.miniloan.service;

import com.miniloan.service.CreditAssessmentService.MaxApprovable;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * ประเมินวงเงินอนุมัติสูงสุดล่วงหน้า (UC-miniloan-028 · API-021 · BR-miniloan-027@v1 ·
 * BR-miniloan-003@v1 · ACL-021).
 *
 * <p><b>The same arithmetic as the real decision, not a copy of it.</b> AC-miniloan-117's reason for
 * existing is the last clause of its own sentence — "ตัวเลขที่ผู้ใช้เห็นมาจากฝั่งเดียวกับที่จะใช้
 * ตัดสินตอนอนุมัติจริง จึงไม่มีวันไม่ตรงกัน" — so this delegates to {@link
 * CreditAssessmentService#maxApprovableAmount}, the method CALC-miniloan-003@v1 is signed against
 * and GD-miniloan-004 pins. A second implementation here, however carefully copied, would be a
 * second answer that could drift from the first, which is the one outcome the criterion rules out.
 *
 * <p><b>Nothing is stored and no application is needed</b> — API-021 says so: "โดยยังไม่ต้องมี
 * ใบสมัครอยู่ในระบบ". The income arrives in the request, the answer goes back, and no row is written.
 * That is also why ACL-021's {@code scope: own} has no owner to compare against: there is no record
 * to belong to anyone, so "own" collapses to "only ROLE-001 may ask", and that is the whole of the
 * check below. A reader looking for a missing ownership comparison will not find one, and none is
 * missing.
 *
 * <p><b>No eligibility floor is applied here, on purpose.</b> BR-miniloan-001@v1's ≥ 15,000 บาท is an
 * eligibility rule, not part of the ceiling: GD-miniloan-004's own row for 15,000.00 returns
 * 75,000.00, and the calculation is defined across incomes the eligibility rule would turn down.
 * Adding a floor would make the preview disagree with the approval path — the exact divergence
 * AC-miniloan-117 says must never happen. A request carrying no income at all is a different thing:
 * that is a malformed request, refused by the route before this method is reached.
 */
@Service
public class CreditPreviewService {

    /** ผู้สมัคร — the only role ACL-021 admits. */
    private static final String APPLICANT = "ROLE-001";

    /** rbac.json's default effect on API-021 — ACL-021 names ROLE-001 and no other entry names this use case. */
    public static class PreviewNotPermittedException extends RuntimeException {
        public PreviewNotPermittedException() {
            super("บทบาทนี้ไม่มีสิทธิ์ประเมินวงเงินอนุมัติสูงสุดล่วงหน้า");
        }
    }

    /**
     * BR-miniloan-003@v1 through the signed contract. Returns {@link MaxApprovable} whole rather than
     * just the amount, so the caller that has to prove this against GD-miniloan-004 can read every
     * column of the answer key; what a client is told is narrower and the controller decides that.
     */
    public MaxApprovable preview(String callerRole, BigDecimal monthlyIncome) {
        if (!APPLICANT.equals(callerRole)) {
            throw new PreviewNotPermittedException();
        }
        return CreditAssessmentService.maxApprovableAmount(monthlyIncome);
    }
}
