---
type: Example
title: happy — ใบสมัครเปลี่ยนเป็น "ยื่นแล้ว (Submitted)" ตามปกติ — เส้น Dra
description: ใบสมัครเปลี่ยนเป็น "ยื่นแล้ว (Submitted)" ตามปกติ — เส้น Draft → Submitted เป็นของ Applicant เจ้าของใบเท่านั้น
resource: ../rules/BR-miniloan-031@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-061
status: draft
kind: happy
proves: [BR-miniloan-031@v1, BR-miniloan-031@v2]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:4fd3de22c9efaa44da103d509a812bd072a1fe0565e75b964218e8804a7c9a82
---

# EX-miniloan-061

## กำหนดให้ (given)
ใบสมัครสถานะ "ร่าง (Draft)" ที่กรอกครบแล้ว และผู้ที่ล็อกอินอยู่คือ Applicant เจ้าของใบสมัครนั้น

## เมื่อ (when)
กดยื่นใบสมัคร

## แล้ว (then)
ใบสมัครเปลี่ยนเป็น "ยื่นแล้ว (Submitted)" ตามปกติ — เส้น Draft → Submitted เป็นของ Applicant เจ้าของใบเท่านั้น

## พิสูจน์กฎ

- [BR-miniloan-031@v1](../rules/BR-miniloan-031@v1.md) ❄️ — สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) ทำได้เฉพาะ Loan Officer เท่านั้น และถ้าใบนั้นถูกมอบหมายแล้วต้องเป็นคนที่ถูกมอบหมายตาม BR-miniloan-032@v1** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ยกเลิกได้ต้องแจ้งเจ้าหน้าที่ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ได้ ยังไม่ตัดสิน (ดู Q-miniloan-012)
- [BR-miniloan-031@v2](../rules/BR-miniloan-031@v2.md) ✅ ปัจจุบัน — สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) แยกตามว่าใบนั้นถูกมอบหมายแล้วหรือยัง — ใบที่ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะ Loan Officer ที่ถูกมอบหมายตาม BR-miniloan-032@v1 · ใบที่ยังไม่ถูกมอบหมาย (Draft และ Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ (UL-miniloan-019) เท่านั้น Loan Officer ทั่วไปทำไม่ได้** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ต้องการยกเลิกต้องแจ้งเจ้าหน้าที่ และถ้าใบยังไม่ถูกมอบหมายต้องถึงหัวหน้า
