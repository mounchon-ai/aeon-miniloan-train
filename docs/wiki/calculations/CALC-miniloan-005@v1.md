---
type: Calculation Contract
title: สัญญาการคำนวณของ BR-miniloan-050@v2
description: ค่าธรรมเนียมการโปะ (PaydownFee) = ส่วนเกิน (Overpayment) × 1% · เงินต้นที่ตัดได้ (PrincipalReduction) = Overpayment − PaydownFee (ที่ปัดแล้ว) · ถ้า PrincipalReduction > เงินต้นคงเหลือ (RemainingPrincipal) → ปฏิเสธการบันทึกทั้งรายการตาม BR-miniloan-052@v2 ไม่คำนวณต่อ · ถ้าไม่เกิน → เงินต้นคงเหลือใหม่ = RemainingPrincipal − PrincipalReduction · 1% เป็นค่าคงที่ในสูตร ไม่ใช่ input
resource: ../rules/BR-miniloan-050@v2.md
tags: [miniloan, calculation]
id: CALC-miniloan-005@v1
status: draft
constrains: BR-miniloan-050@v2
is_current: true
effective_from: 2026-09-01
numeric_type: decimal
rounding_mode: HALF_UP
golden: [GD-miniloan-006]
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:196c6553840ec4791e96ac793feae63f7808963c1a63ff1f7ce5f06f3467e35c
---

# CALC-miniloan-005@v1

## สูตร

```
ค่าธรรมเนียมการโปะ (PaydownFee) = ส่วนเกิน (Overpayment) × 1% · เงินต้นที่ตัดได้ (PrincipalReduction) = Overpayment − PaydownFee (ที่ปัดแล้ว) · ถ้า PrincipalReduction > เงินต้นคงเหลือ (RemainingPrincipal) → ปฏิเสธการบันทึกทั้งรายการตาม BR-miniloan-052@v2 ไม่คำนวณต่อ · ถ้าไม่เกิน → เงินต้นคงเหลือใหม่ = RemainingPrincipal − PrincipalReduction · 1% เป็นค่าคงที่ในสูตร ไม่ใช่ input
```

ผูกกับกฎ [BR-miniloan-050@v2](../rules/BR-miniloan-050@v2.md)

## ตัวแปรเข้า

| ชื่อ | ชนิด | ความหมาย |
|---|---|---|
| `overpayment` | money(2) | ส่วนเกินยอดงวดที่นำไปโปะเงินต้น — คำนวณจากยอดที่ชำระลบยอดงวด เป็นหน้าที่ของ BR-miniloan-046@v2 ไม่ใช่ของสัญญานี้ สัญญานี้รับเป็น input ตรงๆ |
| `remaining_principal` | money(2) | เงินต้นคงเหลือก่อนโปะ — อ่านจากคอลัมน์ยอดคงเหลือของตารางผ่อนฉบับล่าสุดตาม BR-miniloan-053@v1 เท่านั้น ไม่คำนวณขึ้นใหม่ |

## การปัดเศษ — ส่วนที่ทำให้ตัวเลขต่างกันได้ทั้งที่สูตรเหมือนกัน

| เรื่อง | ค่า |
|---|---|
| ชนิดตัวเลข | decimal |
| วิธีปัด | HALF_UP |
| ปัดตรงไหน | ปัดทันทีเฉพาะที่บรรทัดค่าธรรมเนียมการโปะ (2 ตำแหน่ง) สอดคล้องกับ BR-miniloan-035@v1 · เงินต้นที่ตัดได้ = ส่วนเกิน − ค่าธรรมเนียมที่ปัดแล้ว (หักเอา ไม่ปัดซ้ำ) · เงินต้นคงเหลือใหม่ = เงินต้นคงเหลือ − เงินต้นที่ตัดได้ (ลบตรงๆ ไม่ปัดซ้ำ เพราะทั้งสองค่าเป็น 2 ตำแหน่งอยู่แล้ว) |
| เศษที่เหลือ | — |

## พฤติกรรมที่ขอบ

- overpayment น้อยมาก (เช่น 0.01 บาท) → ค่าธรรมเนียม 1% ของ 0.01 = 0.0001 ปัดด้วย HALF_UP เป็น 0.00 บาท (EX-miniloan-112) — เงินต้นที่ตัดได้เท่ากับ overpayment เต็มจำนวน
- PrincipalReduction เท่ากับ RemainingPrincipal พอดี (เหลือ 0.00) → รับ ไม่ปฏิเสธ เงินต้นคงเหลือใหม่ปิดที่ 0.00 พอดี (EX-miniloan-148)
- PrincipalReduction มากกว่า RemainingPrincipal แม้เพียงหนึ่งสตางค์ → ปฏิเสธการบันทึกทั้งรายการตาม BR-miniloan-052@v2 ไม่ตัดเท่าที่เหลือแล้วทอนคืนส่วนล้น (EX-miniloan-149) — ขอบระหว่างรับกับปฏิเสธคมที่หนึ่งสตางค์
- อัตราค่าธรรมเนียม 1% เป็นค่าคงที่ตายตัวในสูตร ไม่มีเวอร์ชันหรือปรับได้เหมือนอัตราดอกเบี้ยตาม BR-miniloan-036/037@v1

## เลขเฉลย

- [GD-miniloan-006](../golden/GD-miniloan-006.md) — 5 แถว · ✅ aplus191

## คำถามที่ผูกอยู่

- [DQ-miniloan-001](../questions/DQ-miniloan-001.md)

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล |
|---|---|---|
| **CALC-miniloan-005@v1** (หน้านี้) ✅ | 2026-09-01 | ตั้งต้น |

## หมายเหตุ
เจ็ดช่องตอบครบในรอบเดียว ทุกข้อเลือกตามค่าที่ระบบเสนอ ไม่มีข้อไหน "ยังไม่แน่ใจ" จึงไม่มีการ์ดแดงใหม่ · ตัวเลขที่ EX-miniloan-111/112/148/149 เคยระบุไว้ตรงๆ (เช่น 5,050.51 → ค่าธรรมเนียม 50.51) เป็นการยืนยันล่วงหน้าโดยเจ้าของสเปกในรอบ /req:example ก่อนมีสัญญานี้ — /req:golden BR-miniloan-050@v2 ที่จะตามมาต้องรันจริงเพื่อยืนยันว่าตรงกับสัญญานี้ ไม่ใช่ถือว่าตรงอยู่แล้วเพราะเคยเขียนไว้ · DQ-miniloan-001 (decimal(p,s) กี่ตำแหน่ง) ยังเปิดอยู่เหมือนเดิม ผูกไว้เพราะสัญญานี้ใช้ money(2) เป็นสมมติฐานทำงานเช่นเดียวกับ CALC-miniloan-001/002/003/004
