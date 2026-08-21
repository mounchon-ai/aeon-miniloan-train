---
type: Business Rule
title: บันทึกร่างใบสมัครได้แม้กรอกไม่ครบ — ขั้นร่างยังไม่ตรวจ business rule เต็ม และได้
description: บันทึกร่างใบสมัครได้แม้กรอกไม่ครบ — ขั้นร่างยังไม่ตรวจ business rule เต็ม และได้ใบสมัครสถานะ Draft
resource: ../requirements/REQ-miniloan-001.md
tags: [miniloan, policy]
id: BR-miniloan-008@v1
status: draft
belongs_to: REQ-miniloan-001
kind: policy
is_current: true
test_design: [EP]
proven_by: [EX-miniloan-032, EX-miniloan-033]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:5d693080829966a8bd5484b9bde7c1649abf2f1f627c8705b2815248e03b932b
---

# BR-miniloan-008@v1

## ข้อความของกฎ
บันทึกร่างใบสมัครได้แม้กรอกไม่ครบ — ขั้นร่างยังไม่ตรวจ business rule เต็ม และได้ใบสมัครสถานะ Draft

## ที่มา

> "**Given** ยังกรอกไม่ครบ **When** บันทึกร่าง **Then** อนุญาตให้บันทึกได้ (ยังไม่ตรวจ business rule เต็ม)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-01

## พิสูจน์โดย

- [EX-miniloan-032](../examples/EX-miniloan-032.md) — happy: ระบบบันทึกสำเร็จและแสดง "บันทึกร่างเรียบร้อย" · ได้ใบสมัครสถานะ "ร่าง (Draft)" ที่กลับมาแก้ต่อได้ · **ไม่มีข้อความเตือนเรื่องช่องที่ยังไม่ได้กรอก**
- [EX-miniloan-033](../examples/EX-miniloan-033.md) — alternate: ระบบบันทึกสำเร็จและแสดง "บันทึกร่างเรียบร้อย" · ใบสมัครเป็น "ร่าง (Draft)" ที่เก็บค่า 5,000 ไว้ตามที่กรอก · **ขั้นร่างยังไม่ตรวจ business rule เต็ม** — ค่านี้จะถูกปฏิเสธก็ต่อเมื่อกดยื่น ตาม BR-miniloan-004@v1

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-008@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
