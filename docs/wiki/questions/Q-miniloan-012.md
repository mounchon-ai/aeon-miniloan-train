---
type: Open Question
title: คำตอบ Q-miniloan-011 ทำให้เกิดเส้นที่เปิดไว้แต่ยังไม่มีใครเดินได้: BR-miniloan-0
description: (1) ใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อเท่านั้น Loan Officer ทั่วไปทำไม่ได้ · และหัวหน้าเป็น actor แยก ไม่ใช่ Loan Officer คนหนึ่ง · (2) เส้น Draft → Cancelled และ Submitted → Cancelled ของ BR-miniloan-010@v1 จึงไม่ต้องตัดออก เพราะมีคนเดินได้แล้ว · เขียนเป็น BR-miniloan-031@v2 ผ่าน /req:change (CHG-miniloan-001) ไม่ใช่แก้ทับ เพราะ @v1 มีตัวอย่างพิสูจน์อยู่
resource: ../rules/BR-miniloan-031@v1.md
tags: [miniloan, question]
id: Q-miniloan-012
state: answered
raised_by: BR-miniloan-031@v1
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:41743b60371e50c0cd5c8e0a8861575176434a055b6907e408d6915bd1544b6c
---

# Q-miniloan-012

## การ์ดแดง
คำตอบ Q-miniloan-011 ทำให้เกิดเส้นที่เปิดไว้แต่ยังไม่มีใครเดินได้: BR-miniloan-031@v1 ให้ยกเลิกได้เฉพาะ Loan Officer และถ้าใบถูกมอบหมายแล้วต้องเป็นคนที่ถูกมอบหมาย · แต่ BR-miniloan-032@v1 มอบหมายตอนใบ**เข้าสู่การพิจารณา** แปลว่าใบสถานะ Draft และ Submitted ยังไม่มีใครถูกมอบหมายเลย — **เส้น Draft → Cancelled และ Submitted → Cancelled ที่ BR-miniloan-010@v1 เปิดไว้ จึงยังไม่มีใครเดินได้จริง** · (1) ใบที่ยังไม่ถูกมอบหมาย ใครยกเลิกได้ — หัวหน้า (UL-miniloan-019), Loan Officer คนไหนก็ได้, หรือยกเลิกไม่ได้จริงๆ จนกว่าจะถูกมอบหมาย · (2) ถ้าคำตอบคือยกเลิกไม่ได้จริง **ต้องตัดเส้น Draft → Cancelled และ Submitted → Cancelled ออกจาก BR-miniloan-010@v1** เพราะกฎที่ประกาศเส้นที่ไม่มีใครเดินได้คือกฎที่ทดสอบไม่ได้

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-031@v1](../rules/BR-miniloan-031@v1.md) |

## คำตอบ
(1) ใบที่ยังไม่ถูกมอบหมาย (Draft, Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อเท่านั้น Loan Officer ทั่วไปทำไม่ได้ · และหัวหน้าเป็น actor แยก ไม่ใช่ Loan Officer คนหนึ่ง · (2) เส้น Draft → Cancelled และ Submitted → Cancelled ของ BR-miniloan-010@v1 จึงไม่ต้องตัดออก เพราะมีคนเดินได้แล้ว · เขียนเป็น BR-miniloan-031@v2 ผ่าน /req:change (CHG-miniloan-001) ไม่ใช่แก้ทับ เพราะ @v1 มีตัวอย่างพิสูจน์อยู่

ตอบเมื่อ 2026-08-15T11:06+07:00

