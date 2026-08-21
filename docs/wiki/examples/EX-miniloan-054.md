---
type: Example
title: alternate — อนุมัติสำเร็จ ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" ที
description: อนุมัติสำเร็จ ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" ที่วงเงิน 150,000 บาท · ค่า DTI ในผลการประเมิน **ไม่ถูกคำนวณใหม่** ยังเป็นค่าที่คิดจากยอดที่ขอเดิม ตาม BR-miniloan-002@v1
resource: ../rules/BR-miniloan-012@v1.md
tags: [miniloan, example, alternate]
id: EX-miniloan-054
status: draft
kind: alternate
proves: [BR-miniloan-012@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:b38c07555df6823722c5d0cace32e30b72a1cfa1156732ebd6e0b530ec2b4d32
---

# EX-miniloan-054

## กำหนดให้ (given)
ใบสมัครใบเดียวกับ EX-miniloan-053 ที่ถูกเตือนว่าขอเกินวงเงิน

## เมื่อ (when)
Loan Officer ปรับวงเงินลงเหลือ 150,000 บาท แล้วกดอนุมัติอีกครั้ง

## แล้ว (then)
อนุมัติสำเร็จ ใบสมัครเปลี่ยนเป็น "อนุมัติแล้ว (Approved)" ที่วงเงิน 150,000 บาท · ค่า DTI ในผลการประเมิน **ไม่ถูกคำนวณใหม่** ยังเป็นค่าที่คิดจากยอดที่ขอเดิม ตาม BR-miniloan-002@v1

## พิสูจน์กฎ

- [BR-miniloan-012@v1](../rules/BR-miniloan-012@v1.md) ✅ ปัจจุบัน — ถ้าจำนวนเงินกู้ที่ขอ > วงเงินอนุมัติสูงสุดตาม BR-miniloan-003@v1 ระบบต้องเตือนและให้ปรับวงเงินก่อน จึงจะอนุมัติได้
