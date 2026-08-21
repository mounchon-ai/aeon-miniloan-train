---
type: Business Rule
title: บันทึก Payment ตรงตามยอดงวดของบัญชี Active ที่มีงวดค้าง → งวดนั้นเปลี่ยนสถานะเป็
description: บันทึก Payment ตรงตามยอดงวดของบัญชี Active ที่มีงวดค้าง → งวดนั้นเปลี่ยนสถานะเป็น Paid และยอดคงเหลือของบัญชีลดลง
resource: ../requirements/REQ-miniloan-004.md
tags: [miniloan, invariant]
id: BR-miniloan-020@v1
status: draft
belongs_to: REQ-miniloan-004
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-013]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:544d192bfcc7148283d21712e44c7cbff448787fa9900a363c93e73c85316dc5
---

# BR-miniloan-020@v1

## ข้อความของกฎ
บันทึก Payment ตรงตามยอดงวดของบัญชี Active ที่มีงวดค้าง → งวดนั้นเปลี่ยนสถานะเป็น Paid และยอดคงเหลือของบัญชีลดลง

## ที่มา

> "**Given** บัญชี `Active` มีงวดค้าง **When** บันทึก Payment ตรงจำนวนงวด **Then** งวดนั้นสถานะ `Paid` และยอดคงเหลือลดลง"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-10

## พิสูจน์โดย

- [EX-miniloan-013](../examples/EX-miniloan-013.md) — happy: ระบบบันทึกการชำระสำเร็จ และแสดงข้อความ "บันทึกการชำระงวดที่ 3 เรียบร้อย" · งวดที่ 3 เปลี่ยนสถานะเป็น "จ่ายแล้ว" และยอดคงเหลือของบัญชีลดลงตามเงินต้นของงวดนั้น ตาม BR-miniloan-020@v1

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-020@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
