---
type: Example
title: alternate — ระบบบันทึกสำเร็จและแสดง "บันทึกร่างเรียบร้อย" · ใบสมัครเป็น 
description: ระบบบันทึกสำเร็จและแสดง "บันทึกร่างเรียบร้อย" · ใบสมัครเป็น "ร่าง (Draft)" ที่เก็บค่า 5,000 ไว้ตามที่กรอก · **ขั้นร่างยังไม่ตรวจ business rule เต็ม** — ค่านี้จะถูกปฏิเสธก็ต่อเมื่อกดยื่น ตาม BR-miniloan-004@v1
resource: ../rules/BR-miniloan-008@v1.md
tags: [miniloan, example, alternate]
id: EX-miniloan-033
status: draft
kind: alternate
proves: [BR-miniloan-008@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:e36e0c0510d9d8c3332e53dc22ed46f6c9dfa1050114ba72b54e892c2437310a
---

# EX-miniloan-033

## กำหนดให้ (given)
ผู้สมัครกรอกจำนวนเงินกู้ 5,000 บาท ซึ่งต่ำกว่าช่วงที่ BR-miniloan-004@v1 รับ และกรอกช่องอื่นไม่ครบ

## เมื่อ (when)
กด "บันทึกร่าง" (ไม่ใช่กดยื่น)

## แล้ว (then)
ระบบบันทึกสำเร็จและแสดง "บันทึกร่างเรียบร้อย" · ใบสมัครเป็น "ร่าง (Draft)" ที่เก็บค่า 5,000 ไว้ตามที่กรอก · **ขั้นร่างยังไม่ตรวจ business rule เต็ม** — ค่านี้จะถูกปฏิเสธก็ต่อเมื่อกดยื่น ตาม BR-miniloan-004@v1

## พิสูจน์กฎ

- [BR-miniloan-008@v1](../rules/BR-miniloan-008@v1.md) ✅ ปัจจุบัน — บันทึกร่างใบสมัครได้แม้กรอกไม่ครบ — ขั้นร่างยังไม่ตรวจ business rule เต็ม และได้ใบสมัครสถานะ Draft
