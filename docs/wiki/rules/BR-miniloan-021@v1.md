---
type: Business Rule
title: LoanAccount เปลี่ยนเป็น Closed ได้เฉพาะสองทาง: ชำระครบทุกงวด หรือปิดก่อนกำหนดสำเ
description: LoanAccount เปลี่ยนเป็น Closed ได้เฉพาะสองทาง: ชำระครบทุกงวด หรือปิดก่อนกำหนดสำเร็จ
resource: ../requirements/REQ-miniloan-004.md
tags: [miniloan, invariant]
id: BR-miniloan-021@v1
status: draft
belongs_to: REQ-miniloan-004
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-004, EX-miniloan-005, EX-miniloan-006, EX-miniloan-007]
golden: []
provenance: [SRC-001, SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:31c94e97362dea77a9245f4aee0933bd5045d0133ba3e22f9a732f63f13db80f
---

# BR-miniloan-021@v1

## ข้อความของกฎ
LoanAccount เปลี่ยนเป็น Closed ได้เฉพาะสองทาง: ชำระครบทุกงวด หรือปิดก่อนกำหนดสำเร็จ

## ที่มา

> "Active → Closed · ปิดได้เมื่อชำระครบทุกงวด หรือปิดก่อนกำหนดสำเร็จ"
> — [SRC-001](../sources/SRC-001.md) หน้า — §6 · LoanAccount

> "**Given** ชำระครบทุกงวด **When** บันทึกงวดสุดท้าย **Then** บัญชีเปลี่ยนเป็น `Closed`"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-10

## พิสูจน์โดย

- [EX-miniloan-004](../examples/EX-miniloan-004.md) — happy: หน้าบัญชีแสดงสถานะ "ปิดบัญชีแล้ว (Closed)" พร้อมข้อความ "ชำระครบทุกงวดแล้ว — ปิดบัญชีเมื่อ {วันที่บันทึกงวดสุดท้าย}" และทุกงวดในตารางผ่อนแสดงสถานะ "จ่ายแล้ว"
- [EX-miniloan-005](../examples/EX-miniloan-005.md) — alternate: หน้าบัญชีแสดงสถานะ "ปิดบัญชีแล้ว (Closed)" พร้อมข้อความ "ปิดบัญชีก่อนกำหนด — ชำระยอดปิดบัญชีครบเมื่อ {วันที่บันทึก}" และงวดที่ 6 ถึง 12 ในตารางผ่อนแสดงสถานะ "ยกเลิก (ปิดบัญชีก่อนกำหนด)" ตาม BR-miniloan-023@v1
- [EX-miniloan-006](../examples/EX-miniloan-006.md) — exception: ระบบไม่เปลี่ยนสถานะบัญชี และแสดงข้อความ "ปิดบัญชีไม่ได้ — บัญชีนี้ยังมีงวดค้าง 7 งวด · ปิดบัญชีได้เมื่อชำระครบทุกงวด หรือชำระยอดปิดบัญชีก่อนกำหนดครบเท่านั้น" · บัญชียังคงสถานะ "กำลังผ่อนชำระ (Active)"
- [EX-miniloan-007](../examples/EX-miniloan-007.md) — boundary: ทั้งสองทางถูกปฏิเสธเหมือนกัน — หน้าจอแสดง "บัญชีนี้ปิดแล้ว — ปิดซ้ำไม่ได้" และ API ปฏิเสธด้วยเช่นกันตาม BR-miniloan-025@v1 (กฎบังคับที่ฝั่ง API ไม่ใช่แค่ซ่อนบนหน้าจอ) · สถานะยังเป็น "ปิดบัญชีแล้ว (Closed)" ไม่เปลี่ยนแปลง

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-021@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
