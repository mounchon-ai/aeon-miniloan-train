---
type: Business Rule
title: เรียก API ด้วยข้อมูลที่ผิด business rule → API ต้องปฏิเสธพร้อมรหัสและข้อความ err
description: เรียก API ด้วยข้อมูลที่ผิด business rule → API ต้องปฏิเสธพร้อมรหัสและข้อความ error ที่ชัดเจน โดยไม่พึ่งการ validate ของ Web เพียงอย่างเดียว
resource: ../requirements/REQ-miniloan-006.md
tags: [miniloan, constraint]
id: BR-miniloan-026@v1
status: draft
belongs_to: REQ-miniloan-006
kind: constraint
is_current: true
test_design: [EP, decision_table]
proven_by: [EX-miniloan-125, EX-miniloan-126]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:f6f766ffc414b291a09555d8a7dc231c34b833ae9dc32a8280ce7b16c38e62b1
---

# BR-miniloan-026@v1

## ข้อความของกฎ
เรียก API ด้วยข้อมูลที่ผิด business rule → API ต้องปฏิเสธพร้อมรหัสและข้อความ error ที่ชัดเจน โดยไม่พึ่งการ validate ของ Web เพียงอย่างเดียว

## ที่มา

> "**Given** เรียก API ด้วยข้อมูลที่ผิด business rule **When** ประมวลผล **Then** API ปฏิเสธพร้อมรหัส/ข้อความ error ที่ชัดเจน (ไม่พึ่งการ validate ของ Web เพียงอย่างเดียว)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-13

## พิสูจน์โดย

- [EX-miniloan-125](../examples/EX-miniloan-125.md) — exception: API ปฏิเสธพร้อม **รหัสข้อผิดพลาดที่อ่านได้ด้วยเครื่อง** และ **ข้อความไทยที่คนอ่านรู้เรื่อง** — **"จำนวนเงินกู้ที่ขอต้องอยู่ระหว่าง 10,000 – 1,000,000 บาท"** · **ไม่ใช่ข้อความว่างเปล่า ไม่ใช่ 500 Internal Server Error และไม่ใช่รหัสอย่างเดียวโดยไม่มีข้อความ**
- [EX-miniloan-126](../examples/EX-miniloan-126.md) — exception: **API ยังปฏิเสธเหมือนเดิมด้วยรหัสและข้อความเดียวกัน** · หน้าจอแสดงข้อความที่ API ส่งกลับมา ไม่ใช่ข้อความที่หน้าจอแต่งเอง · **การตรวจของหน้าจอเป็นความสะดวก ไม่ใช่ด่านสุดท้าย**

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-026@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
