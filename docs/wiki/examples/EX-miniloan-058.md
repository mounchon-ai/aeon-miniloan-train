---
type: Example
title: happy — ใบสมัครเปลี่ยนเป็น "เบิกจ่ายแล้ว (Disbursed)" และระบบสร้างบั
description: ใบสมัครเปลี่ยนเป็น "เบิกจ่ายแล้ว (Disbursed)" และระบบสร้างบัญชีสินเชื่อสถานะ "กำลังผ่อนชำระ (Active)" ขึ้นหนึ่งบัญชี พร้อมตารางผ่อนตาม BR-miniloan-015@v1 · หน้าจอแสดง "เบิกจ่ายเรียบร้อย — เปิดบัญชีสินเชื่อเลขที่ {เลขบัญชี}"
resource: ../rules/BR-miniloan-014@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-058
status: draft
kind: happy
proves: [BR-miniloan-014@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:64f77a3716cc086811947abc20fd2be458a17292300cf77061f8ab69cff9024f
---

# EX-miniloan-058

## กำหนดให้ (given)
ใบสมัครสถานะ "อนุมัติแล้ว (Approved)" วงเงิน 100,000 บาท 12 งวด ที่ยังไม่เคยเบิกจ่าย

## เมื่อ (when)
Loan Officer สั่งเบิกจ่าย

## แล้ว (then)
ใบสมัครเปลี่ยนเป็น "เบิกจ่ายแล้ว (Disbursed)" และระบบสร้างบัญชีสินเชื่อสถานะ "กำลังผ่อนชำระ (Active)" ขึ้นหนึ่งบัญชี พร้อมตารางผ่อนตาม BR-miniloan-015@v1 · หน้าจอแสดง "เบิกจ่ายเรียบร้อย — เปิดบัญชีสินเชื่อเลขที่ {เลขบัญชี}"

## พิสูจน์กฎ

- [BR-miniloan-014@v1](../rules/BR-miniloan-014@v1.md) ✅ ปัจจุบัน — เบิกจ่ายได้เฉพาะใบสมัครสถานะ Approved · เมื่อเบิกจ่ายสำเร็จ ใบสมัครเปลี่ยนเป็น Disbursed และระบบสร้าง LoanAccount สถานะ Active หนึ่งบัญชีต่อหนึ่งใบสมัคร
