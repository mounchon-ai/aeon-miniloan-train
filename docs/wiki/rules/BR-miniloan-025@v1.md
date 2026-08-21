---
type: Business Rule
title: ทุกความสามารถใน Epic 1–8 ต้องมี API endpoint รองรับ และ business rule ต้องถูกบัง
description: ทุกความสามารถใน Epic 1–8 ต้องมี API endpoint รองรับ และ business rule ต้องถูกบังคับที่ฝั่ง API เสมอ
resource: ../requirements/REQ-miniloan-006.md
tags: [miniloan, constraint]
id: BR-miniloan-025@v1
status: draft
belongs_to: REQ-miniloan-006
kind: constraint
is_current: true
test_design: [EP]
proven_by: [EX-miniloan-123, EX-miniloan-124]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:e54310618c51ce9801d81276000a5db65f5dd0e84301f1fde1e026fa3f772b07
---

# BR-miniloan-025@v1

## ข้อความของกฎ
ทุกความสามารถใน Epic 1–8 ต้องมี API endpoint รองรับ และ business rule ต้องถูกบังคับที่ฝั่ง API เสมอ

## ที่มา

> "**Given** ความสามารถใดๆ ใน Epic 1–8 **When** Web ต้องใช้งาน **Then** มี API endpoint รองรับ และ business rule ถูกบังคับที่ฝั่ง API เสมอ"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-13

## พิสูจน์โดย

- [EX-miniloan-123](../examples/EX-miniloan-123.md) — happy: **มี endpoint รองรับจริงและทำงานได้ครบ** — ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" พร้อมบันทึกผู้อนุมัติและเวลา เหมือนกับที่กดผ่านหน้าจอทุกประการ · **ใบนี้พิสูจน์ความสามารถหนึ่งตัวเป็นตัวแทน ไม่ได้พิสูจน์ทุกความสามารถใน Epic 1–8 พร้อมกัน** — ความครบถ้วนต้องมาจากการที่ทุกกฎมีตัวอย่างของตัวเอง
- [EX-miniloan-124](../examples/EX-miniloan-124.md) — exception: API ปฏิเสธ — **"อนุมัติไม่ได้ — ใบสมัครนี้มอบหมายให้ผู้พิจารณาคนอื่น"** · **กฎธุรกิจถูกบังคับที่ฝั่ง API ไม่ใช่แค่ที่หน้าจอ** — ถ้าปฏิเสธเฉพาะบนหน้าจอ ใบนี้จะผ่าน และนั่นคือสิ่งที่กฎข้อนี้ห้าม

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-025@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
