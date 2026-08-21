---
type: Business Rule
title: เมื่อใบสมัครถูกยื่น ระบบต้องประเมินอัตโนมัติและสร้าง CreditAssessment ที่มีคะแนน
description: เมื่อใบสมัครถูกยื่น ระบบต้องประเมินอัตโนมัติและสร้าง CreditAssessment ที่มีคะแนน Band (A/B/C) วงเงินที่อนุมัติได้ และเหตุผลที่ผ่าน/ไม่ผ่านรายเกณฑ์
resource: ../requirements/REQ-miniloan-001.md
tags: [miniloan, invariant]
id: BR-miniloan-009@v1
status: draft
belongs_to: REQ-miniloan-001
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-034, EX-miniloan-035, EX-miniloan-036]
golden: []
provenance: [SRC-001, SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:405d02f0caaf72b9cbf24cb0559eaebe4964af3773d1e50900f666fafe553c6f
---

# BR-miniloan-009@v1

## ข้อความของกฎ
เมื่อใบสมัครถูกยื่น ระบบต้องประเมินอัตโนมัติและสร้าง CreditAssessment ที่มีคะแนน Band (A/B/C) วงเงินที่อนุมัติได้ และเหตุผลที่ผ่าน/ไม่ผ่านรายเกณฑ์

## ที่มา

> "**Given** ใบสมัครถูกยื่น **When** ระบบประเมิน **Then** สร้าง CreditAssessment ที่มีคะแนน, Band (A/B/C), วงเงินที่อนุมัติได้ และเหตุผล"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-03

> "**Given** มี CreditAssessment **When** เปิดดูใบสมัคร **Then** เห็นรายการเกณฑ์ที่ผ่าน/ไม่ผ่าน และวงเงินสูงสุดตาม BR-03"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-04

## พิสูจน์โดย

- [EX-miniloan-034](../examples/EX-miniloan-034.md) — happy: ระบบประเมินทันทีโดยไม่ต้องมีใครกดอะไรเพิ่ม และหน้าผลการประเมินมีครบสามส่วน — ช่อง "Credit Band" มีค่ากำกับ · ช่อง "วงเงินที่อนุมัติได้" มีจำนวนเงิน · และรายการ "เหตุผลรายเกณฑ์" แสดงผลผ่าน/ไม่ผ่านของทุกเกณฑ์ ไม่ใช่เฉพาะข้อที่ตก
- [EX-miniloan-035](../examples/EX-miniloan-035.md) — exception: ไม่มี CreditAssessment ให้ดู และหน้าจอแสดง "ยังไม่มีผลการประเมิน — ใบสมัครนี้ยังไม่ได้ยื่น" · **การประเมินผูกกับการยื่น ไม่ใช่กับการบันทึก**
- [EX-miniloan-036](../examples/EX-miniloan-036.md) — boundary: ถึงผลจะเป็นการปฏิเสธ ระบบก็ยัง **ต้องสร้าง CreditAssessment เก็บไว้** — หน้าผลการประเมินแสดง "อายุ 19 ปี ✗ ต้องอยู่ระหว่าง 20–60 ปี" พร้อม Credit Band และวงเงินที่คำนวณได้ · **ไม่ใช่ปฏิเสธแล้วไม่เก็บอะไรเลย** ไม่งั้นจะตอบไม่ได้ว่าตอนนั้นระบบตัดสินด้วยอะไร

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-009@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
