---
type: Example
title: exception — ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้ยังไม่เข้าสู่การพิจา
description: ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้ยังไม่เข้าสู่การพิจารณา" · สถานะยังเป็น "ยื่นแล้ว (Submitted)" และไม่มีการบันทึกผู้อนุมัติใดๆ
resource: ../rules/BR-miniloan-011@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-050
status: draft
kind: exception
proves: [BR-miniloan-011@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:6a7bc70185e36594fdae23cba7f01f908f5266ca9eb48b155af0abf859700866
---

# EX-miniloan-050

## กำหนดให้ (given)
ใบสมัครสถานะ "ยื่นแล้ว (Submitted)" ที่ระบบยังประเมินไม่เสร็จ จึงยังไม่เข้า UnderReview

## เมื่อ (when)
Loan Officer พยายามกดอนุมัติใบสมัครนั้น

## แล้ว (then)
ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้ยังไม่เข้าสู่การพิจารณา" · สถานะยังเป็น "ยื่นแล้ว (Submitted)" และไม่มีการบันทึกผู้อนุมัติใดๆ

## พิสูจน์กฎ

- [BR-miniloan-011@v1](../rules/BR-miniloan-011@v1.md) ✅ ปัจจุบัน — อนุมัติได้เฉพาะใบสมัครสถานะ UnderReview และเมื่ออนุมัติต้องบันทึกผู้อนุมัติและเวลาไว้ด้วย
