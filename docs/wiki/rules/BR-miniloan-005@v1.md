---
type: Business Rule
title: อัตราดอกเบี้ยเป็นแบบลดต้นลดดอก 25% ต่อปี
description: อัตราดอกเบี้ยเป็นแบบลดต้นลดดอก 25% ต่อปี
resource: ../requirements/REQ-miniloan-001.md
tags: [miniloan, policy]
id: BR-miniloan-005@v1
status: draft
belongs_to: REQ-miniloan-001
kind: policy
is_current: true
test_design: [EP]
proven_by: [EX-miniloan-027, EX-miniloan-028]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:a8668d46a820e08a1d334a0324a3a57b5437fa8b9aeba5a544c6b72dec5c9ae2
---

# BR-miniloan-005@v1

## ข้อความของกฎ
อัตราดอกเบี้ยเป็นแบบลดต้นลดดอก 25% ต่อปี

## ที่มา

> "อัตราดอกเบี้ย: แบบลดต้นลดดอก 25% ต่อปี (สมมติ ภายใต้เพดานที่กฎหมายกำหนด)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §5 · BR-04

## พิสูจน์โดย

- [EX-miniloan-027](../examples/EX-miniloan-027.md) — happy: ดอกเบี้ยของงวดที่ 1 เท่ากับ เงินต้นตั้งต้น × 25% ÷ 12 และดอกเบี้ยของงวดถัดๆ ไป **ลดลงทุกงวด** เพราะคิดจากยอดคงเหลือที่ลดลง · หน้าจอกำกับว่า "อัตราดอกเบี้ย 25% ต่อปี (ลดต้นลดดอก)"
- [EX-miniloan-028](../examples/EX-miniloan-028.md) — exception: ดอกเบี้ยเท่ากันทุกงวด — เป็นผลของการคิดแบบคงที่ (flat) ซึ่ง **ผิด BR-miniloan-005@v1** · ตารางที่ถูกต้องต้องมีดอกเบี้ยงวดสุดท้ายน้อยกว่างวดแรกเสมอ · เคสนี้ต้องทำให้เทสต์ตก ไม่ใช่ผ่าน

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-005@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
