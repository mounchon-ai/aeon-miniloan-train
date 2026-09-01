---
type: Calculation Contract
title: สัญญาการคำนวณของ BR-miniloan-022@v1
description: ยอดปิดบัญชีก่อนกำหนด (EarlySettlementAmount) = เงินต้นคงเหลือ (RemainingPrincipal) + ดอกเบี้ยค้างจ่าย (AccruedInterest) + ค่าธรรมเนียมปิดก่อนกำหนด (EarlySettlementFee) · EarlySettlementFee = RemainingPrincipal × 1% · AccruedInterest = RemainingPrincipal × AnnualRate × DaysElapsed / 365 · DaysElapsed = ClosingDate − LastPaidDueDate (จำนวนวันจริง) · 1% และ 365 เป็นค่าคงที่ในสูตร ไม่ใช่ input
resource: ../rules/BR-miniloan-022@v1.md
tags: [miniloan, calculation]
id: CALC-miniloan-004@v1
status: draft
constrains: BR-miniloan-022@v1
is_current: true
effective_from: 2026-09-01
numeric_type: decimal
rounding_mode: HALF_UP
golden: [GD-miniloan-005]
timestamp: 2026-09-01T20:00:00+07:00
spec_hash: sha256:a71ac588c4951156fff09a7330f26756ef9f866782ed20143068a978e0814213
---

# CALC-miniloan-004@v1

## สูตร

```
ยอดปิดบัญชีก่อนกำหนด (EarlySettlementAmount) = เงินต้นคงเหลือ (RemainingPrincipal) + ดอกเบี้ยค้างจ่าย (AccruedInterest) + ค่าธรรมเนียมปิดก่อนกำหนด (EarlySettlementFee) · EarlySettlementFee = RemainingPrincipal × 1% · AccruedInterest = RemainingPrincipal × AnnualRate × DaysElapsed / 365 · DaysElapsed = ClosingDate − LastPaidDueDate (จำนวนวันจริง) · 1% และ 365 เป็นค่าคงที่ในสูตร ไม่ใช่ input
```

ผูกกับกฎ [BR-miniloan-022@v1](../rules/BR-miniloan-022@v1.md)

## ตัวแปรเข้า

| ชื่อ | ชนิด | ความหมาย |
|---|---|---|
| `remaining_principal` | money(2) | เงินต้นคงเหลือ ณ วันที่ปิด — อ่านจากคอลัมน์ยอดคงเหลือของตารางผ่อนฉบับล่าสุดตาม BR-miniloan-053@v1 เท่านั้น ไม่คำนวณขึ้นใหม่จากสูตรอื่น |
| `annual_rate` | rate(10) | อัตราดอกเบี้ยต่อปีของเวอร์ชันที่ผูกกับบัญชีตอนสร้างตารางผ่อน ตาม BR-miniloan-036@v1 และ BR-miniloan-037@v1 — ไม่ใช่อัตรา master ปัจจุบัน |
| `last_paid_due_date` | date | วันครบกำหนดของงวดล่าสุดที่ชำระแล้ว · ถ้ายังไม่มีงวดไหนถูกชำระเลย ใช้วันเบิกจ่ายของบัญชีแทน |
| `closing_date` | date | วันที่ขอปิดบัญชี ป้อนโดย Operations/Applicant · ต้อง ≥ last_paid_due_date มิฉะนั้นสัญญาปฏิเสธ input ก่อนเข้าสูตร |

## การปัดเศษ — ส่วนที่ทำให้ตัวเลขต่างกันได้ทั้งที่สูตรเหมือนกัน

