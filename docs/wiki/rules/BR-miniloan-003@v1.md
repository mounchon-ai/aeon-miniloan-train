---
type: Business Rule
title: วงเงินอนุมัติสูงสุด = 5 เท่าของรายได้ต่อเดือน และไม่เกิน 1,000,000 บาท
description: วงเงินอนุมัติสูงสุด = 5 เท่าของรายได้ต่อเดือน และไม่เกิน 1,000,000 บาท
resource: ../requirements/REQ-miniloan-001.md
tags: [miniloan, calculation]
id: BR-miniloan-003@v1
status: draft
belongs_to: REQ-miniloan-001
kind: calculation
is_current: true
test_design: [BVA]
constrained_by: CALC-miniloan-005@v1
proven_by: [EX-miniloan-019, EX-miniloan-020, EX-miniloan-021, EX-miniloan-022]
golden: [GD-miniloan-006]
provenance: [SRC-001]
timestamp: 2026-08-24T09:30:00+07:00
spec_hash: sha256:dc0973a8542148b9c6617fcb6f4e17a61f325b0374e3c060db7fe8583f512165
---

# BR-miniloan-003@v1

## ข้อความของกฎ
วงเงินอนุมัติสูงสุด = 5 เท่าของรายได้ต่อเดือน และไม่เกิน 1,000,000 บาท

คำนวณตามสัญญา [CALC-miniloan-005@v1](../calculations/CALC-miniloan-005@v1.md)

## ที่มา

> "วงเงินอนุมัติสูงสุด = 5 เท่าของรายได้ต่อเดือน และไม่เกิน 1,000,000 บาท"
> — [SRC-001](../sources/SRC-001.md) หน้า — §5 · BR-03

## พิสูจน์โดย

- [EX-miniloan-019](../examples/EX-miniloan-019.md) — happy: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 150,000 บาท (5 เท่าของรายได้ 30,000 บาท/เดือน)" — เพดาน 1,000,000 บาทไม่ได้เข้ามาเกี่ยวข้องในเคสนี้
- [EX-miniloan-020](../examples/EX-miniloan-020.md) — boundary: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 999,995 บาท (5 เท่าของรายได้ 199,999 บาท/เดือน)" — ยังใช้ค่าจากสูตร ไม่ใช่ค่าจากเพดาน
- [EX-miniloan-021](../examples/EX-miniloan-021.md) — boundary: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 1,000,000 บาท" — จุดนี้คือรอยต่อที่สูตรกับเพดานให้คำตอบตรงกัน โค้ดที่เขียนเงื่อนไขผิดด้าน (ใช้ < แทน ≤ หรือกลับกัน) จะพลาดที่ค่านี้ค่าเดียว
- [EX-miniloan-022](../examples/EX-miniloan-022.md) — boundary: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 1,000,000 บาท (ถูกจำกัดด้วยเพดาน 1,000,000 บาท ไม่ใช่ 5 เท่าของรายได้)" — ต้องบอกด้วยว่าอะไรเป็นตัวจำกัด ไม่ใช่แสดงแต่ตัวเลข เพราะ BR-miniloan-009@v1 บังคับให้ผลประเมินมีเหตุผลกำกับ
- [GD-miniloan-006](../golden/GD-miniloan-006.md) — เลขเฉลย 5 แถว · ✅ mounc 2026-08-22

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-003@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
