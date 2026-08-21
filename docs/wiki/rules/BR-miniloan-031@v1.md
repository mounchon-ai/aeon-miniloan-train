---
type: Business Rule
title: สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applica
description: สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) ทำได้เฉพาะ Loan Officer เท่านั้น และถ้าใบนั้นถูกมอบหมายแล้วต้องเป็นคนที่ถูกมอบหมายตาม BR-miniloan-032@v1** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ยกเลิกได้ต้องแจ้งเจ้าหน้าที่ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ได้ ยังไม่ตัดสิน (ดู Q-miniloan-012)
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, invariant]
id: BR-miniloan-031@v1
status: superseded
belongs_to: REQ-miniloan-002
kind: invariant
is_current: false
test_design: [state_transition, decision_table]
proven_by: [EX-miniloan-061, EX-miniloan-062, EX-miniloan-063, EX-miniloan-064]
golden: []
superseded_by: BR-miniloan-031@v2
provenance: [SRC-002, SRC-002, SRC-001, SRC-011]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:460e8831616347c1dd8b6a030a7cd7832ac9b7815404c1fdf3b0b99c38819c05
---

# BR-miniloan-031@v1

## ข้อความของกฎ
สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) ทำได้เฉพาะ Loan Officer เท่านั้น และถ้าใบนั้นถูกมอบหมายแล้วต้องเป็นคนที่ถูกมอบหมายตาม BR-miniloan-032@v1** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ยกเลิกได้ต้องแจ้งเจ้าหน้าที่ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ได้ ยังไม่ตัดสิน (ดู Q-miniloan-012)

## ที่มา

> "สิทธิ์เดินสถานะใบสมัคร: Draft→Submitted เฉพาะ Applicant เจ้าของใบสมัคร · Submitted→UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview→Approved, UnderReview→Rejected, Approved→Disbursed เฉพาะ Loan Officer"
> — [SRC-002](../sources/SRC-002.md) หน้า — §[2] QB-perm-01

> "Applicant ห้ามทำสิ่งที่ย้อนกลับไม่ได้: อนุมัติ ปฏิเสธ เบิกจ่าย บันทึกการชำระ ปิดบัญชี"
> — [SRC-002](../sources/SRC-002.md) หน้า — §[1] QB-actor-02

> "**Loan Officer** | เจ้าหน้าที่พิจารณา อนุมัติ/ปฏิเสธ และสั่งเบิกจ่าย"
> — [SRC-001](../sources/SRC-001.md) หน้า — §3 · ผู้ใช้งาน (Actors)

> "การยกเลิกใบสมัครทำได้เฉพาะเจ้าหน้าที่ (Loan Officer) เท่านั้น · Applicant ยกเลิกใบของตัวเองไม่ได้ทุกสถานะ ต้องแจ้งเจ้าหน้าที่"
> — [SRC-011](../sources/SRC-011.md) หน้า — §[1] Q-miniloan-011 ท่อน 1

## พิสูจน์โดย

- [EX-miniloan-061](../examples/EX-miniloan-061.md) — happy: ใบสมัครเปลี่ยนเป็น "ยื่นแล้ว (Submitted)" ตามปกติ — เส้น Draft → Submitted เป็นของ Applicant เจ้าของใบเท่านั้น
- [EX-miniloan-062](../examples/EX-miniloan-062.md) — exception: API ปฏิเสธ — "ไม่มีสิทธิ์อนุมัติใบสมัคร" · **การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ** ตาม BR-miniloan-025@v1 · สถานะใบสมัครไม่เปลี่ยน
- [EX-miniloan-063](../examples/EX-miniloan-063.md) — exception: ทั้งสองทางถูกปฏิเสธ — "ยกเลิกใบสมัครเองไม่ได้ — กรุณาติดต่อเจ้าหน้าที่" · **แม้จะเป็นใบของตัวเองและยังเป็นร่างอยู่ก็ตาม** เพราะ Cancelled เป็นสถานะสุดท้ายที่ย้อนกลับไม่ได้
- [EX-miniloan-064](../examples/EX-miniloan-064.md) — alternate: **ไม่มีปุ่มให้ใครกดเลย** — ทั้ง Applicant, Loan Officer และหัวหน้า · ใบสมัครเปลี่ยนเป็น "อยู่ระหว่างพิจารณา (UnderReview)" เองเมื่อการประเมินเสร็จ เพราะเส้นนี้เป็นของ System ไม่ใช่ของคน

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-031@v1** (หน้านี้) ❄️ | — | ตั้งต้น | — |
| [BR-miniloan-031@v2](BR-miniloan-031@v2.md) ✅ | 2026-08-14 | หัวหน้าเจ้าหน้าที่สินเชื่อถูกตัดสินแล้วว่าเป็น actor แยก ไม่ใช่ Loan Officer คนหนึ่ง (คำตอบ Q-miniloan-012) · ประโยคเดิมที่ว่า "ยกเลิกได้เฉพาะ Loan Officer เท่านั้น" จึงแคบเกินความจริง และทำให้เส้นยกเลิกใบสถานะ Draft กับ Submitted ที่ BR-miniloan-010@v1 เปิดไว้ ไม่มีใครเดินได้ | [CHG-miniloan-001](../changes/CHG-miniloan-001.md) |
