---
type: Example
title: happy — ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุด
description: ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุดท้าย · หน้าจอแสดง **"ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง"** · ระบบบันทึกว่าหัวหน้าเป็นผู้กระทำพร้อมเวลาตาม NFR-miniloan-002 · **เส้น Draft → Cancelled ที่ BR-miniloan-010@v1 ประกาศไว้ตั้งแต่แรก เดินได้จริงเป็นครั้งแรกที่ใบนี้**
resource: ../rules/BR-miniloan-031@v2.md
tags: [miniloan, example, happy]
id: EX-miniloan-091
status: draft
kind: happy
proves: [BR-miniloan-031@v2]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:81a5b65d7e0f8090de4aae5fd463724d37343be498cb933d46840aad03bafd42
---

# EX-miniloan-091

## กำหนดให้ (given)
ใบสมัครสถานะ "ร่าง (Draft)" ที่ยังไม่มีใครถูกมอบหมาย — การมอบหมายเกิดตอนใบเข้าสู่การพิจารณาตาม BR-miniloan-032@v1 · ผู้ที่ล็อกอินอยู่คือหัวหน้าเจ้าหน้าที่สินเชื่อ

## เมื่อ (when)
หัวหน้าสั่งยกเลิกใบสมัครนั้นพร้อมระบุเหตุผลตาม BR-miniloan-047@v1

## แล้ว (then)
ใบสมัครเปลี่ยนเป็น "ยกเลิกแล้ว (Cancelled)" ซึ่งเป็นสถานะสุดท้าย · หน้าจอแสดง **"ยกเลิกใบสมัครเรียบร้อย — ใบนี้จบแล้ว สร้างใบใหม่ได้ถ้าต้องการยื่นอีกครั้ง"** · ระบบบันทึกว่าหัวหน้าเป็นผู้กระทำพร้อมเวลาตาม NFR-miniloan-002 · **เส้น Draft → Cancelled ที่ BR-miniloan-010@v1 ประกาศไว้ตั้งแต่แรก เดินได้จริงเป็นครั้งแรกที่ใบนี้**

## พิสูจน์กฎ

- [BR-miniloan-031@v2](../rules/BR-miniloan-031@v2.md) ✅ ปัจจุบัน — สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) แยกตามว่าใบนั้นถูกมอบหมายแล้วหรือยัง — ใบที่ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะ Loan Officer ที่ถูกมอบหมายตาม BR-miniloan-032@v1 · ใบที่ยังไม่ถูกมอบหมาย (Draft และ Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ (UL-miniloan-019) เท่านั้น Loan Officer ทั่วไปทำไม่ได้** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ต้องการยกเลิกต้องแจ้งเจ้าหน้าที่ และถ้าใบยังไม่ถูกมอบหมายต้องถึงหัวหน้า
