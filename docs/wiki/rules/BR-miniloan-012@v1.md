---
type: Business Rule
title: ถ้าจำนวนเงินกู้ที่ขอ > วงเงินอนุมัติสูงสุดตาม BR-miniloan-003@v1 ระบบต้องเตือนแล
description: ถ้าจำนวนเงินกู้ที่ขอ > วงเงินอนุมัติสูงสุดตาม BR-miniloan-003@v1 ระบบต้องเตือนและให้ปรับวงเงินก่อน จึงจะอนุมัติได้
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, invariant]
id: BR-miniloan-012@v1
status: draft
belongs_to: REQ-miniloan-002
kind: invariant
is_current: true
test_design: [BVA, decision_table]
proven_by: [EX-miniloan-052, EX-miniloan-053, EX-miniloan-054]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:45ea399cfbcb5d0431f332d9931a5b6310d80edddeb0810015f0069d952fb435
---

# BR-miniloan-012@v1

## ข้อความของกฎ
ถ้าจำนวนเงินกู้ที่ขอ > วงเงินอนุมัติสูงสุดตาม BR-miniloan-003@v1 ระบบต้องเตือนและให้ปรับวงเงินก่อน จึงจะอนุมัติได้

## ที่มา

> "**Given** วงเงินที่ขอ > วงเงินอนุมัติได้ (BR-03) **When** อนุมัติ **Then** ระบบเตือนและให้ปรับวงเงินก่อนอนุมัติ"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-05

## พิสูจน์โดย

- [EX-miniloan-052](../examples/EX-miniloan-052.md) — boundary: อนุมัติได้ทันที ไม่มีข้อความเตือนเรื่องวงเงิน · ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" — ขอเท่าวงเงินสูงสุดพอดีไม่ถือว่าเกิน
- [EX-miniloan-053](../examples/EX-miniloan-053.md) — exception: ระบบไม่อนุมัติ และแสดง "อนุมัติไม่ได้ — จำนวนเงินที่ขอ 150,001 บาท เกินวงเงินอนุมัติสูงสุด 150,000 บาท กรุณาปรับวงเงินก่อน" · ใบสมัครยังเป็น "อยู่ระหว่างพิจารณา (UnderReview)"
- [EX-miniloan-054](../examples/EX-miniloan-054.md) — alternate: อนุมัติสำเร็จ ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" ที่วงเงิน 150,000 บาท · ค่า DTI ในผลการประเมิน **ไม่ถูกคำนวณใหม่** ยังเป็นค่าที่คิดจากยอดที่ขอเดิม ตาม BR-miniloan-002@v1

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-012@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
