---
type: Business Rule
title: เมื่อชำระยอดปิดบัญชีก่อนกำหนดครบ บัญชีเปลี่ยนเป็น Closed และงวดที่เหลือในตารางผ่
description: เมื่อชำระยอดปิดบัญชีก่อนกำหนดครบ บัญชีเปลี่ยนเป็น Closed และงวดที่เหลือในตารางผ่อนถูกยกเลิก
resource: ../requirements/REQ-miniloan-004.md
tags: [miniloan, invariant]
id: BR-miniloan-023@v1
status: draft
belongs_to: REQ-miniloan-004
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-005]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:9fe13ae70cbb0d0e580d80d61622a6d63ac56c3614f303fc456318204b1a9658
---

# BR-miniloan-023@v1

## ข้อความของกฎ
เมื่อชำระยอดปิดบัญชีก่อนกำหนดครบ บัญชีเปลี่ยนเป็น Closed และงวดที่เหลือในตารางผ่อนถูกยกเลิก

## ที่มา

> "**Given** ชำระยอดปิดบัญชีครบ **When** บันทึก **Then** บัญชีเปลี่ยนเป็น `Closed` และงวดที่เหลือถูกยกเลิก"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-11

## พิสูจน์โดย

- [EX-miniloan-005](../examples/EX-miniloan-005.md) — alternate: หน้าบัญชีแสดงสถานะ "ปิดบัญชีแล้ว (Closed)" พร้อมข้อความ "ปิดบัญชีก่อนกำหนด — ชำระยอดปิดบัญชีครบเมื่อ {วันที่บันทึก}" และงวดที่ 6 ถึง 12 ในตารางผ่อนแสดงสถานะ "ยกเลิก (ปิดบัญชีก่อนกำหนด)" ตาม BR-miniloan-023@v1

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-023@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
