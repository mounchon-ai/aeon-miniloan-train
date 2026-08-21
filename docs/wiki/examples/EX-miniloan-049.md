---
type: Example
title: happy — ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" และหน้าใบสมัครแส
description: ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" และหน้าใบสมัครแสดง "อนุมัติโดย ก. เมื่อ {วันที่เวลา}" · ทั้งชื่อผู้อนุมัติและเวลาถูกบันทึกไว้กับใบสมัคร ไม่ใช่แค่ใน log แยก
resource: ../rules/BR-miniloan-011@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-049
status: draft
kind: happy
proves: [BR-miniloan-011@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:d4c7e0c904b51462da64da1f591955fd68999e385bc5044ec4584ace3db2b9f7
---

# EX-miniloan-049

## กำหนดให้ (given)
ใบสมัครสถานะ "อยู่ระหว่างพิจารณา (UnderReview)" ที่มอบหมายให้ Loan Officer ชื่อ ก. แล้ว

## เมื่อ (when)
ก. กดอนุมัติใบสมัครนั้น

## แล้ว (then)
ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" และหน้าใบสมัครแสดง "อนุมัติโดย ก. เมื่อ {วันที่เวลา}" · ทั้งชื่อผู้อนุมัติและเวลาถูกบันทึกไว้กับใบสมัคร ไม่ใช่แค่ใน log แยก

## พิสูจน์กฎ

- [BR-miniloan-011@v1](../rules/BR-miniloan-011@v1.md) ✅ ปัจจุบัน — อนุมัติได้เฉพาะใบสมัครสถานะ UnderReview และเมื่ออนุมัติต้องบันทึกผู้อนุมัติและเวลาไว้ด้วย
