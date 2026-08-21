---
type: Example
title: happy — ระบบบันทึกค่าและแสดง "ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญช
description: ระบบบันทึกค่าและแสดง "ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น {role ที่เลือก} เรียบร้อย" · หลังจากนี้คำขอปรับปรุงบัญชีที่ปิดแล้วยื่นได้ตาม BR-miniloan-038@v1 และประตูที่ BR-miniloan-040@v1 ปิดไว้ก็เปิดออก
resource: ../rules/BR-miniloan-039@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-083
status: draft
kind: happy
proves: [BR-miniloan-048@v1, BR-miniloan-039@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:a7a1bd8221aab492437b58c4a40c6b611c118a0d0332c10f61e6280f140ce98f
---

# EX-miniloan-083

## กำหนดให้ (given)
ระบบยังไม่ได้ตั้ง role ผู้อนุมัติ · ผู้ใช้ที่ล็อกอินมีบทบาท Loan Officer

## เมื่อ (when)
Loan Officer เปิดหน้าตั้งค่าระบบแล้วกำหนด role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้ว

## แล้ว (then)
ระบบบันทึกค่าและแสดง "ตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้วเป็น {role ที่เลือก} เรียบร้อย" · หลังจากนี้คำขอปรับปรุงบัญชีที่ปิดแล้วยื่นได้ตาม BR-miniloan-038@v1 และประตูที่ BR-miniloan-040@v1 ปิดไว้ก็เปิดออก

## พิสูจน์กฎ

- [BR-miniloan-039@v1](../rules/BR-miniloan-039@v1.md) ✅ ปัจจุบัน — ผู้อนุมัติการปรับปรุงบัญชีสินเชื่อที่ปิดแล้ว (Closed) กำหนดเป็น role ได้ — เป็นค่าที่ตั้งไว้ในระบบและเปลี่ยนได้ภายหลังโดยไม่ต้องแก้โปรแกรม ไม่ผูกตายกับ actor ใด actor หนึ่งในโค้ด
- [BR-miniloan-048@v1](../rules/BR-miniloan-048@v1.md) ✅ ปัจจุบัน — ผู้มีสิทธิ์ตั้งค่าและเปลี่ยน role ผู้อนุมัติตาม BR-miniloan-039@v1 คือ Loan Officer เท่านั้น — Operations และ Applicant ตั้งค่าไม่ได้ ทั้งจากหน้าจอและผ่าน API · โมดูลนี้ไม่เพิ่ม actor ผู้ดูแลระบบ (System Admin) เข้ามาในรอบนี้
