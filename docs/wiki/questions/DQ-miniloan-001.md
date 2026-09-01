---
type: Deferred Question
title: จำนวนเงินในระบบเก็บเป็น decimal(p,s) เท่าไร และปัดกี่ตำแหน่ง — §8 บอกแค่ว่าต้องแ
description: เก็บเป็น decimal(18,2) ปัดเศษด้วย round half up ทันทีที่คำนวณทุกจุดตาม BR-miniloan-035@v1 — 2 ตำแหน่งทศนิยม (สตางค์) เพราะสกุลเงินบาทมีหน่วยย่อยสุดคือสตางค์ และ CHG-miniloan-005 ยืนยันแล้วว่าไม่มีบัญชีภายในที่ละเอียดกว่าคอลัมน์ที่แสดง
resource: ../rules/BR-miniloan-016@v1.md
tags: [miniloan, question, calculation]
id: DQ-miniloan-001
state: answered
raised_by: BR-miniloan-016@v1
answer_phase: domain
timestamp: 2026-09-01T18:00:00+07:00
spec_hash: sha256:1c52d08f7f5f28d12c3b8f2eedec4a7aa1b85200b91d0538b736d8839f422ebe
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
เก็บเป็น decimal(18,2) ปัดเศษด้วย round half up ทันทีที่คำนวณทุกจุดตาม BR-miniloan-035@v1 — 2 ตำแหน่งทศนิยม (สตางค์) เพราะสกุลเงินบาทมีหน่วยย่อยสุดคือสตางค์ และ CHG-miniloan-005 ยืนยันแล้วว่าไม่มีบัญชีภายในที่ละเอียดกว่าคอลัมน์ที่แสดง

ตอบเมื่อ 2026-09-01T18:00:00+07:00

## ผลที่ตามมา

- `ENT-007`
