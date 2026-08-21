---
type: Example
title: boundary — แสดง **"ยื่นแล้ว 0 · อยู่ระหว่างพิจารณา 0 · อนุมัติแล้ว 0 · 
description: แสดง **"ยื่นแล้ว 0 · อยู่ระหว่างพิจารณา 0 · อนุมัติแล้ว 0 · ใช้งานอยู่ 0 · ปิดแล้ว 0"** · **เป็นเลขศูนย์ ไม่ใช่ช่องว่าง ไม่ใช่ขีด และไม่ใช่ข้อความว่าไม่มีข้อมูล** · หน้าจอไม่พังและไม่แสดงข้อผิดพลาด
resource: ../rules/BR-miniloan-024@v1.md
tags: [miniloan, example, boundary]
id: EX-miniloan-121
status: draft
kind: boundary
proves: [BR-miniloan-024@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:297753e70db1f9f80a3324b21f21250d7e87b96a71d1c76751c516b4c4fbd36a
---

# EX-miniloan-121

## กำหนดให้ (given)
ระบบที่ยังไม่มีใบสมัครและไม่มีบัญชีสินเชื่อเลยสักรายการ — ขอบล่างสุดของการนับ

## เมื่อ (when)
Loan Officer เปิดแดชบอร์ด

## แล้ว (then)
แสดง **"ยื่นแล้ว 0 · อยู่ระหว่างพิจารณา 0 · อนุมัติแล้ว 0 · ใช้งานอยู่ 0 · ปิดแล้ว 0"** · **เป็นเลขศูนย์ ไม่ใช่ช่องว่าง ไม่ใช่ขีด และไม่ใช่ข้อความว่าไม่มีข้อมูล** · หน้าจอไม่พังและไม่แสดงข้อผิดพลาด

## พิสูจน์กฎ

- [BR-miniloan-024@v1](../rules/BR-miniloan-024@v1.md) ✅ ปัจจุบัน — แดชบอร์ดของ Loan Officer แสดงจำนวนแยกตามสถานะ Submitted / UnderReview / Approved / Active / Closed โดยนับข้ามทั้ง LoanApplication และ LoanAccount
