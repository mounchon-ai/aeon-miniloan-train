---
type: Deferred Question
title: key ที่ใช้กันการยิงซ้ำของแต่ละคำสั่งคือฟิลด์อะไร (ยื่นใบสมัคร · อนุมัติ · เบิกจ่
description: ใช้ requestId ที่ client ส่งมาต่อคำสั่งหนึ่งครั้ง (ไม่ใช่ natural key) เก็บไว้ใน IdempotencyRecord พร้อม unique constraint — เพราะบางคำสั่ง (เช่น อนุมัติ) ไม่มี natural key ที่แยกความพยายามซ้ำจากคำสั่งที่ตั้งใจสั่งซ้ำจริงได้
resource: ../rules/BR-miniloan-043@v1.md
tags: [miniloan, question, idempotency]
id: DQ-miniloan-009
state: answered
raised_by: BR-miniloan-043@v1
answer_phase: domain
timestamp: 2026-09-01T20:00:00+07:00
spec_hash: sha256:fadf53f2177b43fd78ffb6318e7bca8a2487222be222b1ca708d87330b7e7f6f
---

# DQ-miniloan-009

## คำถามที่เลื่อนไป
key ที่ใช้กันการยิงซ้ำของแต่ละคำสั่งคือฟิลด์อะไร (ยื่นใบสมัคร · อนุมัติ · เบิกจ่าย · บันทึกการชำระ) — natural key ที่มีอยู่แล้ว หรือ key ที่ client ส่งมาต่อครั้ง

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-043@v1](../rules/BR-miniloan-043@v1.md) |
| หมวด | idempotency |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:LoanApplication / Payment (ยังไม่มีฟิลด์ให้ตั้ง unique constraint)` |

## คำตอบ
ใช้ requestId ที่ client ส่งมาต่อคำสั่งหนึ่งครั้ง (ไม่ใช่ natural key) เก็บไว้ใน IdempotencyRecord พร้อม unique constraint — เพราะบางคำสั่ง (เช่น อนุมัติ) ไม่มี natural key ที่แยกความพยายามซ้ำจากคำสั่งที่ตั้งใจสั่งซ้ำจริงได้

ตอบเมื่อ 2026-09-01T18:00:00+07:00

## ผลที่ตามมา

- `ENT-014`
