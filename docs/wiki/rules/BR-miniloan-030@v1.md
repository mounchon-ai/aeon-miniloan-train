---
type: Business Rule
title: Web เรียก API ต้องแนบ token จำลองทุกครั้ง และ API ต้องตรวจสอบ token ก่อนให้บริกา
description: Web เรียก API ต้องแนบ token จำลองทุกครั้ง และ API ต้องตรวจสอบ token ก่อนให้บริการ (ระบบ auth จริงอยู่นอกขอบเขต)
resource: ../requirements/REQ-miniloan-006.md
tags: [miniloan, constraint]
id: BR-miniloan-030@v1
status: draft
belongs_to: REQ-miniloan-006
kind: constraint
is_current: true
test_design: [EP]
proven_by: [EX-miniloan-133, EX-miniloan-134, EX-miniloan-135]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:c89e97e869eda75dfb486a80c285c3d91d6017f732b025765ce59afca7a78bfd
---

# BR-miniloan-030@v1

## ข้อความของกฎ
Web เรียก API ต้องแนบ token จำลองทุกครั้ง และ API ต้องตรวจสอบ token ก่อนให้บริการ (ระบบ auth จริงอยู่นอกขอบเขต)

## ที่มา

> "**Given** Web เรียก API **When** ส่ง request **Then** แนบ token จำลอง และ API ตรวจสอบก่อนให้บริการ (auth จริงอยู่นอกขอบเขต)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-16

## พิสูจน์โดย

- [EX-miniloan-133](../examples/EX-miniloan-133.md) — happy: **คำขอมี token แนบไปด้วยทุกครั้ง** และ API ตรวจ token ผ่านแล้วจึงให้บริการ · ข้อมูลถูกส่งกลับตามปกติ
- [EX-miniloan-134](../examples/EX-miniloan-134.md) — exception: API ปฏิเสธก่อนแตะข้อมูลใดๆ — **"ไม่ได้รับอนุญาต — กรุณาเข้าสู่ระบบใหม่"** · **ไม่มีข้อมูลใบสมัครถูกส่งกลับแม้แต่รายการเดียว**
- [EX-miniloan-135](../examples/EX-miniloan-135.md) — exception: API ปฏิเสธเหมือนกรณีไม่มี token เลย · **การมี token ไม่พอ ต้องเป็น token ที่ผ่านการตรวจ** — ถ้า API รับ token ทุกอันที่แนบมา การตรวจก็ไม่มีความหมาย · ระบบ auth จริงอยู่นอกขอบเขต แต่การตรวจต้องมี

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-030@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
