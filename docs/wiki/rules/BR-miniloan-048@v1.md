---
type: Business Rule
title: ผู้มีสิทธิ์ตั้งค่าและเปลี่ยน role ผู้อนุมัติตาม BR-miniloan-039@v1 คือ Loan Offi
description: ผู้มีสิทธิ์ตั้งค่าและเปลี่ยน role ผู้อนุมัติตาม BR-miniloan-039@v1 คือ Loan Officer เท่านั้น — Operations และ Applicant ตั้งค่าไม่ได้ ทั้งจากหน้าจอและผ่าน API · โมดูลนี้ไม่เพิ่ม actor ผู้ดูแลระบบ (System Admin) เข้ามาในรอบนี้
resource: ../requirements/REQ-miniloan-004.md
tags: [miniloan, policy]
id: BR-miniloan-048@v1
status: draft
belongs_to: REQ-miniloan-004
kind: policy
is_current: true
test_design: [decision_table]
proven_by: [EX-miniloan-083, EX-miniloan-084]
golden: []
provenance: [SRC-012]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:832a775f1e4f02b69e75c0659694f86facf04a83eb37966ff3b530a549d5eb9c
---

# BR-miniloan-048@v1

## ข้อความของกฎ
ผู้มีสิทธิ์ตั้งค่าและเปลี่ยน role ผู้อนุมัติตาม BR-miniloan-039@v1 คือ Loan Officer เท่านั้น — Operations และ Applicant ตั้งค่าไม่ได้ ทั้งจากหน้าจอและผ่าน API · โมดูลนี้ไม่เพิ่ม actor ผู้ดูแลระบบ (System Admin) เข้ามาในรอบนี้

## ที่มา

> "ผู้มีสิทธิ์ตั้งค่าและเปลี่ยน role ผู้อนุมัติคือ Loan Officer เท่านั้น ไม่เพิ่ม actor ผู้ดูแลระบบเข้ามาในรอบนี้"
> — [SRC-012](../sources/SRC-012.md) หน้า — §[2] Q-miniloan-009 ท่อนที่ 1

## พิสูจน์โดย

- [EX-miniloan-083](../examples/EX-miniloan-083.md) — happy: ระบบบันทึกค่าและแสดง "ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น {role ที่เลือก} เรียบร้อย" · หลังจากนี้คำขอปรับปรุงบัญชีที่ปิดแล้วยื่นได้ตาม BR-miniloan-038@v1 และประตูที่ BR-miniloan-040@v1 ปิดไว้ก็เปิดออก
- [EX-miniloan-084](../examples/EX-miniloan-084.md) — exception: ทั้งสองทางถูกปฏิเสธ — เมนูตั้งค่า role ผู้อนุมัติไม่ปรากฏสำหรับบทบาทนี้ และ API ปฏิเสธด้วยข้อความ "ไม่มีสิทธิ์ตั้งค่า role ผู้อนุมัติ — ทำได้เฉพาะ Loan Officer" ตาม BR-miniloan-025@v1 · ค่า role ผู้อนุมัติไม่เปลี่ยน — ฝ่ายที่จะเป็นผู้ขอแก้ ตั้งผู้อนุมัติของตัวเองไม่ได้

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-048@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