| เรื่อง | ค่า |
|---|---|
| ชนิดตัวเลข | decimal |
| วิธีปัด | HALF_UP |
| ปัดตรงไหน | ปัดทันทีเฉพาะที่บรรทัดดอกเบี้ยค้างจ่ายและบรรทัดค่าธรรมเนียมปิดก่อนกำหนด เป็นค่าเดียวแต่ละบรรทัด (2 ตำแหน่ง) สอดคล้องกับ BR-miniloan-035@v1 · เงินต้นคงเหลือเป็นค่าที่ปัดมาแล้วจากคอลัมน์ตารางผ่อน (ตาม CALC-miniloan-001@v2) อ่านตรงๆ ไม่ปัดซ้ำ · ผลรวมทั้งสามบรรทัด (เงินต้นคงเหลือ + ดอกเบี้ยที่ปัดแล้ว + ค่าธรรมเนียมที่ปัดแล้ว) ไม่ปัดซ้ำอีกชั้น เพราะทุกตัวเป็น 2 ตำแหน่งอยู่แล้วรวมกันยังคงเป็น 2 ตำแหน่งพอดี |
| เศษที่เหลือ | — |

## พฤติกรรมที่ขอบ

- closing_date เท่ากับ last_paid_due_date พอดี (ช่วงนับยาว 0 วัน) → ดอกเบี้ยค้างจ่าย = 0.00 บาท เป็นศูนย์โดยนิยาม ไม่ใช่ผลจากการปัด (EX-miniloan-075)
- closing_date = last_paid_due_date + 1 วัน → ดอกเบี้ยค้างจ่ายของ 1 วันเต็ม คำนวณจากสูตรตามปกติ ไม่ปัดขึ้นเป็นทั้งงวดหรือทั้งเดือน ตัวหารคือ 365 เท่านั้น (EX-miniloan-076)
- closing_date ก่อน last_paid_due_date (ป้อนย้อนหลัง) → สัญญาปฏิเสธ input ทันทีก่อนเข้าสูตร ไม่คำนวณต่อและไม่คืนค่าดอกเบี้ยติดลบ
- ยังไม่มีงวดไหนถูกชำระเลย (ขอยอดปิดบัญชีก่อนงวดแรกถึงกำหนด) → last_paid_due_date อ้างอิงวันเบิกจ่ายของบัญชีแทน ไม่ใช่ค่าว่างหรือ error
- annual_rate = 0% ต่อปี → AccruedInterest = 0.00 บาทเสมอไม่ว่า DaysElapsed จะเป็นเท่าไร (ตัวเศษเป็น 0 ไม่ใช่ตัวหาร จึงไม่มีปัญหาหารด้วยศูนย์) — ขอบเดียวกับที่ CALC-miniloan-001@v2 นิยามไว้สำหรับอัตรา 0%

## เลขเฉลย

- [GD-miniloan-005](../golden/GD-miniloan-005.md) — 7 แถว · ✅ aplus191

## คำถามที่ผูกอยู่

- [DQ-miniloan-001](../questions/DQ-miniloan-001.md)

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล |
|---|---|---|
| **CALC-miniloan-004@v1** (หน้านี้) ✅ | 2026-09-01 | ตั้งต้น |

## หมายเหตุ
เจ็ดช่องตอบครบในรอบเดียว ทุกข้อเลือกตามค่าที่ระบบเสนอ (ดาว) ไม่มีข้อไหน "ยังไม่แน่ใจ" จึงไม่มีการ์ดแดงใหม่ · เงินต้นคงเหลือไม่ใช่ input ที่คำนวณเอง แต่อ่านจาก BR-miniloan-053@v1 ตรงๆ ทำให้สัญญานี้ไม่มีความเสี่ยงเรื่องบัญชีคู่ขนานแบบที่เคยเกิดกับ CALC-miniloan-001@v1 (ดู Q-miniloan-016 ที่ปิดไปแล้ว) · DQ-miniloan-001 (decimal(p,s) กี่ตำแหน่ง) ยังเปิดอยู่เหมือนเดิม ผูกไว้เพราะสัญญานี้ก็ใช้ money(2) เป็นสมมติฐานทำงานเช่นเดียวกับ CALC-miniloan-001/002/003 — ยังไม่ใช่คำตอบที่ปิดแล้ว
