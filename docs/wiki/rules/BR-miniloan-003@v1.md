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
constrained_by: CALC-miniloan-003@v1
proven_by: [EX-miniloan-019, EX-miniloan-020, EX-miniloan-021, EX-miniloan-022]
golden: [GD-miniloan-004]
provenance: [SRC-001]
timestamp: 2026-09-01T20:00:00+07:00
spec_hash: sha256:d25d16813bd40dbe7a46d2cbcb7646ee434c801cf98d5e53bb8532399a584dea
---

# BR-miniloan-003@v1

## ข้อความของกฎ
วงเงินอนุมัติสูงสุด = 5 เท่าของรายได้ต่อเดือน และไม่เกิน 1,000,000 บาท

คำนวณตามสัญญา [CALC-miniloan-003@v1](../calculations/CALC-miniloan-003@v1.md)

## ที่มา

> "วงเงินอนุมัติสูงสุด = 5 เท่าของรายได้ต่อเดือน และไม่เกิน 1,000,000 บาท"
> — [SRC-001](../sources/SRC-001.md) หน้า — §5 · BR-03

## พิสูจน์โดย

- [EX-miniloan-019](../examples/EX-miniloan-019.md) — happy: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 150,000 บาท (5 เท่าของรายได้ 30,000 บาท/เดือน)" — เพดาน 1,000,000 บาทไม่ได้เข้ามาเกี่ยวข้องในเคสนี้
- [EX-miniloan-020](../examples/EX-miniloan-020.md) — boundary: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 999,995 บาท (5 เท่าของรายได้ 199,999 บาท/เดือน)" — ยังใช้ค่าจากสูตร ไม่ใช่ค่าจากเพดาน
- [EX-miniloan-021](../examples/EX-miniloan-021.md) — boundary: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 1,000,000 บาท" — จุดนี้คือรอยต่อที่สูตรกับเพดานให้คำตอบตรงกัน โค้ดที่เขียนเงื่อนไขผิดด้าน (ใช้ < แทน ≤ หรือกลับกัน) จะพลาดที่ค่านี้ค่าเดียว
- [EX-miniloan-022](../examples/EX-miniloan-022.md) — boundary: หน้าผลการประเมินแสดง "วงเงินที่อนุมัติได้ 1,000,000 บาท (ถูกจำกัดด้วยเพดาน 1,000,000 บาท ไม่ใช่ 5 เท่าของรายได้)" — ต้องบอกด้วยว่าอะไรเป็นตัวจำกัด ไม่ใช่แสดงแต่ตัวเลข เพราะ BR-miniloan-009@v1 บังคับให้ผลประเมินมีเหตุผลกำกับ
- [GD-miniloan-004](../golden/GD-miniloan-004.md) — เลขเฉลย 6 แถว · ✅ aplus191 2026-09-01

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-003@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
