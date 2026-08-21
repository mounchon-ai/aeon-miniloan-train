---
type: Business Rule
title: ใบสมัครรับได้เฉพาะจำนวนเงินกู้ 10,000 – 1,000,000 บาท และจำนวนงวด 6 – 60 งวด (รา
description: ใบสมัครรับได้เฉพาะจำนวนเงินกู้ 10,000 – 1,000,000 บาท และจำนวนงวด 6 – 60 งวด (รายเดือน)
resource: ../requirements/REQ-miniloan-001.md
tags: [miniloan, constraint]
id: BR-miniloan-004@v1
status: draft
belongs_to: REQ-miniloan-001
kind: constraint
is_current: true
test_design: [BVA, EP]
proven_by: [EX-miniloan-023, EX-miniloan-024, EX-miniloan-025, EX-miniloan-026]
golden: []
provenance: [SRC-001, SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:644c5bd2b43581ade7523448c058b4c60221f05a28892a6e4a04c11514095401
---

# BR-miniloan-004@v1

## ข้อความของกฎ
ใบสมัครรับได้เฉพาะจำนวนเงินกู้ 10,000 – 1,000,000 บาท และจำนวนงวด 6 – 60 งวด (รายเดือน)

## ที่มา

> "จำนวนเงินกู้: 10,000 – 1,000,000 บาท · จำนวนงวด: 6 – 60 งวด (รายเดือน)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §5 · BR-04

> "**Given** จำนวนเงินกู้/งวด อยู่นอกช่วง BR-04 **When** กดยื่น **Then** ระบบแจ้ง error และไม่เปลี่ยนสถานะ"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-02

## พิสูจน์โดย

- [EX-miniloan-023](../examples/EX-miniloan-023.md) — happy: ระบบรับใบสมัคร ไม่มีข้อความเตือนเรื่องช่วงจำนวนเงินหรือจำนวนงวด และใบสมัครเดินต่อไปตาม BR-miniloan-007@v1
- [EX-miniloan-024](../examples/EX-miniloan-024.md) — boundary: ระบบรับใบสมัคร ไม่มีข้อความเตือน — ค่าที่ตรงขอบล่างพอดีถือว่าอยู่ในช่วง
- [EX-miniloan-025](../examples/EX-miniloan-025.md) — boundary: ระบบรับใบสมัคร ไม่มีข้อความเตือน — ค่าที่ตรงขอบบนพอดีถือว่าอยู่ในช่วงเช่นเดียวกับขอบล่าง
- [EX-miniloan-026](../examples/EX-miniloan-026.md) — exception: ระบบไม่เปลี่ยนสถานะใบสมัคร และแสดงข้อความครบทั้งสองข้อ — "จำนวนเงินกู้ 9,999 บาท ✗ ต้องอยู่ระหว่าง 10,000 – 1,000,000 บาท" · "จำนวนงวด 61 งวด ✗ ต้องอยู่ระหว่าง 6 – 60 งวด" · ใบสมัครยังเป็น "ร่าง (Draft)" ตาม BR-miniloan-007@v1

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-004@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
