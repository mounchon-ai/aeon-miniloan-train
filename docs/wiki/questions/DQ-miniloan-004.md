---
type: Deferred Question
title: แดชบอร์ดและยอดรวมตาม BR-miniloan-024@v1 ใช้ขอบเขตข้อมูลเดียวกับการเปิดดูรายใบหรื
description: ใช้ขอบเขตเดียวกับการเปิดดูรายใบ — Loan Officer เห็นจำนวนเฉพาะใบสมัคร/บัญชีที่ตัวเองถูกมอบหมายเท่านั้น สอดคล้องกับคำตอบของ DQ-miniloan-002
resource: ../rules/BR-miniloan-033@v1.md
tags: [miniloan, question, data_scope]
id: DQ-miniloan-004
state: answered
raised_by: BR-miniloan-033@v1
answer_phase: domain
timestamp: 2026-09-01T18:00:00+07:00
spec_hash: sha256:9a1c8b7617b38ed215031add0a5a4b9e7c2e082260eabc5ded871a677c288763
---

# DQ-miniloan-004

## คำถามที่เลื่อนไป
แดชบอร์ดและยอดรวมตาม BR-miniloan-024@v1 ใช้ขอบเขตข้อมูลเดียวกับการเปิดดูรายใบหรือไม่ — Loan Officer เห็นจำนวนของทั้งระบบ หรือเห็นเฉพาะที่ตัวเองถูกมอบหมาย

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-033@v1](../rules/BR-miniloan-033@v1.md) |
| หมวด | data_scope |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:LoanApplication / LoanAccount (ต้องรู้ก่อนว่าฟิลด์เจ้าของและผู้รับมอบหมายอยู่ที่ไหน)` |

## คำตอบ
ใช้ขอบเขตเดียวกับการเปิดดูรายใบ — Loan Officer เห็นจำนวนเฉพาะใบสมัคร/บัญชีที่ตัวเองถูกมอบหมายเท่านั้น สอดคล้องกับคำตอบของ DQ-miniloan-002

ตอบเมื่อ 2026-09-01T18:00:00+07:00

## ผลที่ตามมา

- `ENT-001`
- `ENT-007`
