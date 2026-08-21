---
type: Deferred Question
title: key ที่ใช้กันการยิงซ้ำของแต่ละคำสั่งคือฟิลด์อะไร (ยื่นใบสมัคร · อนุมัติ · เบิกจ่
description: key ที่ใช้กันการยิงซ้ำของแต่ละคำสั่งคือฟิลด์อะไร (ยื่นใบสมัคร · อนุมัติ · เบิกจ่าย · บันทึกการชำระ) — natural key ที่มีอยู่แล้ว หรือ key ที่ client ส่งมาต่อครั้ง
resource: ../rules/BR-miniloan-043@v1.md
tags: [miniloan, question, idempotency]
id: DQ-miniloan-009
state: open
raised_by: BR-miniloan-043@v1
answer_phase: domain
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:d5df352641dab1c88e57634682a6ca6da714eb7c00118e2df6fd1a8782a9a3b6
---

# DQ-miniloan-009

## คำถามที่เลื่อนไป
key ที่ใช้กันการยิงซ้ำของแต่ละคำสั่งคือฟิลด์อะไร (ยื่นใบสมัคร · อนุมัติ · เบิกจ่าย · บันทึกการชำระ) — natural key ที่มีอยู่แล้ว หรือ key ที่ client ส่งมาต่อครั้ง

| เรื่อง | ค่า |
|---|---|
| สถานะ | 🛑 **open** — ยังไม่มีคำตอบ |
| ตั้งขึ้นจาก | [BR-miniloan-043@v1](../rules/BR-miniloan-043@v1.md) |
| หมวด | idempotency |
| ตอบตอนไหน | `/domain:ask` |
| ติดอยู่ที่ | `entity:LoanApplication / Payment (ยังไม่มีฟิลด์ให้ตั้ง unique constraint)` |

