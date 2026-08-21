---
type: Business Rule
title: แดชบอร์ดของ Loan Officer แสดงจำนวนแยกตามสถานะ Submitted / UnderReview / Approved
description: แดชบอร์ดของ Loan Officer แสดงจำนวนแยกตามสถานะ Submitted / UnderReview / Approved / Active / Closed โดยนับข้ามทั้ง LoanApplication และ LoanAccount
resource: ../requirements/REQ-miniloan-005.md
tags: [miniloan, policy]
id: BR-miniloan-024@v1
status: draft
belongs_to: REQ-miniloan-005
kind: policy
is_current: true
test_design: [EP]
proven_by: [EX-miniloan-120, EX-miniloan-121, EX-miniloan-122]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:57a7167a4fd342609f29699cdca755002e37c58c4610a4ca5450f51cbb722836
---

# BR-miniloan-024@v1

## ข้อความของกฎ
แดชบอร์ดของ Loan Officer แสดงจำนวนแยกตามสถานะ Submitted / UnderReview / Approved / Active / Closed โดยนับข้ามทั้ง LoanApplication และ LoanAccount

## ที่มา

> "**Given** มีใบสมัครหลายสถานะ **When** เปิดแดชบอร์ด **Then** เห็นจำนวนแยกตามสถานะ (Submitted/UnderReview/Approved/Active/Closed)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-12

## พิสูจน์โดย

- [EX-miniloan-120](../examples/EX-miniloan-120.md) — happy: แดชบอร์ดแสดงครบห้าตัวเลข **"ยื่นแล้ว 3 · อยู่ระหว่างพิจารณา 2 · อนุมัติแล้ว 1 · ใช้งานอยู่ 5 · ปิดแล้ว 4"** · **สามตัวแรกนับจากใบสมัคร สองตัวหลังนับจากบัญชีสินเชื่อ — ตัวเลขชุดเดียวกันมาจากสอง Aggregate**
- [EX-miniloan-121](../examples/EX-miniloan-121.md) — boundary: แสดง **"ยื่นแล้ว 0 · อยู่ระหว่างพิจารณา 0 · อนุมัติแล้ว 0 · ใช้งานอยู่ 0 · ปิดแล้ว 0"** · **เป็นเลขศูนย์ ไม่ใช่ช่องว่าง ไม่ใช่ขีด และไม่ใช่ข้อความว่าไม่มีข้อมูล** · หน้าจอไม่พังและไม่แสดงข้อผิดพลาด
- [EX-miniloan-122](../examples/EX-miniloan-122.md) — exception: ถูกนับเป็น **"ใช้งานอยู่ 1" เท่านั้น** · **ต้องไม่ถูกนับซ้ำในช่อง "อนุมัติแล้ว" ด้วย** และสถานะ Disbursed ของใบสมัครไม่มีช่องของตัวเองบนแดชบอร์ด · **นี่คือจุดที่การนับข้ามสอง Aggregate พลาดได้ง่ายที่สุด** · ขอบเขตข้อมูลที่ Loan Officer แต่ละคนเห็นยังไม่ตัดสิน (DQ-miniloan-004)

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-024@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
