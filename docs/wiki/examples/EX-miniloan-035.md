---
type: Example
title: exception — ไม่มี CreditAssessment ให้ดู และหน้าจอแสดง "ยังไม่มีผลการประ
description: ไม่มี CreditAssessment ให้ดู และหน้าจอแสดง "ยังไม่มีผลการประเมิน — ใบสมัครนี้ยังไม่ได้ยื่น" · **การประเมินผูกกับการยื่น ไม่ใช่กับการบันทึก**
resource: ../rules/BR-miniloan-009@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-035
status: draft
kind: exception
proves: [BR-miniloan-009@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:783265dd671f27e879906cd464c58d2d794beeb8e42bc29168527f0e64aaefc8
---

# EX-miniloan-035

## กำหนดให้ (given)
ใบสมัครที่ยังเป็นสถานะ Draft — บันทึกร่างไว้แล้วตาม BR-miniloan-008@v1 แต่ยังไม่ได้กดยื่น

## เมื่อ (when)
เปิดดูผลการประเมินของใบสมัครนั้น

## แล้ว (then)
ไม่มี CreditAssessment ให้ดู และหน้าจอแสดง "ยังไม่มีผลการประเมิน — ใบสมัครนี้ยังไม่ได้ยื่น" · **การประเมินผูกกับการยื่น ไม่ใช่กับการบันทึก**

## พิสูจน์กฎ

- [BR-miniloan-009@v1](../rules/BR-miniloan-009@v1.md) ✅ ปัจจุบัน — เมื่อใบสมัครถูกยื่น ระบบต้องประเมินอัตโนมัติและสร้าง CreditAssessment ที่มีคะแนน Band (A/B/C) วงเงินที่อนุมัติได้ และเหตุผลที่ผ่าน/ไม่ผ่านรายเกณฑ์
