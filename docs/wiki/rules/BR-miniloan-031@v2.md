---
type: Business Rule
title: สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applica
description: สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) แยกตามว่าใบนั้นถูกมอบหมายแล้วหรือยัง — ใบที่ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะ Loan Officer ที่ถูกมอบหมายตาม BR-miniloan-032@v1 · ใบที่ยังไม่ถูกมอบหมาย (Draft และ Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ (UL-miniloan-019) เท่านั้น Loan Officer ทั่วไปทำไม่ได้** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ต้องการยกเลิกต้องแจ้งเจ้าหน้าที่ และถ้าใบยังไม่ถูกมอบหมายต้องถึงหัวหน้า
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, invariant]
id: BR-miniloan-031@v2
status: draft
belongs_to: REQ-miniloan-002
kind: invariant
is_current: true
effective_from: 2026-08-14
test_design: [state_transition, decision_table]
proven_by: [EX-miniloan-061, EX-miniloan-062, EX-miniloan-064, EX-miniloan-091, EX-miniloan-092, EX-miniloan-093, EX-miniloan-047]
golden: []
supersedes: BR-miniloan-031@v1
provenance: [SRC-002, SRC-002, SRC-001, SRC-011, SRC-013, SRC-015]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:41f85fe287f9be40642cf9c5c0cd09bcc0444734730159dd1508b675ca727c63
---

# BR-miniloan-031@v2

