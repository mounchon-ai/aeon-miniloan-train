---
type: Business Rule
title: อนุมัติได้เฉพาะใบสมัครสถานะ UnderReview และเมื่ออนุมัติต้องบันทึกผู้อนุมัติและเว
description: อนุมัติได้เฉพาะใบสมัครสถานะ UnderReview และเมื่ออนุมัติต้องบันทึกผู้อนุมัติและเวลาไว้ด้วย
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, invariant]
id: BR-miniloan-011@v1
status: draft
belongs_to: REQ-miniloan-002
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-049, EX-miniloan-050, EX-miniloan-051]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:5d9c1683f5257ae344d5e60d81baf0f3720b64bfea7642ecb7ce7372fd208c82
---

# BR-miniloan-011@v1

## ข้อความของกฎ
อนุมัติได้เฉพาะใบสมัครสถานะ UnderReview และเมื่ออนุมัติต้องบันทึกผู้อนุมัติและเวลาไว้ด้วย

## ที่มา

> "**Given** ใบสมัครสถานะ `UnderReview` **When** เจ้าหน้าที่กดอนุมัติ **Then** สถานะเปลี่ยนเป็น `Approved` พร้อมบันทึกผู้อนุมัติและเวลา"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-05

## พิสูจน์โดย

- [EX-miniloan-049](../examples/EX-miniloan-049.md) — happy: ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" และหน้าใบสมัครแสดง "อนุมัติโดย ก. เมื่อ {วันที่เวลา}" · ทั้งชื่อผู้อนุมัติและเวลาถูกบันทึกไว้กับใบสมัคร ไม่ใช่แค่ใน log แยก
- [EX-miniloan-050](../examples/EX-miniloan-050.md) — exception: ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้ยังไม่เข้าสู่การพิจารณา" · สถานะยังเป็น "ยื่นแล้ว (Submitted)" และไม่มีการบันทึกผู้อนุมัติใดๆ
- [EX-miniloan-051](../examples/EX-miniloan-051.md) — boundary: ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้อนุมัติไปแล้วเมื่อ {วันที่เวลา}" · ค่าผู้อนุมัติและเวลาเดิมไม่ถูกเขียนทับ

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-011@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
