---
type: Example
title: exception — **การตรวจต้องไม่ผ่าน และต้องชี้ได้ว่าฟิลด์ไหนไม่ตรง** · **ถื
description: **การตรวจต้องไม่ผ่าน และต้องชี้ได้ว่าฟิลด์ไหนไม่ตรง** · **ถือเป็นข้อผิดพลาดของสัญญา ไม่ใช่เรื่องเล็กที่ปล่อยผ่าน** — เพราะฝั่งเว็บสร้างขึ้นจาก schema ที่ประกาศ ถ้าของจริงไม่ตรง เว็บจะพังโดยไม่มีใครรู้ล่วงหน้า
resource: ../rules/BR-miniloan-029@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-132
status: draft
kind: exception
proves: [BR-miniloan-029@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:462b0b087f8ad0c00129b602e9adbad8e17c7def0e7c4943de087a861ef735a0
---

# EX-miniloan-132

## กำหนดให้ (given)
endpoint ที่ response จริงมีฟิลด์ไม่ตรงกับ schema ที่ประกาศ (เช่น ส่งจำนวนเงินเป็นข้อความแทนตัวเลข)

## เมื่อ (when)
รันการตรวจ response กับ schema

## แล้ว (then)
**การตรวจต้องไม่ผ่าน และต้องชี้ได้ว่าฟิลด์ไหนไม่ตรง** · **ถือเป็นข้อผิดพลาดของสัญญา ไม่ใช่เรื่องเล็กที่ปล่อยผ่าน** — เพราะฝั่งเว็บสร้างขึ้นจาก schema ที่ประกาศ ถ้าของจริงไม่ตรง เว็บจะพังโดยไม่มีใครรู้ล่วงหน้า

## พิสูจน์กฎ

- [BR-miniloan-029@v1](../rules/BR-miniloan-029@v1.md) ✅ ปัจจุบัน — API ต้องมีสัญญาที่ทดสอบได้ — คำอธิบาย endpoint และ request/response schema (เช่น OpenAPI) — และ request/response จริงต้องตรงตาม schema ที่ประกาศไว้
