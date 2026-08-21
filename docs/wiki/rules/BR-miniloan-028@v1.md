---
type: Business Rule
title: เมื่อ API ปิดให้บริการ Web ต้องแสดงสถานะข้อผิดพลาดอย่างเหมาะสม และต้องไม่ crash
description: เมื่อ API ปิดให้บริการ Web ต้องแสดงสถานะข้อผิดพลาดอย่างเหมาะสม และต้องไม่ crash
resource: ../requirements/REQ-miniloan-006.md
tags: [miniloan, constraint]
id: BR-miniloan-028@v1
status: draft
belongs_to: REQ-miniloan-006
kind: constraint
is_current: true
test_design: [EP]
proven_by: [EX-miniloan-129, EX-miniloan-130]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:dda724f1dc9b97b97354de8175079de4693b068a20528ac20651ecfbb587d405
---

# BR-miniloan-028@v1

## ข้อความของกฎ
เมื่อ API ปิดให้บริการ Web ต้องแสดงสถานะข้อผิดพลาดอย่างเหมาะสม และต้องไม่ crash

## ที่มา

> "**Given** API ปิดให้บริการ **When** เปิด Web **Then** Web แสดงสถานะข้อผิดพลาดอย่างเหมาะสม (ไม่ crash)"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-14

## พิสูจน์โดย

- [EX-miniloan-129](../examples/EX-miniloan-129.md) — exception: หน้าจอ **แสดงสถานะข้อผิดพลาดอย่างชัดเจน** — **"ตอนนี้เชื่อมต่อระบบไม่ได้ กรุณาลองใหม่อีกครั้ง"** · **ไม่ใช่หน้าขาว ไม่ใช่หน้าที่ค้างหมุนตลอด และไม่ใช่ข้อความภาษาอังกฤษดิบจากเบราว์เซอร์** · เมนูและส่วนอื่นของหน้ายังกดได้ ไม่พังทั้งแอป
- [EX-miniloan-130](../examples/EX-miniloan-130.md) — boundary: หน้าจอโหลดข้อมูลได้ตามปกติทันที · **ไม่ต้องปิดแล้วเปิดแอปใหม่ ไม่ต้องล็อกอินใหม่** · สถานะข้อผิดพลาดหายไปเอง — พิสูจน์ว่าหน้าจอไม่ได้พังค้างอยู่ แค่แสดงสถานะ

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-028@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
