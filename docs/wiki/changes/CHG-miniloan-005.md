---
type: Change Set
title: ความขัดกันระหว่าง CALC-miniloan-001@v1 (คำนวณเต็มความละเอียดภายใน ปัดเฉพาะตอนเขี
description: กระทบ 1 โหนด · มีผล 2026-09-01
resource: ../rules/CALC-miniloan-001@v2.md
tags: [miniloan, change]
id: CHG-miniloan-005
requested_by: เจ้าของสเปก (ตอบสดในรอบ /req:change CALC-miniloan-001 นี้ — ยืนยันให้ CALC-miniloan-001 ปรับตาม BR-miniloan-035@v1 แทนที่จะแก้ BR-035 พร้อมปิด Q-miniloan-016 ในรอบเดียวกัน)
approved_by: เจ้าของสเปก
effective_from: 2026-09-01
affects: [CALC-miniloan-001@v2]
invalidates: [GD-miniloan-001]
triggered_by: []
timestamp: 2026-09-01T19:00:00+07:00
spec_hash: sha256:4b5eb97ba1dcff343a6d2de2c13187a6ba144e80f2f96c38dafd481f1913b034
---

# CHG-miniloan-005

## ทำไมถึงเปลี่ยน
ความขัดกันระหว่าง CALC-miniloan-001@v1 (คำนวณเต็มความละเอียดภายใน ปัดเฉพาะตอนเขียนแถว) กับ BR-miniloan-035@v1 (ปัดทันทีทุกจุดที่เกิดขึ้น) ถูกจับได้จาก Q-miniloan-016 หลังรัน GD-miniloan-001 จริง (คอลัมน์ยอดคงเหลือที่แสดงกับบัญชีภายในต่างกันระหว่างทางราว 0.02–0.17 บาท แม้ปิดที่ 0 พอดีเท่ากันตอนจบตาราง) · เจ้าของสเปกเลือกให้ CALC-miniloan-001 เป็นฝ่ายปรับตาม BR-miniloan-035@v1 แทนที่จะแก้ BR-035 — ผลคือไม่มีบัญชีภายในที่ละเอียดกว่าคอลัมน์ที่แสดงอีกต่อไป มีคอลัมน์เดียวที่ทุกกฎอ่านค่าได้โดยไม่กำกวม ซึ่งปิดคำถามท่อน (3) ของ Q-miniloan-016 ได้ทันที (BR-miniloan-022@v1 และ BR-miniloan-046@v1/BR-miniloan-050@v2 ไม่มีคอลัมน์คู่แข่งให้เลือกอ่านอีกต่อไป)

| เรื่อง | ค่า |
|---|---|
| ใครขอ | เจ้าของสเปก (ตอบสดในรอบ /req:change CALC-miniloan-001 นี้ — ยืนยันให้ CALC-miniloan-001 ปรับตาม BR-miniloan-035@v1 แทนที่จะแก้ BR-035 พร้อมปิด Q-miniloan-016 ในรอบเดียวกัน) |
| ใครอนุมัติ | เจ้าของสเปก |
| มีผลตั้งแต่ | 2026-09-01 |
| เอกสารที่ทำให้เปลี่ยน | — |

## เปลี่ยนอะไร

| โหนด | จาก | เป็น |
|---|---|---|
| [CALC-miniloan-001@v2](../calculations/CALC-miniloan-001@v2.md) | [CALC-miniloan-001@v1](../calculations/CALC-miniloan-001@v1.md) EMI = P × r × (1+r)^n / ((1+r)^n − 1) · P = เงินต้น · r = อั | EMI = P × r × (1+r)^n / ((1+r)^n − 1) · P = เงินต้น · r = อั |

## ต้องทำต่อ

- 🔴 [GD-miniloan-001](../golden/GD-miniloan-001.md) ใช้ไม่ได้แล้ว — ต้องคำนวณเลขเฉลยใหม่ (`/req:golden`)

## หมายเหตุ
BR-miniloan-016@v1 (กฎที่ CALC นี้ผูกอยู่) ไม่ต้องขึ้นเวอร์ชัน — ข้อความกฎไม่พูดถึงจังหวะปัดภายใน · ไม่มี example ใดพิสูจน์ CALC-miniloan-001 โดยตรง (EX-miniloan-097/098 พิสูจน์ BR-miniloan-016@v1 ระดับสูตร ไม่กระทบ) และไม่มี traces_down (ยังไม่ถึง design/build) จึง coverage ไม่ลดลงจากรอบนี้ · DQ-miniloan-001 ยังเปิดอยู่เหมือนเดิม คนละคำถามกับจังหวะปัด · ขั้นถัดไปที่ต้องทำแยก: /req:golden CALC-miniloan-001@v2 เพื่อสร้างเลขเฉลยใหม่ภายใต้จังหวะปัดนี้ ก่อนหน้านั้น check #13 (golden dataset verified) จะเตือนว่า CALC-miniloan-001@v2 ยังไม่มีเลขเฉลย
