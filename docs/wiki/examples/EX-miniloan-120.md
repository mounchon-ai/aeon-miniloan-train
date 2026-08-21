---
type: Example
title: happy — แดชบอร์ดแสดงครบห้าตัวเลข **"ยื่นแล้ว 3 · อยู่ระหว่างพิจารณา 
description: แดชบอร์ดแสดงครบห้าตัวเลข **"ยื่นแล้ว 3 · อยู่ระหว่างพิจารณา 2 · อนุมัติแล้ว 1 · ใช้งานอยู่ 5 · ปิดแล้ว 4"** · **สามตัวแรกนับจากใบสมัคร สองตัวหลังนับจากบัญชีสินเชื่อ — ตัวเลขชุดเดียวกันมาจากสอง Aggregate**
resource: ../rules/BR-miniloan-024@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-120
status: draft
kind: happy
proves: [BR-miniloan-024@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:9dda529cbc9c062adf68fe3930002cdf90247d4f5063fa1e75cfdc378f2e9e96
---

# EX-miniloan-120

## กำหนดให้ (given)
ระบบมีใบสมัครสถานะ Submitted 3 ใบ · UnderReview 2 ใบ · Approved 1 ใบ และมีบัญชีสินเชื่อสถานะ Active 5 บัญชี · Closed 4 บัญชี

## เมื่อ (when)
Loan Officer เปิดแดชบอร์ด

## แล้ว (then)
แดชบอร์ดแสดงครบห้าตัวเลข **"ยื่นแล้ว 3 · อยู่ระหว่างพิจารณา 2 · อนุมัติแล้ว 1 · ใช้งานอยู่ 5 · ปิดแล้ว 4"** · **สามตัวแรกนับจากใบสมัคร สองตัวหลังนับจากบัญชีสินเชื่อ — ตัวเลขชุดเดียวกันมาจากสอง Aggregate**

## พิสูจน์กฎ

- [BR-miniloan-024@v1](../rules/BR-miniloan-024@v1.md) ✅ ปัจจุบัน — แดชบอร์ดของ Loan Officer แสดงจำนวนแยกตามสถานะ Submitted / UnderReview / Approved / Active / Closed โดยนับข้ามทั้ง LoanApplication และ LoanAccount
