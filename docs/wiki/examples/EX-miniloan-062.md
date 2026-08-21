---
type: Example
title: exception — API ปฏิเสธ — "ไม่มีสิทธิ์อนุมัติใบสมัคร" · **การปฏิเสธเกิดที
description: API ปฏิเสธ — "ไม่มีสิทธิ์อนุมัติใบสมัคร" · **การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ** ตาม BR-miniloan-025@v1 · สถานะใบสมัครไม่เปลี่ยน
resource: ../rules/BR-miniloan-031@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-062
status: draft
kind: exception
proves: [BR-miniloan-031@v1, BR-miniloan-031@v2]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:24e7638c6bff284550d1707d53cafe86d6a717eb3621429ebc0dc925cafa34b3
---

# EX-miniloan-062

## กำหนดให้ (given)
ใบสมัครของตัวเองที่อยู่สถานะ "อยู่ระหว่างพิจารณา (UnderReview)" และผู้ที่ล็อกอินอยู่คือ Applicant เจ้าของใบ

## เมื่อ (when)
Applicant เรียก API อนุมัติใบสมัครของตัวเองโดยตรง โดยไม่ผ่านหน้าจอ

## แล้ว (then)
API ปฏิเสธ — "ไม่มีสิทธิ์อนุมัติใบสมัคร" · **การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ** ตาม BR-miniloan-025@v1 · สถานะใบสมัครไม่เปลี่ยน

## พิสูจน์กฎ

- [BR-miniloan-031@v1](../rules/BR-miniloan-031@v1.md) ❄️ — สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) ทำได้เฉพาะ Loan Officer เท่านั้น และถ้าใบนั้นถูกมอบหมายแล้วต้องเป็นคนที่ถูกมอบหมายตาม BR-miniloan-032@v1** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ยกเลิกได้ต้องแจ้งเจ้าหน้าที่ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ได้ ยังไม่ตัดสิน (ดู Q-miniloan-012)
- [BR-miniloan-031@v2](../rules/BR-miniloan-031@v2.md) ✅ ปัจจุบัน — สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) แยกตามว่าใบนั้นถูกมอบหมายแล้วหรือยัง — ใบที่ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะ Loan Officer ที่ถูกมอบหมายตาม BR-miniloan-032@v1 · ใบที่ยังไม่ถูกมอบหมาย (Draft และ Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ (UL-miniloan-019) เท่านั้น Loan Officer ทั่วไปทำไม่ได้** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ต้องการยกเลิกต้องแจ้งเจ้าหน้าที่ และถ้าใบยังไม่ถูกมอบหมายต้องถึงหัวหน้า
