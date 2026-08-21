---
type: Example
title: exception — ระบบปฏิเสธ — "เบิกจ่ายไม่ได้ — ใบสมัครนี้ยังไม่ได้รับอนุมัติ
description: ระบบปฏิเสธ — "เบิกจ่ายไม่ได้ — ใบสมัครนี้ยังไม่ได้รับอนุมัติ" · ไม่มีบัญชีสินเชื่อถูกสร้าง และสถานะใบสมัครไม่เปลี่ยน
resource: ../rules/BR-miniloan-014@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-059
status: draft
kind: exception
proves: [BR-miniloan-014@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:791fcc06dae9d4a50c654d68d46b67296cd1457b005344584d124d3019d00c78
---

# EX-miniloan-059

## กำหนดให้ (given)
ใบสมัครสถานะ "อยู่ระหว่างพิจารณา (UnderReview)" ที่ยังไม่มีใครอนุมัติ

## เมื่อ (when)
Loan Officer สั่งเบิกจ่ายใบนั้น

## แล้ว (then)
ระบบปฏิเสธ — "เบิกจ่ายไม่ได้ — ใบสมัครนี้ยังไม่ได้รับอนุมัติ" · ไม่มีบัญชีสินเชื่อถูกสร้าง และสถานะใบสมัครไม่เปลี่ยน

## พิสูจน์กฎ

- [BR-miniloan-014@v1](../rules/BR-miniloan-014@v1.md) ✅ ปัจจุบัน — เบิกจ่ายได้เฉพาะใบสมัครสถานะ Approved · เมื่อเบิกจ่ายสำเร็จ ใบสมัครเปลี่ยนเป็น Disbursed และระบบสร้าง LoanAccount สถานะ Active หนึ่งบัญชีต่อหนึ่งใบสมัคร
