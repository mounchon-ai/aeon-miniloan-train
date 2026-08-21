---
type: Example
title: boundary — ทั้งสองทางถูกปฏิเสธเป็นการเดินสถานะที่ไม่ถูกต้อง — "อนุมัติไ
description: ทั้งสองทางถูกปฏิเสธเป็นการเดินสถานะที่ไม่ถูกต้อง — "อนุมัติไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้ว" · สถานะยังเป็น "ปฏิเสธแล้ว (Rejected)"
resource: ../rules/BR-miniloan-013@v1.md
tags: [miniloan, example, boundary]
id: EX-miniloan-057
status: draft
kind: boundary
proves: [BR-miniloan-013@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:fd896e6b6848afde5afe954dd90283603b4b8c5cf309410968a5c7815dd289e9
---

# EX-miniloan-057

## กำหนดให้ (given)
ใบสมัครที่ถูกปฏิเสธไปแล้วสถานะ "ปฏิเสธแล้ว (Rejected)" — ขอบที่ตรวจคือสถานะสุดท้ายที่ไม่มีเส้นออก

## เมื่อ (when)
Loan Officer พยายามกดอนุมัติใบนั้น ทั้งจากหน้าจอและด้วยการเรียก API ตรง

## แล้ว (then)
ทั้งสองทางถูกปฏิเสธเป็นการเดินสถานะที่ไม่ถูกต้อง — "อนุมัติไม่ได้ — ใบสมัครนี้ถูกปฏิเสธไปแล้ว" · สถานะยังเป็น "ปฏิเสธแล้ว (Rejected)"

## พิสูจน์กฎ

- [BR-miniloan-013@v1](../rules/BR-miniloan-013@v1.md) ✅ ปัจจุบัน — ปฏิเสธใบสมัครต้องระบุเหตุผลเสมอ และสถานะ Rejected เป็นสถานะสุดท้าย — พยายามอนุมัติใบที่ Rejected แล้วต้องถูกปฏิเสธเป็น invalid transition
