---
type: Example
title: happy — ใบสมัครเปลี่ยนเป็น "ปฏิเสธแล้ว (Rejected)" และหน้าใบสมัครแสด
description: ใบสมัครเปลี่ยนเป็น "ปฏิเสธแล้ว (Rejected)" และหน้าใบสมัครแสดง "ปฏิเสธโดย {ชื่อเจ้าหน้าที่} เมื่อ {วันที่เวลา} · เหตุผล: ภาระหนี้ต่อรายได้สูงเกินเกณฑ์"
resource: ../rules/BR-miniloan-013@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-055
status: draft
kind: happy
proves: [BR-miniloan-013@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:eba21106f8105196ef0865a146c6f8d6e7bb3bfb6ac58572ae8bb26fafc7fefd
---

# EX-miniloan-055

## กำหนดให้ (given)
ใบสมัครสถานะ "อยู่ระหว่างพิจารณา (UnderReview)" ที่มอบหมายแล้ว

## เมื่อ (when)
Loan Officer กดปฏิเสธพร้อมกรอกเหตุผล "ภาระหนี้ต่อรายได้สูงเกินเกณฑ์"

## แล้ว (then)
ใบสมัครเปลี่ยนเป็น "ปฏิเสธแล้ว (Rejected)" และหน้าใบสมัครแสดง "ปฏิเสธโดย {ชื่อเจ้าหน้าที่} เมื่อ {วันที่เวลา} · เหตุผล: ภาระหนี้ต่อรายได้สูงเกินเกณฑ์"

## พิสูจน์กฎ

- [BR-miniloan-013@v1](../rules/BR-miniloan-013@v1.md) ✅ ปัจจุบัน — ปฏิเสธใบสมัครต้องระบุเหตุผลเสมอ และสถานะ Rejected เป็นสถานะสุดท้าย — พยายามอนุมัติใบที่ Rejected แล้วต้องถูกปฏิเสธเป็น invalid transition
