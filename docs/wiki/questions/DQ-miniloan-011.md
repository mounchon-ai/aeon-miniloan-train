---
type: Deferred Question
title: ถ้า Loan Officer ของใบสมัครเดิมถูกเปลี่ยน/มอบหมายใหม่หลังเบิกจ่ายไปแล้ว (บัญชีสิ
description: บัญชีสินเชื่อสืบทอด Operations ผู้ดูแลจาก Loan Officer ที่ถูกมอบหมายในใบสมัครต้นทางโดยอัตโนมัติตอนเบิกจ่าย (ยืนยันกับเจ้าของโปรเจกต์แล้ว) และไม่อัปเดตตาม Loan Officer คนใหม่ภายหลัง เพราะใบสมัครต้นทางเป็นสถานะสุดท้าย Disbursed แล้วจึงมอบหมายใหม่ไม่ได้
resource: ../rules/BR-miniloan-054@v1.md
tags: [miniloan, question, data_scope]
id: DQ-miniloan-011
state: answered
raised_by: BR-miniloan-054@v1
answer_phase: domain
timestamp: 2026-09-01T18:00:00+07:00
spec_hash: sha256:ed23f1bee0bc89ccd5089d775192bf0a001491d60f3e2c1ecfa2d21e0b587553
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
บัญชีสินเชื่อสืบทอด Operations ผู้ดูแลจาก Loan Officer ที่ถูกมอบหมายในใบสมัครต้นทางโดยอัตโนมัติตอนเบิกจ่าย (ยืนยันกับเจ้าของโปรเจกต์แล้ว) และไม่อัปเดตตาม Loan Officer คนใหม่ภายหลัง เพราะใบสมัครต้นทางเป็นสถานะสุดท้าย Disbursed แล้วจึงมอบหมายใหม่ไม่ได้

ตอบเมื่อ 2026-09-01T18:00:00+07:00

## ผลที่ตามมา

- `ENT-007`
