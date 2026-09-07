---
type: Deferred Question
title: key ที่ใช้กันการยิงซ้ำของแต่ละคำสั่งคือฟิลด์อะไร (ยื่นใบสมัคร · อนุมัติ · เบิกจ่
description: key กันซ้ำคือคู่ (commandType, requestId) โดย requestId เป็นค่าที่ client ส่งมาต่อครั้ง (idempotency key) เก็บในตาราง IdempotencyKey พร้อม unique constraint ที่ฐานข้อมูล ครอบคลุมคำสั่งยื่นใบสมัคร อนุมัติ ปฏิเสธ ยกเลิก เบิกจ่าย และบันทึกการชำระ
resource: ../rules/BR-miniloan-043@v1.md
tags: [miniloan, question, idempotency]
id: DQ-miniloan-009
state: answered
raised_by: BR-miniloan-043@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:3c167ba6920323c3400f99856c3c3696a00a386a986d425f5e50818d28f25722
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
key กันซ้ำคือคู่ (commandType, requestId) โดย requestId เป็นค่าที่ client ส่งมาต่อครั้ง (idempotency key) เก็บในตาราง IdempotencyKey พร้อม unique constraint ที่ฐานข้อมูล ครอบคลุมคำสั่งยื่นใบสมัคร อนุมัติ ปฏิเสธ ยกเลิก เบิกจ่าย และบันทึกการชำระ

ตอบเมื่อ 2026-09-03T03:39:19Z

## ผลที่ตามมา

- `ENT-012`
