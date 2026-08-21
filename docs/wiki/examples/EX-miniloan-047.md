---
type: Example
title: alternate — ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุด
description: ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุดท้าย · หน้าจอแสดง "ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง" · ปุ่มอนุมัติ ปฏิเสธ และเบิกจ่ายหายไปทั้งหมด
resource: ../rules/BR-miniloan-010@v1.md
tags: [miniloan, example, alternate]
id: EX-miniloan-047
status: draft
kind: alternate
proves: [BR-miniloan-010@v1, BR-miniloan-031@v2]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:b6229e3f4556c4aedf15d6530122006b0677f670333775472aa2d8a587df63e4
---

# EX-miniloan-047

## กำหนดให้ (given)
ใบสมัครสถานะ "อนุมัติแล้ว (Approved)" ที่ถูกมอบหมายให้ Loan Officer คนหนึ่งอยู่แล้ว และยังไม่ได้เบิกจ่าย

## เมื่อ (when)
Loan Officer ที่ถูกมอบหมายสั่งยกเลิกใบสมัครนั้นพร้อมระบุเหตุผล

## แล้ว (then)
ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุดท้าย · หน้าจอแสดง "ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง" · ปุ่มอนุมัติ ปฏิเสธ และเบิกจ่ายหายไปทั้งหมด

## พิสูจน์กฎ

- [BR-miniloan-010@v1](../rules/BR-miniloan-010@v1.md) ✅ ปัจจุบัน — LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderReview → Approved → Disbursed และ UnderReview → Rejected · มีเส้นยกเลิกเพิ่ม: Draft, Submitted, UnderReview และ Approved → Cancelled ได้ · ใบที่ Disbursed แล้วยกเลิกไม่ได้ทุกกรณี ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1 แทน · Rejected และ Cancelled เป็นสถานะสุดท้ายทั้งคู่ ไม่มีเส้นออก · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น (Submit(), Approve(), Reject(), Disburse(), Cancel()) — ถอยสถานะกลับไม่ได้ วิธีแก้เมื่อเดินผิดคือยกเลิกแล้วสร้างใบใหม่
- [BR-miniloan-031@v2](../rules/BR-miniloan-031@v2.md) ✅ ปัจจุบัน — สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) แยกตามว่าใบนั้นถูกมอบหมายแล้วหรือยัง — ใบที่ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะ Loan Officer ที่ถูกมอบหมายตาม BR-miniloan-032@v1 · ใบที่ยังไม่ถูกมอบหมาย (Draft และ Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ (UL-miniloan-019) เท่านั้น Loan Officer ทั่วไปทำไม่ได้** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ต้องการยกเลิกต้องแจ้งเจ้าหน้าที่ และถ้าใบยังไม่ถูกมอบหมายต้องถึงหัวหน้า
