---
type: Example
title: happy — ใบสมัครเดินตามลำดับ "ร่าง (Draft)" → "ยื่นแล้ว (Submitted)" 
description: ใบสมัครเดินตามลำดับ "ร่าง (Draft)" → "ยื่นแล้ว (Submitted)" → "อยู่ระหว่างพิจารณา (UnderReview)" → "อนุมัติแล้ว (Approved)" → "เบิกจ่ายแล้ว (Disbursed)" โดยไม่ข้ามขั้นใดเลย และทุกครั้งที่เปลี่ยนสถานะมีผู้กระทำและเวลาบันทึกไว้
resource: ../rules/BR-miniloan-010@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-045
status: draft
kind: happy
proves: [BR-miniloan-010@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:98baa1332b94b955cc44e15f1f0acf097e4bffc77fceb09333e6e7691e71b93c
---

# EX-miniloan-045

## กำหนดให้ (given)
ใบสมัครใหม่ที่กรอกครบและผู้สมัครผ่านทุกเกณฑ์ · หัวหน้ามอบหมายให้ Loan Officer แล้วตาม BR-miniloan-032@v1

## เมื่อ (when)
เดินครบเส้นทางหลัก: ยื่น → ระบบประเมิน → เจ้าหน้าที่อนุมัติ → เจ้าหน้าที่สั่งเบิกจ่าย

## แล้ว (then)
ใบสมัครเดินตามลำดับ "ร่าง (Draft)" → "ยื่นแล้ว (Submitted)" → "อยู่ระหว่างพิจารณา (UnderReview)" → "อนุมัติแล้ว (Approved)" → "เบิกจ่ายแล้ว (Disbursed)" โดยไม่ข้ามขั้นใดเลย และทุกครั้งที่เปลี่ยนสถานะมีผู้กระทำและเวลาบันทึกไว้

## พิสูจน์กฎ

- [BR-miniloan-010@v1](../rules/BR-miniloan-010@v1.md) ✅ ปัจจุบัน — LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderReview → Approved → Disbursed และ UnderReview → Rejected · มีเส้นยกเลิกเพิ่ม: Draft, Submitted, UnderReview และ Approved → Cancelled ได้ · ใบที่ Disbursed แล้วยกเลิกไม่ได้ทุกกรณี ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1 แทน · Rejected และ Cancelled เป็นสถานะสุดท้ายทั้งคู่ ไม่มีเส้นออก · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น (Submit(), Approve(), Reject(), Disburse(), Cancel()) — ถอยสถานะกลับไม่ได้ วิธีแก้เมื่อเดินผิดคือยกเลิกแล้วสร้างใบใหม่
