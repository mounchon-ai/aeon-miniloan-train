---
type: Deferred Question
title: แก้ไขเวอร์ชันอัตราดอกเบี้ยที่ประกาศไปแล้วได้ไหม หรือต้องสร้างเวอร์ชันใหม่เสมอ — 
description: แก้ไขเวอร์ชันอัตราดอกเบี้ยที่ประกาศไปแล้วไม่ได้ (immutable) ต้องสร้างเวอร์ชันใหม่เสมอเมื่อจะเปลี่ยนค่า
resource: ../rules/BR-miniloan-037@v1.md
tags: [miniloan, question, temporal]
id: DQ-miniloan-005
state: answered
raised_by: BR-miniloan-037@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:26a2a7991238deb1a614d29d344a1173d75ab68f91a3f0196956c96744e6b6c9
---

# DQ-miniloan-005

## คำถามที่เลื่อนไป
แก้ไขเวอร์ชันอัตราดอกเบี้ยที่ประกาศไปแล้วได้ไหม หรือต้องสร้างเวอร์ชันใหม่เสมอ — BR-miniloan-037@v1 บอกแค่ว่าห้ามลบ ไม่ได้บอกว่าห้ามแก้

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-037@v1](../rules/BR-miniloan-037@v1.md) |
| หมวด | temporal |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:InterestRate (UL-miniloan-014 ยังไม่ถูกผูกกับ entity)` |

## คำตอบ
แก้ไขเวอร์ชันอัตราดอกเบี้ยที่ประกาศไปแล้วไม่ได้ (immutable) ต้องสร้างเวอร์ชันใหม่เสมอเมื่อจะเปลี่ยนค่า

ตอบเมื่อ 2026-09-03T03:39:19Z

## ผลที่ตามมา

- `ENT-005`
