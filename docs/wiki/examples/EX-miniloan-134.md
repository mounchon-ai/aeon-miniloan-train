---
type: Example
title: exception — API ปฏิเสธก่อนแตะข้อมูลใดๆ — **"ไม่ได้รับอนุญาต — กรุณาเข้าส
description: API ปฏิเสธก่อนแตะข้อมูลใดๆ — **"ไม่ได้รับอนุญาต — กรุณาเข้าสู่ระบบใหม่"** · **ไม่มีข้อมูลใบสมัครถูกส่งกลับแม้แต่รายการเดียว**
resource: ../rules/BR-miniloan-030@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-134
status: draft
kind: exception
proves: [BR-miniloan-030@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:8e6b196e4b4d11efee5cfa70d3dda4fb5b829f7bd45bcec0c0c3b8c899cd6b6a
---

# EX-miniloan-134

## กำหนดให้ (given)
คำขอที่ **ไม่มี token แนบมาเลย**

## เมื่อ (when)
เรียก API ดึงรายการใบสมัครโดยตรง

## แล้ว (then)
API ปฏิเสธก่อนแตะข้อมูลใดๆ — **"ไม่ได้รับอนุญาต — กรุณาเข้าสู่ระบบใหม่"** · **ไม่มีข้อมูลใบสมัครถูกส่งกลับแม้แต่รายการเดียว**

## พิสูจน์กฎ

- [BR-miniloan-030@v1](../rules/BR-miniloan-030@v1.md) ✅ ปัจจุบัน — Web เรียก API ต้องแนบ token จำลองทุกครั้ง และ API ต้องตรวจสอบ token ก่อนให้บริการ (ระบบ auth จริงอยู่นอกขอบเขต)
