---
type: Business Rule
title: LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderRe
description: LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderReview → Approved → Disbursed และ UnderReview → Rejected · มีเส้นยกเลิกเพิ่ม: Draft, Submitted, UnderReview และ Approved → Cancelled ได้ · ใบที่ Disbursed แล้วยกเลิกไม่ได้ทุกกรณี ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1 แทน · Rejected และ Cancelled เป็นสถานะสุดท้ายทั้งคู่ ไม่มีเส้นออก · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น (Submit(), Approve(), Reject(), Disburse(), Cancel()) — ถอยสถานะกลับไม่ได้ วิธีแก้เมื่อเดินผิดคือยกเลิกแล้วสร้างใบใหม่
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, invariant]
id: BR-miniloan-010@v1
status: draft
belongs_to: REQ-miniloan-002
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-045, EX-miniloan-046, EX-miniloan-047, EX-miniloan-048]
golden: []
provenance: [SRC-001, SRC-004, SRC-010, SRC-010]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:1ebb4694d17d9a4794641027824e08bc7bfa7ff6469b073678aac779f2a1e975
---

# BR-miniloan-010@v1

## ข้อความของกฎ
LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderReview → Approved → Disbursed และ UnderReview → Rejected · มีเส้นยกเลิกเพิ่ม: Draft, Submitted, UnderReview และ Approved → Cancelled ได้ · ใบที่ Disbursed แล้วยกเลิกไม่ได้ทุกกรณี ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1 แทน · Rejected และ Cancelled เป็นสถานะสุดท้ายทั้งคู่ ไม่มีเส้นออก · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น (Submit(), Approve(), Reject(), Disburse(), Cancel()) — ถอยสถานะกลับไม่ได้ วิธีแก้เมื่อเดินผิดคือยกเลิกแล้วสร้างใบใหม่

## ที่มา

> "Draft → Submitted → UnderReview → Approved → Disbursed / └──────→ Rejected · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น"
> — [SRC-001](../sources/SRC-001.md) หน้า — §6 · LoanApplication

> "เดินสถานะผิดแล้วถอยกลับไม่ได้ ไม่มีเส้นถอยในระบบ · วิธีแก้คือยกเลิกแล้วสร้างใบใหม่"
> — [SRC-004](../sources/SRC-004.md) หน้า — §[1] QB-rev-01

> "ใบที่ถูกยกเลิกไปอยู่สถานะใหม่ Cancelled แยกขาดจาก Rejected"
> — [SRC-010](../sources/SRC-010.md) หน้า — §[2] Q-miniloan-007 ท่อน 1

> "ยกเลิกได้ตั้งแต่ Draft ถึง Approved เท่านั้น · ใบที่ Disbursed แล้วยกเลิกไม่ได้ ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1"
> — [SRC-010](../sources/SRC-010.md) หน้า — §[3] Q-miniloan-007 ท่อน 2+3

## พิสูจน์โดย

- [EX-miniloan-045](../examples/EX-miniloan-045.md) — happy: ใบสมัครเดินตามลำดับ "ร่าง (Draft)" → "ยื่นแล้ว (Submitted)" → "อยู่ระหว่างพิจารณา (UnderReview)" → "อนุมัติแล้ว (Approved)" → "เบิกจ่ายแล้ว (Disbursed)" โดยไม่ข้ามขั้นใดเลย และทุกครั้งที่เปลี่ยนสถานะมีผู้กระทำและเวลาบันทึกไว้
- [EX-miniloan-046](../examples/EX-miniloan-046.md) — exception: ทั้งสองทางถูกปฏิเสธเหมือนกัน — "ถอยสถานะใบสมัครไม่ได้ — ถ้าต้องแก้ ให้ยกเลิกใบนี้แล้วสร้างใบใหม่" และ API ปฏิเสธด้วยตาม BR-miniloan-025@v1 · สถานะยังเป็น "อนุมัติแล้ว (Approved)" ไม่เปลี่ยน
- [EX-miniloan-047](../examples/EX-miniloan-047.md) — alternate: ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุดท้าย · หน้าจอแสดง "ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง" · ปุ่มอนุมัติ ปฏิเสธ และเบิกจ่ายหายไปทั้งหมด
- [EX-miniloan-048](../examples/EX-miniloan-048.md) — boundary: ระบบปฏิเสธ — "ยกเลิกใบสมัครที่เบิกจ่ายแล้วไม่ได้ — ใบนี้มีบัญชีสินเชื่อเปิดอยู่ ให้ดำเนินการทางปิดบัญชีแทน" · สถานะยังเป็น "เบิกจ่ายแล้ว (Disbursed)" และบัญชีสินเชื่อไม่ถูกแตะต้อง

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-010@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
