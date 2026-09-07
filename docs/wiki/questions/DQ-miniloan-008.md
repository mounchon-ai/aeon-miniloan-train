---
type: Deferred Question
title: ข้อมูลที่เขียนลงไปแล้วก่อนการเรียก API จะล้มเหลว ต้อง rollback ทั้งก้อนหรือค้างค
description: ขอบเขต transaction คือทั้งคำสั่งเบิกจ่าย: เปลี่ยนสถานะใบสมัครเป็น Disbursed, สร้าง LoanAccount, สร้าง RepaymentSchedule และ Installment ทุกงวด อยู่ในธุรกรรมเดียวกันทั้งหมด ล้มเหลวที่จุดใดก็ตาม = rollback ทั้งก้อน ไม่มีบัญชีที่ไม่มีตารางผ่อน และไม่มีสถานะครึ่งทางให้ต้องซ่อมทีหลัง · กู้คืนด้วยการที่ผู้ใช้สั่งซ้ำเอง ไม่มี retry อัตโนมัติตาม BR-miniloan-042@v1 และ REQ-miniloan-006 · การสั่งซ้ำปลอดภัยเพราะ ENT-012 IdempotencyKey บังคับคู่ (commandType, requestId) ด้วย unique constraint ที่ db · บันทึกไว้ที่ interfaces.json ruleEnforcement ของ BR-miniloan-015@v1 enforceAt [domain, db] ผ่าน ADR-004 · SCN-miniloan-035 และ SCN-miniloan-036 ที่เซ็นแล้วพิสูจน์ข้อนี้อยู่ก่อนแล้ว
resource: ../rules/BR-miniloan-042@v1.md
tags: [miniloan, question, integration]
id: DQ-miniloan-008
state: answered
raised_by: BR-miniloan-042@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:5327f8a326bbd077e70bb2924fb4c32d16d872285c155b10525568cde89c4b6a
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
ขอบเขต transaction คือทั้งคำสั่งเบิกจ่าย: เปลี่ยนสถานะใบสมัครเป็น Disbursed, สร้าง LoanAccount, สร้าง RepaymentSchedule และ Installment ทุกงวด อยู่ในธุรกรรมเดียวกันทั้งหมด ล้มเหลวที่จุดใดก็ตาม = rollback ทั้งก้อน ไม่มีบัญชีที่ไม่มีตารางผ่อน และไม่มีสถานะครึ่งทางให้ต้องซ่อมทีหลัง · กู้คืนด้วยการที่ผู้ใช้สั่งซ้ำเอง ไม่มี retry อัตโนมัติตาม BR-miniloan-042@v1 และ REQ-miniloan-006 · การสั่งซ้ำปลอดภัยเพราะ ENT-012 IdempotencyKey บังคับคู่ (commandType, requestId) ด้วย unique constraint ที่ db · บันทึกไว้ที่ interfaces.json ruleEnforcement ของ BR-miniloan-015@v1 enforceAt [domain, db] ผ่าน ADR-004 · SCN-miniloan-035 และ SCN-miniloan-036 ที่เซ็นแล้วพิสูจน์ข้อนี้อยู่ก่อนแล้ว

ตอบเมื่อ 2026-09-05

## ผลที่ตามมา

- `ADR-004`
