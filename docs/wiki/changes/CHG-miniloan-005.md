---
type: Change Set
title: Q-miniloan-016 ท่อน (3) เผยว่า CALC-miniloan-001@v1 (เก็บค่าเต็มความละเอียดภายใน
description: กระทบ 1 โหนด · มีผล 2026-08-21
resource: ../rules/CALC-miniloan-001@v2.md
tags: [miniloan, change]
id: CHG-miniloan-005
requested_by: เจ้าของสเปก (ตอบผ่าน AskUserQuestion ที่ routed มาจากการตรวจ Q-miniloan-016 ในรอบ /req:check — เลือกย้าย CALC ให้ตรงกับ BR-miniloan-035@v1 แทนที่จะแก้กฎให้ยกเว้นสัญญานี้)
approved_by: เจ้าของสเปก
effective_from: 2026-08-21
affects: [CALC-miniloan-001@v2]
invalidates: [GD-miniloan-001]
triggered_by: []
timestamp: 2026-08-24T09:30:00+07:00
spec_hash: sha256:951ac3bc88ea3bb8d809de9562b66e186f6d3a1bbe78d69ac57257708907ffdd
---

# CHG-miniloan-005

## ทำไมถึงเปลี่ยน
Q-miniloan-016 ท่อน (3) เผยว่า CALC-miniloan-001@v1 (เก็บค่าเต็มความละเอียดภายใน ปัดเฉพาะตอนแสดง) ขัดกับ BR-miniloan-035@v1 (ตอบไปแล้วว่าต้องปัดทุกจุดทันทีที่เกิด ไม่เก็บค่าเต็มไว้) — สัญญาเขียนก่อนกฎข้อนั้นจะเกิดจึงไม่เคยรู้จักกัน เจ้าของสเปกเลือกย้ายสัญญาให้ตรงกับกฎที่ตอบแล้ว แทนที่จะแก้กฎให้ยกเว้นสัญญานี้

| เรื่อง | ค่า |
|---|---|
| ใครขอ | เจ้าของสเปก (ตอบผ่าน AskUserQuestion ที่ routed มาจากการตรวจ Q-miniloan-016 ในรอบ /req:check — เลือกย้าย CALC ให้ตรงกับ BR-miniloan-035@v1 แทนที่จะแก้กฎให้ยกเว้นสัญญานี้) |
| ใครอนุมัติ | เจ้าของสเปก |
| มีผลตั้งแต่ | 2026-08-21 |
| เอกสารที่ทำให้เปลี่ยน | — |

## เปลี่ยนอะไร

| โหนด | จาก | เป็น |
|---|---|---|
| [CALC-miniloan-001@v2](../calculations/CALC-miniloan-001@v2.md) | [CALC-miniloan-001@v1](../calculations/CALC-miniloan-001@v1.md) EMI = P × r × (1+r)^n / ((1+r)^n − 1) · P = เงินต้น · r = อั | EMI = P × r × (1+r)^n / ((1+r)^n − 1) · P = เงินต้น · r = อั |

## ต้องทำต่อ

- 🔴 [GD-miniloan-001](../golden/GD-miniloan-001.md) ใช้ไม่ได้แล้ว — ต้องคำนวณเลขเฉลยใหม่ (`/req:golden`)

## หมายเหตุ
GD-miniloan-001 คำนวณภายใต้กลไก @v1 (บัญชีภายในเก็บค่าเต็ม) จึงไม่ใช่หลักฐานของ @v2 อีกต่อไป ต้องรัน /req:golden CALC-miniloan-001@v2 ใหม่ทั้งหมด (ทั้ง C2 และ C4) ก่อนจะปิด Q-miniloan-016 ได้จริง · BR-miniloan-016@v1 (constrained_by) อัปเดตให้ชี้ @v2 แล้ว ข้อความของกฎเองไม่เปลี่ยน (สูตร EMI เดิมทุกตัวอักษร) จึงไม่ต้องขึ้นเวอร์ชันกฎ · EX-miniloan-147 (พิสูจน์ BR-miniloan-053@v1) ใช้ผลต่างที่กลไก @v1 สร้างไว้เป็นหลักฐาน boundary — ผลต่างนั้นหายไปใต้ @v2 ต้องรีวิวผ่าน /req:example BR-miniloan-053@v1 แยกต่างหาก เป็นขั้นที่ยังไม่อนุมัติในรอบนี้ · Q-miniloan-016 ยังไม่ปิด — เหลือรอผลการรัน /req:golden จริงมายืนยัน
