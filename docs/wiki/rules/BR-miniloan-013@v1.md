---
type: Business Rule
title: ปฏิเสธใบสมัครต้องระบุเหตุผลเสมอ และสถานะ Rejected เป็นสถานะสุดท้าย — พยายามอนุมั
description: ปฏิเสธใบสมัครต้องระบุเหตุผลเสมอ และสถานะ Rejected เป็นสถานะสุดท้าย — พยายามอนุมัติใบที่ Rejected แล้วต้องถูกปฏิเสธเป็น invalid transition
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, invariant]
id: BR-miniloan-013@v1
status: draft
belongs_to: REQ-miniloan-002
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-055, EX-miniloan-056, EX-miniloan-057]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:00c95831ec62c4d784e2d0e20d80b68b880bb4ce599530bd372739e09d7e4a9c
---

# BR-miniloan-013@v1

## ข้อความของกฎ
ปฏิเสธใบสมัครต้องระบุเหตุผลเสมอ และสถานะ Rejected เป็นสถานะสุดท้าย — พยายามอนุมัติใบที่ Rejected แล้วต้องถูกปฏิเสธเป็น invalid transition

## ที่มา

> "**Given** ใบสมัครสถานะ `UnderReview` หรือ Band C **When** กดปฏิเสธพร้อมระบุเหตุผล **Then** สถานะเปลี่ยนเป็น `Rejected` (final) · **Given** ใบสมัคร `Rejected` แล้ว **When** พยายามอนุมัติ **Then** ระบบปฏิเสธการกระทำ (invalid transition)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-06

## พิสูจน์โดย

- [EX-miniloan-055](../examples/EX-miniloan-055.md) — happy: ใบสมัครเปลี่ยนเป็น "ปฏิเสธแล้ว (Rejected)" และหน้าใบสมัครแสดง "ปฏิเสธโดย {ชื่อเจ้าหน้าที่} เมื่อ {วันที่เวลา} · เหตุผล: ภาระหนี้ต่อรายได้สูงเกินเกณฑ์"
- [EX-miniloan-056](../examples/EX-miniloan-056.md) — exception: ระบบไม่เปลี่ยนสถานะ และแสดง "ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ" · ใบสมัครยังเป็น "อยู่ระหว่างพิจารณา (UnderReview)"
- [EX-miniloan-057](../examples/EX-miniloan-057.md) — boundary: ทั้งสองทางถูกปฏิเสธเป็นการเดินสถานะที่ไม่ถูกต้อง — "อนุมัติไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้ว" · สถานะยังเป็น "ปฏิเสธแล้ว (Rejected)"

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-013@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
