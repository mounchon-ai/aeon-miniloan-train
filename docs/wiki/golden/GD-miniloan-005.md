---
type: Golden Dataset
title: เลขเฉลยของ CALC-miniloan-004@v1 · BR-miniloan-022@v1
description: 7 แถว · ยืนยันแล้ว
resource: ../rules/BR-miniloan-022@v1.md
tags: [miniloan, golden]
id: GD-miniloan-005
status: validated
proves: [CALC-miniloan-004@v1, BR-miniloan-022@v1]
verified_by: aplus191
verified_at: 2026-09-01T19:30+07:00
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:64331713d6bf9e5989aaee2a53224fc9c7875881bd33d725d8505fae18003e65
---

# GD-miniloan-005

## สถานะการยืนยัน

✅ **aplus191** ยืนยันเมื่อ 2026-09-01T19:30+07:00 — ตัวเลขชุดนี้ใช้ยันกับลูกค้าได้

คำนวณโดย `golden/CALC-miniloan-004@v1.mjs` เมื่อ 2026-09-01T19:00+07:00

## พิสูจน์

- [BR-miniloan-022@v1](../rules/BR-miniloan-022@v1.md) — ยอดปิดบัญชีก่อนกำหนด = เงินต้นคงเหลือ + ดอกเบี้ยค้างจ่ายถึงวันที่ปิด + ค่าธรรมเนียมปิดก่อนกำหนด 1% ของเงินต้นคงเหลือ · ไม่คิดดอกเบี้ยของงวดในอนาคตที่ยังไม่ถึงกำหนด · ดอกเบี้ยค้างจ่ายใช้ฐานนับวัน actual/365 (นับวันจริง หารด้วย 365) นับจากวันครบกำหนดงวดล่าสุดที่ชำระแล้วถึงวันที่ปิด — ปิดตรงวันครบกำหนดที่เพิ่งชำระ ดอกเบี้ยค้างจ่ายเป็น 0
- [CALC-miniloan-004@v1](../calculations/CALC-miniloan-004@v1.md) — ยอดปิดบัญชีก่อนกำหนด (EarlySettlementAmount) = เงินต้นคงเหลือ (RemainingPrincipal) + ดอกเบี้ยค้างจ่าย (AccruedInterest) + ค่าธรรมเนียมปิดก่อนกำหนด (EarlySettlementFee) · EarlySettlementFee = RemainingPrincipal × 1% · AccruedInterest = RemainingPrincipal × AnnualRate × DaysElapsed / 365 · DaysElapsed = ClosingDate − LastPaidDueDate (จำนวนวันจริง) · 1% และ 365 เป็นค่าคงที่ในสูตร ไม่ใช่ input

## ตาราง (7 แถว)

| remaining_principal | annual_rate | last_paid_due_date | closing_date | computed | days_elapsed | remaining_principal | accrued_interest | early_settlement_fee | early_settlement_amount | declined_by | reason | มาจากแถวไหน |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 50000.00 | 0.25 | 2026-06-15 | 2026-06-25 | true | 10 | 50,000.00 | 342.47 | 500.00 | 50,842.47 | — | — | — |
| 50000.00 | 0.25 | 2026-06-15 | 2026-06-15 | true | 0 | 50,000.00 | 0.00 | 500.00 | 50,500.00 | — | — | — |
| 50000.00 | 0.25 | 2026-06-15 | 2026-06-16 | true | 1 | 50,000.00 | 34.25 | 500.00 | 50,534.25 | — | — | — |
| 50000.00 | 0.25 | 2026-06-15 | 2026-06-14 | false | — | — | — | — | — | CALC-miniloan-004@v1 boundary_behavior | closing_date (2026-06-14) ก่อน last_paid_due_date (2026-06-15) — ป้อนย้อนหลัง สัญญาปฏิเสธ input ก่อนเข้าสูตร | — |
| 50000.00 | 0 | 2026-06-15 | 2026-07-15 | true | 30 | 50,000.00 | 0.00 | 500.00 | 50,500.00 | — | — | — |
| 100000.00 | 0.25 | 2026-01-10 | 2026-01-20 | true | 10 | 100,000.00 | 684.93 | 1,000.00 | 101,684.93 | — | — | — |
| 73456.78 | 0.18 | 2026-03-01 | 2026-04-17 | true | 47 | 73,456.78 | 1,702.59 | 734.57 | 75,893.94 | — | — | — |

## หมายเหตุ
ไม่มี source ชนิด sample_data ผูกกับ REQ-miniloan-004 จึงไม่มีแถวลูกค้าให้เทียบ mismatches[] ว่างเพราะไม่มีอะไรให้เทียบ ไม่ใช่เพราะเทียบแล้วตรง · remaining_principal ในทุกเคสเป็นค่าที่ตั้งไว้ตรงๆ (input ของสัญญานี้) ไม่ได้ไล่มาจากตารางผ่อนจริงของ CALC-miniloan-001 — สัญญานี้ไม่รู้จักและไม่ต้องรู้จักที่มาของมัน ตาม BR-miniloan-053@v1 · C1 ใช้ช่วงวันเดียวกับ EX-miniloan-074/075/076 (15→25 มิ.ย.) เพื่อให้อ่านคู่กับตัวอย่างเหล่านั้นได้ แต่เงินต้น 50,000.00 เป็นค่าสมมติแยกต่างหาก ไม่ใช่ยอดคงเหลือจริงของบัญชี 100,000/12 งวดใน EX-074 · ยังไม่ verified — รอเจ้าของสเปกยืนยันตาราง
