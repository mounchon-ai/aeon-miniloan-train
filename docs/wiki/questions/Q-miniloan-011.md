---
type: Open Question
title: BR-miniloan-010@v1 เพิ่มสถานะ Cancelled และ method Cancel() เข้ามาแล้ว แต่ยังไม่
description: (1) ยกเลิกได้เฉพาะเจ้าหน้าที่ (Loan Officer) — Applicant ยกเลิกเองไม่ได้ทุกสถานะ · เขียนลงใน BR-miniloan-031@v1 แล้ว · (2) ยกเลิกต้องระบุเหตุผลเสมอ → BR-miniloan-047@v1 · ส่วนที่คำตอบ (1) เปิดช่องไว้ — ใบที่ยังไม่ถูกมอบหมายใครยกเลิกได้ — แยกไปเป็น Q-miniloan-012
resource: ../rules/BR-miniloan-010@v1.md
tags: [miniloan, question]
id: Q-miniloan-011
state: answered
raised_by: BR-miniloan-010@v1
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:1ef9463d1ca129621cdd800e0cc8c7253c009ce05ed95ad545f2d4658fce393a
---

# Q-miniloan-011

## การ์ดแดง
BR-miniloan-010@v1 เพิ่มสถานะ Cancelled และ method Cancel() เข้ามาแล้ว แต่ยังไม่มีใครตอบสองข้อที่ทำให้เขียนโค้ดไม่ได้: (1) **ใครมีสิทธิ์กดยกเลิก** — Applicant ยกเลิกใบของตัวเองได้ไหม · Loan Officer ที่ถูกมอบหมายยกเลิกได้ไหม · หัวหน้า · หรือแล้วแต่สถานะ (เช่น ตอน Draft เป็นของ Applicant แต่พอถึง Approved ต้องเป็นเจ้าหน้าที่) · BR-miniloan-031@v1 ระบุสิทธิ์ไว้ครบทุกเส้นยกเว้นเส้นยกเลิก เพราะตอนเขียนยังไม่มีเส้นนี้ · (2) **ยกเลิกต้องระบุเหตุผลไหม** — BR-miniloan-013@v1 บังคับให้ Rejected ต้องมีเหตุผลเสมอ ถ้า Cancelled ไม่ต้องมี จะตอบไม่ได้ว่าทำไมใบนั้นถูกยกเลิก ซึ่งขัดกับ NFR-miniloan-002 (ตรวจสอบย้อนหลัง)

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-010@v1](../rules/BR-miniloan-010@v1.md) |

## คำตอบ
(1) ยกเลิกได้เฉพาะเจ้าหน้าที่ (Loan Officer) — Applicant ยกเลิกเองไม่ได้ทุกสถานะ · เขียนลงใน BR-miniloan-031@v1 แล้ว · (2) ยกเลิกต้องระบุเหตุผลเสมอ → BR-miniloan-047@v1 · ส่วนที่คำตอบ (1) เปิดช่องไว้ — ใบที่ยังไม่ถูกมอบหมายใครยกเลิกได้ — แยกไปเป็น Q-miniloan-012

ตอบเมื่อ 2026-08-14T22:12+07:00

