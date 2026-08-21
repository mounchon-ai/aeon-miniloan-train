---
type: Example
title: exception — ระบบไม่เปลี่ยนสถานะ และแสดง "ปฏิเสธไม่ได้ — ต้องระบุเหตุผลกา
description: ระบบไม่เปลี่ยนสถานะ และแสดง "ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ" · ใบสมัครยังเป็น "อยู่ระหว่างพิจารณา (UnderReview)"
resource: ../rules/BR-miniloan-013@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-056
status: draft
kind: exception
proves: [BR-miniloan-013@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:f9fa754ade214b1fdc0fa0611701732e359551e441d2a44d4be74460110b2506
---

# EX-miniloan-056

## กำหนดให้ (given)
ใบสมัครสถานะ "อยู่ระหว่างพิจารณา (UnderReview)" เช่นเดียวกัน

## เมื่อ (when)
Loan Officer กดปฏิเสธโดยเว้นช่องเหตุผลไว้ว่าง

## แล้ว (then)
ระบบไม่เปลี่ยนสถานะ และแสดง "ปฏิเสธไม่ได้ — ต้องระบุเหตุผลการปฏิเสธ" · ใบสมัครยังเป็น "อยู่ระหว่างพิจารณา (UnderReview)"

## พิสูจน์กฎ

- [BR-miniloan-013@v1](../rules/BR-miniloan-013@v1.md) ✅ ปัจจุบัน — ปฏิเสธใบสมัครต้องระบุเหตุผลเสมอ และสถานะ Rejected เป็นสถานะสุดท้าย — พยายามอนุมัติใบที่ Rejected แล้วต้องถูกปฏิเสธเป็น invalid transition
