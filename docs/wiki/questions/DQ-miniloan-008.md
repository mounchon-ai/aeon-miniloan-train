---
type: Deferred Question
title: ข้อมูลที่เขียนลงไปแล้วก่อนการเรียก API จะล้มเหลว ต้อง rollback ทั้งก้อนหรือค้างค
description: ขอบเขต transaction ครอบคลุมการเปลี่ยนสถานะใบสมัครเป็น Disbursed พร้อมสร้าง LoanAccount และ RepaymentSchedule ทั้งก้อนในธุรกรรมเดียว (all-or-nothing) เพราะเป็น API call เดียวในระบบ monolithic ตาม REQ-miniloan-006 ไม่มีการแยกเป็น saga ข้ามบริการ ถ้าขั้นใดล้มเหลวต้อง rollback ทั้งหมด
resource: ../rules/BR-miniloan-042@v1.md
tags: [miniloan, question, integration]
id: DQ-miniloan-008
state: answered
raised_by: BR-miniloan-042@v1
answer_phase: domain
timestamp: 2026-09-01T20:00:00+07:00
spec_hash: sha256:282290382494db3b637407919046a9a0a1a01dd4e8771731f469b5c099c5328d
---

# DQ-miniloan-008

## คำถามที่เลื่อนไป
ข้อมูลที่เขียนลงไปแล้วก่อนการเรียก API จะล้มเหลว ต้อง rollback ทั้งก้อนหรือค้างครึ่งทางได้ — ขอบเขตของ transaction อยู่ตรงไหน (เช่น เบิกจ่ายสำเร็จแต่สร้างตารางผ่อนไม่สำเร็จ ตาม BR-miniloan-015@v1)

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-042@v1](../rules/BR-miniloan-042@v1.md) |
| หมวด | integration |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:LoanApplication / LoanAccount (ยังไม่มีขอบเขต aggregate)` |

## คำตอบ
ขอบเขต transaction ครอบคลุมการเปลี่ยนสถานะใบสมัครเป็น Disbursed พร้อมสร้าง LoanAccount และ RepaymentSchedule ทั้งก้อนในธุรกรรมเดียว (all-or-nothing) เพราะเป็น API call เดียวในระบบ monolithic ตาม REQ-miniloan-006 ไม่มีการแยกเป็น saga ข้ามบริการ ถ้าขั้นใดล้มเหลวต้อง rollback ทั้งหมด

ตอบเมื่อ 2026-09-01T18:00:00+07:00

## ผลที่ตามมา

- `ENT-001`
- `ENT-007`
- `ENT-008`
