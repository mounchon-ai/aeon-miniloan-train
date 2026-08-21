---
type: Example
title: boundary — ระบบปฏิเสธ — "ยกเลิกใบสมัครที่เบิกจ่ายแล้วไม่ได้ — ใบนี้มีบั
description: ระบบปฏิเสธ — "ยกเลิกใบสมัครที่เบิกจ่ายแล้วไม่ได้ — ใบนี้มีบัญชีสินเชื่อเปิดอยู่ ให้ดำเนินการทางปิดบัญชีแทน" · สถานะยังเป็น "เบิกจ่ายแล้ว (Disbursed)" และบัญชีสินเชื่อไม่ถูกแตะต้อง
resource: ../rules/BR-miniloan-010@v1.md
tags: [miniloan, example, boundary]
id: EX-miniloan-048
status: draft
kind: boundary
proves: [BR-miniloan-010@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:322b3f5148fc1707f622a08c727b9163af42f08b25550f2f33207708b553e594
---

# EX-miniloan-048

## กำหนดให้ (given)
ใบสมัครสถานะ "เบิกจ่ายแล้ว (Disbursed)" ซึ่งมีบัญชีสินเชื่อเปิดอยู่แล้ว — ขอบที่ตรวจคือจุดที่การยกเลิกหมดสิทธิ์

## เมื่อ (when)
Loan Officer ที่ถูกมอบหมายสั่งยกเลิกใบสมัครนั้น

## แล้ว (then)
ระบบปฏิเสธ — "ยกเลิกใบสมัครที่เบิกจ่ายแล้วไม่ได้ — ใบนี้มีบัญชีสินเชื่อเปิดอยู่ ให้ดำเนินการทางปิดบัญชีแทน" · สถานะยังเป็น "เบิกจ่ายแล้ว (Disbursed)" และบัญชีสินเชื่อไม่ถูกแตะต้อง

## พิสูจน์กฎ

- [BR-miniloan-010@v1](../rules/BR-miniloan-010@v1.md) ✅ ปัจจุบัน — LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderReview → Approved → Disbursed และ UnderReview → Rejected · มีเส้นยกเลิกเพิ่ม: Draft, Submitted, UnderReview และ Approved → Cancelled ได้ · ใบที่ Disbursed แล้วยกเลิกไม่ได้ทุกกรณี ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1 แทน · Rejected และ Cancelled เป็นสถานะสุดท้ายทั้งคู่ ไม่มีเส้นออก · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น (Submit(), Approve(), Reject(), Disburse(), Cancel()) — ถอยสถานะกลับไม่ได้ วิธีแก้เมื่อเดินผิดคือยกเลิกแล้วสร้างใบใหม่
