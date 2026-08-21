---
type: Example
title: exception — ถูกนับเป็น **"ใช้งานอยู่ 1" เท่านั้น** · **ต้องไม่ถูกนับซ้ำใ
description: ถูกนับเป็น **"ใช้งานอยู่ 1" เท่านั้น** · **ต้องไม่ถูกนับซ้ำในช่อง "อนุมัติแล้ว" ด้วย** และสถานะ Disbursed ของใบสมัครไม่มีช่องของตัวเองบนแดชบอร์ด · **นี่คือจุดที่การนับข้ามสอง Aggregate พลาดได้ง่ายที่สุด** · ขอบเขตข้อมูลที่ Loan Officer แต่ละคนเห็นยังไม่ตัดสิน (DQ-miniloan-004)
resource: ../rules/BR-miniloan-024@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-122
status: draft
kind: exception
proves: [BR-miniloan-024@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:def11f3e7e5bfca5b376b96362ebc99952afaf5c7b800f6fd0cfa771c4f58905
---

# EX-miniloan-122

## กำหนดให้ (given)
ใบสมัครหนึ่งใบที่เดินถึงสถานะ "เบิกจ่ายแล้ว (Disbursed)" แล้ว และสร้างบัญชีสินเชื่อสถานะ Active ขึ้นมาหนึ่งบัญชี — ใบเดียวกัน เรื่องเดียวกัน แต่มีตัวตนอยู่ในสอง Aggregate

## เมื่อ (when)
ตรวจตัวเลขบนแดชบอร์ด

## แล้ว (then)
ถูกนับเป็น **"ใช้งานอยู่ 1" เท่านั้น** · **ต้องไม่ถูกนับซ้ำในช่อง "อนุมัติแล้ว" ด้วย** และสถานะ Disbursed ของใบสมัครไม่มีช่องของตัวเองบนแดชบอร์ด · **นี่คือจุดที่การนับข้ามสอง Aggregate พลาดได้ง่ายที่สุด** · ขอบเขตข้อมูลที่ Loan Officer แต่ละคนเห็นยังไม่ตัดสิน (DQ-miniloan-004)

## พิสูจน์กฎ

- [BR-miniloan-024@v1](../rules/BR-miniloan-024@v1.md) ✅ ปัจจุบัน — แดชบอร์ดของ Loan Officer แสดงจำนวนแยกตามสถานะ Submitted / UnderReview / Approved / Active / Closed โดยนับข้ามทั้ง LoanApplication และ LoanAccount
