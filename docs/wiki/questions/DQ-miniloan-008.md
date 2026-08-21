---
type: Deferred Question
title: ข้อมูลที่เขียนลงไปแล้วก่อนการเรียก API จะล้มเหลว ต้อง rollback ทั้งก้อนหรือค้างค
description: ข้อมูลที่เขียนลงไปแล้วก่อนการเรียก API จะล้มเหลว ต้อง rollback ทั้งก้อนหรือค้างครึ่งทางได้ — ขอบเขตของ transaction อยู่ตรงไหน (เช่น เบิกจ่ายสำเร็จแต่สร้างตารางผ่อนไม่สำเร็จ ตาม BR-miniloan-015@v1)
resource: ../rules/BR-miniloan-042@v1.md
tags: [miniloan, question, integration]
id: DQ-miniloan-008
state: open
raised_by: BR-miniloan-042@v1
answer_phase: domain
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:82c2160cda9cbd91d29f60130ad4c20a0fd5104dd95796e915f87767877ce3ee
---

# DQ-miniloan-008

## คำถามที่เลื่อนไป
ข้อมูลที่เขียนลงไปแล้วก่อนการเรียก API จะล้มเหลว ต้อง rollback ทั้งก้อนหรือค้างครึ่งทางได้ — ขอบเขตของ transaction อยู่ตรงไหน (เช่น เบิกจ่ายสำเร็จแต่สร้างตารางผ่อนไม่สำเร็จ ตาม BR-miniloan-015@v1)

| เรื่อง | ค่า |
|---|---|
| สถานะ | 🛑 **open** — ยังไม่มีคำตอบ |
| ตั้งขึ้นจาก | [BR-miniloan-042@v1](../rules/BR-miniloan-042@v1.md) |
| หมวด | integration |
| ตอบตอนไหน | `/domain:ask` |
| ติดอยู่ที่ | `entity:LoanApplication / LoanAccount (ยังไม่มีขอบเขต aggregate)` |

