---
type: Deferred Question
title: แดชบอร์ดและยอดรวมตาม BR-miniloan-024@v1 ใช้ขอบเขตข้อมูลเดียวกับการเปิดดูรายใบหรื
description: แดชบอร์ดใช้ขอบเขตข้อมูลเดียวกับการเปิดดูรายใบ คือ own ไม่ใช่ทั้งระบบ — ACL-018 (UC-miniloan-020 ดูแดชบอร์ดภาพรวมสถานะ) scope: own และ ACL-030 (UI-miniloan-009 เปิดหน้าแดชบอร์ด) scope: own ซึ่งตรงกับ ACL-027/ACL-028 (UI-miniloan-006 คิวใบสมัครที่มอบหมายให้ฉัน · UI-miniloan-007 พิจารณาใบสมัคร) ที่เป็น own เช่นกัน ดังนั้นยอดรวมตาม BR-miniloan-024@v1 ที่ Loan Officer เห็นบนแดชบอร์ด นับเฉพาะใบสมัครและบัญชีที่ตัวเองถูกมอบหมาย ไม่ใช่ของทั้งระบบ
resource: ../rules/BR-miniloan-033@v1.md
tags: [miniloan, question, data_scope]
id: DQ-miniloan-004
state: answered
raised_by: BR-miniloan-033@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:b561d02cd78b025c664d33b52c090a4b5142b95d0a891df4afe80fb691f7e40c
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
แดชบอร์ดใช้ขอบเขตข้อมูลเดียวกับการเปิดดูรายใบ คือ own ไม่ใช่ทั้งระบบ — ACL-018 (UC-miniloan-020 ดูแดชบอร์ดภาพรวมสถานะ) scope: own และ ACL-030 (UI-miniloan-009 เปิดหน้าแดชบอร์ด) scope: own ซึ่งตรงกับ ACL-027/ACL-028 (UI-miniloan-006 คิวใบสมัครที่มอบหมายให้ฉัน · UI-miniloan-007 พิจารณาใบสมัคร) ที่เป็น own เช่นกัน ดังนั้นยอดรวมตาม BR-miniloan-024@v1 ที่ Loan Officer เห็นบนแดชบอร์ด นับเฉพาะใบสมัครและบัญชีที่ตัวเองถูกมอบหมาย ไม่ใช่ของทั้งระบบ

ตอบเมื่อ 2026-09-05

## ผลที่ตามมา

- `ACL-018`
