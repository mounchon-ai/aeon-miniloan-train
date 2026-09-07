---
type: Deferred Question
title: ถ้า Loan Officer ของใบสมัครเดิมถูกเปลี่ยน/มอบหมายใหม่หลังเบิกจ่ายไปแล้ว (บัญชีสิ
description: assignedOperationsId บน LoanAccount เป็นค่า snapshot ที่บันทึกครั้งเดียว ณ วันเบิกจ่ายจาก Loan Officer ของใบสมัครต้นทาง และไม่อัปเดตตามภายหลัง เพราะสเปกปัจจุบันไม่มีกลไกมอบหมายใหม่หลังเบิกจ่าย หากอนาคตมีกลไกนั้นจริง ต้องกลับมาแก้จุดนี้
resource: ../rules/BR-miniloan-054@v1.md
tags: [miniloan, question, data_scope]
id: DQ-miniloan-011
state: answered
raised_by: BR-miniloan-054@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:36d11e710ded6ee97b18c86bf5c122383532a5d809d6b30322ff24479846e013
---

# DQ-miniloan-011

## คำถามที่เลื่อนไป
ถ้า Loan Officer ของใบสมัครเดิมถูกเปลี่ยน/มอบหมายใหม่หลังเบิกจ่ายไปแล้ว (บัญชีสินเชื่อถูกสร้างไปแล้ว) บัญชีนั้นยังผูกกับ Operations ที่สืบทอดจาก Loan Officer คนเดิมตลอดไป หรืออัปเดตตาม Loan Officer คนใหม่ด้วย

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-054@v1](../rules/BR-miniloan-054@v1.md) |
| หมวด | data_scope |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:LoanAccount (ยังไม่มีฟิลด์ผู้รับผิดชอบที่สืบทอดจาก Loan Officer)` |

## คำตอบ
assignedOperationsId บน LoanAccount เป็นค่า snapshot ที่บันทึกครั้งเดียว ณ วันเบิกจ่ายจาก Loan Officer ของใบสมัครต้นทาง และไม่อัปเดตตามภายหลัง เพราะสเปกปัจจุบันไม่มีกลไกมอบหมายใหม่หลังเบิกจ่าย หากอนาคตมีกลไกนั้นจริง ต้องกลับมาแก้จุดนี้

ตอบเมื่อ 2026-09-03T03:39:19Z

## ผลที่ตามมา

- `ENT-006`
