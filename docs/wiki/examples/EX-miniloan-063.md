---
type: Example
title: exception — ทั้งสองทางถูกปฏิเสธ — "ยกเลิกใบสมัครเองไม่ได้ — กรุณาติดต่อเ
description: ทั้งสองทางถูกปฏิเสธ — "ยกเลิกใบสมัครเองไม่ได้ — กรุณาติดต่อเจ้าหน้าที่" · **แม้จะเป็นใบของตัวเองและยังเป็นร่างอยู่ก็ตาม** เพราะ Cancelled เป็นสถานะสุดท้ายที่ย้อนกลับไม่ได้
resource: ../rules/BR-miniloan-031@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-063
status: draft
kind: exception
proves: [BR-miniloan-031@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:45be6917a21df713554370d21f980fdf545a0d22276a91229ad29bc77f74a002
---

# EX-miniloan-063

## กำหนดให้ (given)
ใบสมัครของตัวเองที่อยู่สถานะ "ร่าง (Draft)" และผู้ที่ล็อกอินอยู่คือ Applicant เจ้าของใบ

## เมื่อ (when)
Applicant พยายามยกเลิกใบสมัครของตัวเอง ทั้งจากหน้าจอและด้วยการเรียก API ตรง

## แล้ว (then)
ทั้งสองทางถูกปฏิเสธ — "ยกเลิกใบสมัครเองไม่ได้ — กรุณาติดต่อเจ้าหน้าที่" · **แม้จะเป็นใบของตัวเองและยังเป็นร่างอยู่ก็ตาม** เพราะ Cancelled เป็นสถานะสุดท้ายที่ย้อนกลับไม่ได้

## พิสูจน์กฎ

- [BR-miniloan-031@v1](../rules/BR-miniloan-031@v1.md) ❄️ — สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) ทำได้เฉพาะ Loan Officer เท่านั้น และถ้าใบนั้นถูกมอบหมายแล้วต้องเป็นคนที่ถูกมอบหมายตาม BR-miniloan-032@v1** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ยกเลิกได้ต้องแจ้งเจ้าหน้าที่ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ได้ ยังไม่ตัดสิน (ดู Q-miniloan-012)