## ข้อความของกฎ
สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) แยกตามว่าใบนั้นถูกมอบหมายแล้วหรือยัง — ใบที่ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะ Loan Officer ที่ถูกมอบหมายตาม BR-miniloan-032@v1 · ใบที่ยังไม่ถูกมอบหมาย (Draft และ Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ (UL-miniloan-019) เท่านั้น Loan Officer ทั่วไปทำไม่ได้** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ต้องการยกเลิกต้องแจ้งเจ้าหน้าที่ และถ้าใบยังไม่ถูกมอบหมายต้องถึงหัวหน้า

## ที่มา

> "สิทธิ์เดินสถานะใบสมัคร: Draft→Submitted เฉพาะ Applicant เจ้าของใบสมัคร · Submitted→UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview→Approved, UnderReview→Rejected, Approved→Disbursed เฉพาะ Loan Officer"
> — [SRC-002](../sources/SRC-002.md) หน้า — §[2] QB-perm-01

> "Applicant ห้ามทำสิ่งที่ย้อนกลับไม่ได้: อนุมัติ ปฏิเสธ เบิกจ่าย บันทึกการชำระ ปิดบัญชี"
> — [SRC-002](../sources/SRC-002.md) หน้า — §[1] QB-actor-02

> "**Loan Officer** | เจ้าหน้าที่พิจารณา อนุมัติ/ปฏิเสธ และสั่งเบิกจ่าย"
> — [SRC-001](../sources/SRC-001.md) หน้า — §3 · ผู้ใช้งาน (Actors)

> "การยกเลิกใบสมัครทำได้เฉพาะเจ้าหน้าที่ (Loan Officer) เท่านั้น · Applicant ยกเลิกใบของตัวเองไม่ได้ทุกสถานะ ต้องแจ้งเจ้าหน้าที่"
> — [SRC-011](../sources/SRC-011.md) หน้า — §[1] Q-miniloan-011 ท่อน 1

> "ตอบ b : เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อเท่านั้น"
> — [SRC-013](../sources/SRC-013.md) หน้า — §[2] Q-miniloan-012 · คำตอบข้อ 2ก

> "ตอบ B (supervisor) : หัวหน้าเป็น actor แยก ไม่ใช่ Loan Officer"
> — [SRC-015](../sources/SRC-015.md) หน้า — §[1] Q-miniloan-012 · A/B

## พิสูจน์โดย

- [EX-miniloan-047](../examples/EX-miniloan-047.md) — alternate: ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุดท้าย · หน้าจอแสดง "ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง" · ปุ่มอนุมัติ ปฏิเสธ และเบิกจ่ายหายไปทั้งหมด
- [EX-miniloan-061](../examples/EX-miniloan-061.md) — happy: ใบสมัครเปลี่ยนเป็น "ยื่นแล้ว (Submitted)" ตามปกติ — เส้น Draft → Submitted เป็นของ Applicant เจ้าของใบเท่านั้น
- [EX-miniloan-062](../examples/EX-miniloan-062.md) — exception: API ปฏิเสธ — "ไม่มีสิทธิ์อนุมัติใบสมัคร" · **การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ** ตาม BR-miniloan-025@v1 · สถานะใบสมัครไม่เปลี่ยน
- [EX-miniloan-064](../examples/EX-miniloan-064.md) — alternate: **ไม่มีปุ่มให้ใครกดเลย** — ทั้ง Applicant, Loan Officer และหัวหน้า · ใบสมัครเปลี่ยนเป็น "อยู่ระหว่างพิจารณา (UnderReview)" เองเมื่อการประเมินเสร็จ เพราะเส้นนี้เป็นของ System ไม่ใช่ของคน
- [EX-miniloan-091](../examples/EX-miniloan-091.md) — happy: ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุดท้าย · หน้าจอแสดง **"ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง"** · ระบบบันทึกว่าหัวหน้าเป็นผู้กระทำพร้อมเวลาตาม NFR-miniloan-002 · **เส้น Draft → Cancelled ที่ BR-miniloan-010@v1 ประกาศไว้ตั้งแต่แรก เดินได้จริงเป็นครั้งแรกที่ใบนี้**
- [EX-miniloan-092](../examples/EX-miniloan-092.md) — exception: ทั้งสองทางถูกปฏิเสธเหมือนกัน — **"ยกเลิกใบสมัครที่ยังไม่ถูกมอบหมายได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ"** · การปฏิเสธเกิดที่ฝั่ง API ไม่ใช่แค่ซ่อนปุ่มบนหน้าจอ ตาม BR-miniloan-025@v1 · สถานะยังเป็น "ยื่นแล้ว (Submitted)" ไม่เปลี่ยน · **นี่คือข้อจำกัดที่ @v1 ไม่มี** — ภายใต้ @v1 ใบนี้จะถูกยกเลิกสำเร็จ เพราะกฎเดิมให้ Loan Officer คนไหนก็ได้
- [EX-miniloan-093](../examples/EX-miniloan-093.md) — boundary: ระบบปฏิเสธ — **"ใบสมัครนี้ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะเจ้าหน้าที่ที่รับผิดชอบใบนี้"** · สถานะยังเป็น "อยู่ระหว่างพิจารณา (UnderReview)" ไม่เปลี่ยน · **สิทธิ์ยกเลิกย้ายจากหัวหน้าไปที่ Loan Officer ที่ถูกมอบหมายทันทีที่การมอบหมายเกิด ไม่ใช่ทั้งสองคนถือพร้อมกัน** — คนเดิมคนเดียวกัน ใบเดิมใบเดียวกัน ต่างกันแค่ถูกมอบหมายแล้วหรือยัง

## แทนที่
[BR-miniloan-031@v1](BR-miniloan-031@v1.md) — *เหตุผล: หัวหน้าเจ้าหน้าที่สินเชื่อถูกตัดสินแล้วว่าเป็น actor แยก ไม่ใช่ Loan Officer คนหนึ่ง (คำตอบ Q-miniloan-012) · ประโยคเดิมที่ว่า "ยกเลิกได้เฉพาะ Loan Officer เท่านั้น" จึงแคบเกินความจริง และทำให้เส้นยกเลิกใบสถานะ Draft กับ Submitted ที่ BR-miniloan-010@v1 เปิดไว้ ไม่มีใครเดินได้*

> เดิม: สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) ทำได้เฉพาะ Loan Officer เท่านั้น และถ้าใบนั้นถูกมอบหมายแล้วต้องเป็นคนที่ถูกมอบหมายตาม BR-miniloan-032@v1** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ยกเลิกได้ต้องแจ้งเจ้าหน้าที่ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ได้ ยังไม่ตัดสิน (ดู Q-miniloan-012)

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| [BR-miniloan-031@v1](BR-miniloan-031@v1.md) ❄️ | — | ตั้งต้น | — |
| **BR-miniloan-031@v2** (หน้านี้) ✅ | 2026-08-14 | หัวหน้าเจ้าหน้าที่สินเชื่อถูกตัดสินแล้วว่าเป็น actor แยก ไม่ใช่ Loan Officer คนหนึ่ง (คำตอบ Q-miniloan-012) · ประโยคเดิมที่ว่า "ยกเลิกได้เฉพาะ Loan Officer เท่านั้น" จึงแคบเกินความจริง และทำให้เส้นยกเลิกใบสถานะ Draft กับ Submitted ที่ BR-miniloan-010@v1 เปิดไว้ ไม่มีใครเดินได้ | [CHG-miniloan-001](../changes/CHG-miniloan-001.md) |
