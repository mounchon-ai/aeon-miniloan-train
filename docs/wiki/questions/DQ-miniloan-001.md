---
type: Deferred Question
title: จำนวนเงินในระบบเก็บเป็น decimal(p,s) เท่าไร และปัดกี่ตำแหน่ง — §8 บอกแค่ว่าต้องแ
description: จำนวนเงินเก็บเป็น decimal(18,2) หน่วยบาท ปัดทันทีทุกจุดที่เกิดด้วย round half up ตาม BR-miniloan-035@v1 — 2 ตำแหน่งทศนิยม (สตางค์) ทุกฟิลด์ประเภท money
resource: ../rules/BR-miniloan-016@v1.md
tags: [miniloan, question, calculation]
id: DQ-miniloan-001
state: answered
raised_by: BR-miniloan-016@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:3b7db832568ee2bc08d87997b4106ca0c4855b39ec76af8dad2feb6fdd91e169
---

# DQ-miniloan-001

## คำถามที่เลื่อนไป
จำนวนเงินในระบบเก็บเป็น decimal(p,s) เท่าไร และปัดกี่ตำแหน่ง — §8 บอกแค่ว่าต้องแม่นยำและห้ามใช้ floating point แต่ไม่ได้ระบุความละเอียด

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-016@v1](../rules/BR-miniloan-016@v1.md) |
| หมวด | calculation |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:Money` |

## คำตอบ
จำนวนเงินเก็บเป็น decimal(18,2) หน่วยบาท ปัดทันทีทุกจุดที่เกิดด้วย round half up ตาม BR-miniloan-035@v1 — 2 ตำแหน่งทศนิยม (สตางค์) ทุกฟิลด์ประเภท money

ตอบเมื่อ 2026-09-03T03:39:19Z

## ผลที่ตามมา

- `ENT-008`
