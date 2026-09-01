---
type: Deferred Question
title: ข้อมูลหลักของอัตราดอกเบี้ยต้องมี soft-delete หรือ archive ไหม — เวอร์ชันที่ห้ามล
description: ไม่ต้องมี soft-delete/archive แยก — ทุกเวอร์ชันเก็บถาวรและอ้างอิงได้เสมอ (kind: reference ไม่มีฟังก์ชันแก้ไขในรอบนี้) ส่วนจะแสดงเวอร์ชันเก่าในรายการปกติหรือซ่อนไว้เป็นพฤติกรรมหน้าจอ ยกให้ /design:sitemap ตัดสิน
resource: ../rules/BR-miniloan-037@v1.md
tags: [miniloan, question, temporal]
id: DQ-miniloan-006
state: answered
raised_by: BR-miniloan-037@v1
answer_phase: domain
timestamp: 2026-09-01T18:00:00+07:00
spec_hash: sha256:49a0778bc75f508bf4989a2e5c5e22a91a39214c0deff61d301b6230388989c2
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
ไม่ต้องมี soft-delete/archive แยก — ทุกเวอร์ชันเก็บถาวรและอ้างอิงได้เสมอ (kind: reference ไม่มีฟังก์ชันแก้ไขในรอบนี้) ส่วนจะแสดงเวอร์ชันเก่าในรายการปกติหรือซ่อนไว้เป็นพฤติกรรมหน้าจอ ยกให้ /design:sitemap ตัดสิน

ตอบเมื่อ 2026-09-01T18:00:00+07:00

## ผลที่ตามมา

- `ENT-011`
