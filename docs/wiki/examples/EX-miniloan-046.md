---
type: Example
title: exception — ทั้งสองทางถูกปฏิเสธเหมือนกัน — "ถอยสถานะใบสมัครไม่ได้ — ถ้าต
description: ทั้งสองทางถูกปฏิเสธเหมือนกัน — "ถอยสถานะใบสมัครไม่ได้ — ถ้าต้องแก้ ให้ยกเลิกใบนี้แล้วสร้างใบใหม่" และ API ปฏิเสธด้วยตาม BR-miniloan-025@v1 · สถานะยังเป็น "อนุมัติแล้ว (Approved)" ไม่เปลี่ยน
resource: ../rules/BR-miniloan-010@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-046
status: draft
kind: exception
proves: [BR-miniloan-010@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:c5b63fb4b12bab132ffbbcac4e763c930968d6641ffafc0c15c0660dbe5e0ec7
---

# EX-miniloan-046

## กำหนดให้ (given)
ใบสมัครสถานะ "อนุมัติแล้ว (Approved)" ที่เจ้าหน้าที่เพิ่งพบว่าตัวเองอนุมัติผิดใบ

## เมื่อ (when)
พยายามถอยสถานะกลับไปเป็น "อยู่ระหว่างพิจารณา (UnderReview)" ทั้งจากหน้าจอและด้วยการเรียก API ตรง

## แล้ว (then)
ทั้งสองทางถูกปฏิเสธเหมือนกัน — "ถอยสถานะใบสมัครไม่ได้ — ถ้าต้องแก้ ให้ยกเลิกใบนี้แล้วสร้างใบใหม่" และ API ปฏิเสธด้วยตาม BR-miniloan-025@v1 · สถานะยังเป็น "อนุมัติแล้ว (Approved)" ไม่เปลี่ยน

## พิสูจน์กฎ

- [BR-miniloan-010@v1](../rules/BR-miniloan-010@v1.md) ✅ ปัจจุบัน — LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderReview → Approved → Disbursed และ UnderReview → Rejected · มีเส้นยกเลิกเพิ่ม: Draft, Submitted, UnderReview และ Approved → Cancelled ได้ · ใบที่ Disbursed แล้วยกเลิกไม่ได้ทุกกรณี ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1 แทน · Rejected และ Cancelled เป็นสถานะสุดท้ายทั้งคู่ ไม่มีเส้นออก · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น (Submit(), Approve(), Reject(), Disburse(), Cancel()) — ถอยสถานะกลับไม่ได้ วิธีแก้เมื่อเดินผิดคือยกเลิกแล้วสร้างใบใหม่
