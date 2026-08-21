---
type: Example
title: boundary — ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้อนุมัติไปแล้วเมื่อ {
description: ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้อนุมัติไปแล้วเมื่อ {วันที่เวลา}" · ค่าผู้อนุมัติและเวลาเดิมไม่ถูกเขียนทับ
resource: ../rules/BR-miniloan-011@v1.md
tags: [miniloan, example, boundary]
id: EX-miniloan-051
status: draft
kind: boundary
proves: [BR-miniloan-011@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:a6b567f6079a095304a5d40e49e0b37296f9bcadc0a82ec02228880f256f908b
---

# EX-miniloan-051

## กำหนดให้ (given)
ใบสมัครที่อนุมัติไปแล้วสถานะ "อนุมัติแล้ว (Approved)" — ขอบที่ตรวจคือการอนุมัติซ้ำบนใบที่พ้น UnderReview ไปแล้ว

## เมื่อ (when)
Loan Officer กดอนุมัติใบเดิมอีกครั้ง

## แล้ว (then)
ระบบปฏิเสธ — "อนุมัติไม่ได้ — ใบสมัครนี้อนุมัติไปแล้วเมื่อ {วันที่เวลา}" · ค่าผู้อนุมัติและเวลาเดิมไม่ถูกเขียนทับ

## พิสูจน์กฎ

- [BR-miniloan-011@v1](../rules/BR-miniloan-011@v1.md) ✅ ปัจจุบัน — อนุมัติได้เฉพาะใบสมัครสถานะ UnderReview และเมื่ออนุมัติต้องบันทึกผู้อนุมัติและเวลาไว้ด้วย
