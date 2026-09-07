---
type: Deferred Question
title: ข้อมูลหลักของอัตราดอกเบี้ยต้องมี soft-delete หรือ archive ไหม — เวอร์ชันที่ห้ามล
description: ใช้ soft-archive ผ่านฟิลด์ isArchived — เวอร์ชันเก่าซ่อนจากรายการเลือกใช้งานปกติเมื่อมีเวอร์ชันใหม่ แต่ยังอ้างอิงได้เสมอ ห้ามลบจริง
resource: ../rules/BR-miniloan-037@v1.md
tags: [miniloan, question, temporal]
id: DQ-miniloan-006
state: answered
raised_by: BR-miniloan-037@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:77a840c88b0e15dff1fbead97682bf85987680c61fad99488117f5eaa52044ce
---

# DQ-miniloan-006

## คำถามที่เลื่อนไป
ข้อมูลหลักของอัตราดอกเบี้ยต้องมี soft-delete หรือ archive ไหม — เวอร์ชันที่ห้ามลบต้องยังแสดงในรายการปกติ หรือซ่อนไว้แต่ยังอ้างอิงได้

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-037@v1](../rules/BR-miniloan-037@v1.md) |
| หมวด | temporal |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:InterestRate` |

## คำตอบ
ใช้ soft-archive ผ่านฟิลด์ isArchived — เวอร์ชันเก่าซ่อนจากรายการเลือกใช้งานปกติเมื่อมีเวอร์ชันใหม่ แต่ยังอ้างอิงได้เสมอ ห้ามลบจริง

ตอบเมื่อ 2026-09-03T03:39:19Z

## ผลที่ตามมา

- `ENT-005`
