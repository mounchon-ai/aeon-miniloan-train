---
type: Business Rule
title: API ต้องมีสัญญาที่ทดสอบได้ — คำอธิบาย endpoint และ request/response schema (เช่น
description: API ต้องมีสัญญาที่ทดสอบได้ — คำอธิบาย endpoint และ request/response schema (เช่น OpenAPI) — และ request/response จริงต้องตรงตาม schema ที่ประกาศไว้
resource: ../requirements/REQ-miniloan-006.md
tags: [miniloan, constraint]
id: BR-miniloan-029@v1
status: draft
belongs_to: REQ-miniloan-006
kind: constraint
is_current: true
test_design: [EP]
proven_by: [EX-miniloan-131, EX-miniloan-132]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:c1aaf14e540de2c8b55c6ef62bc8e121ac76bb18d2f3e34f9d244d4f95beafbc
---

# BR-miniloan-029@v1

## ข้อความของกฎ
API ต้องมีสัญญาที่ทดสอบได้ — คำอธิบาย endpoint และ request/response schema (เช่น OpenAPI) — และ request/response จริงต้องตรงตาม schema ที่ประกาศไว้

## ที่มา

> "**Given** API พร้อมใช้งาน **When** เปิดดูสัญญา **Then** มีคำอธิบาย endpoint, request/response schema (เช่น OpenAPI) ที่ใช้ทดสอบและอ้างอิงได้ · **Given** request/response **When** ตรวจสอบ **Then** ตรงตาม schema ที่ประกาศไว้"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-15

## พิสูจน์โดย

- [EX-miniloan-131](../examples/EX-miniloan-131.md) — happy: **response ผ่านการตรวจกับ schema ทุกฟิลด์** — ชื่อฟิลด์ ชนิดข้อมูล และฟิลด์ที่บังคับ ตรงกับที่ประกาศทั้งหมด · **สัญญาที่ประกาศไว้ทดสอบได้จริงด้วยเครื่อง ไม่ใช่เอกสารที่เขียนไว้เฉยๆ**
- [EX-miniloan-132](../examples/EX-miniloan-132.md) — exception: **การตรวจต้องไม่ผ่าน และต้องชี้ได้ว่าฟิลด์ไหนไม่ตรง** · **ถือเป็นข้อผิดพลาดของสัญญา ไม่ใช่เรื่องเล็กที่ปล่อยผ่าน** — เพราะฝั่งเว็บสร้างขึ้นจาก schema ที่ประกาศ ถ้าของจริงไม่ตรง เว็บจะพังโดยไม่มีใครรู้ล่วงหน้า

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-029@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
