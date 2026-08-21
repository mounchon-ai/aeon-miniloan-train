---
type: Example
title: exception — API ปฏิเสธเหมือนกรณีไม่มี token เลย · **การมี token ไม่พอ ต้
description: API ปฏิเสธเหมือนกรณีไม่มี token เลย · **การมี token ไม่พอ ต้องเป็น token ที่ผ่านการตรวจ** — ถ้า API รับ token ทุกอันที่แนบมา การตรวจก็ไม่มีความหมาย · ระบบ auth จริงอยู่นอกขอบเขต แต่การตรวจต้องมี
resource: ../rules/BR-miniloan-030@v1.md
tags: [miniloan, example, exception]
id: EX-miniloan-135
status: draft
kind: exception
proves: [BR-miniloan-030@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:15b513c2e6796253b9fb1e035c58ff161bfe68143daccce3dabdb43b52782610
---

# EX-miniloan-135

## กำหนดให้ (given)
คำขอที่แนบ token ซึ่งไม่ถูกต้อง (ปลอมหรือหมดอายุ)

## เมื่อ (when)
เรียก API ดึงรายการใบสมัครด้วย token นั้น

## แล้ว (then)
API ปฏิเสธเหมือนกรณีไม่มี token เลย · **การมี token ไม่พอ ต้องเป็น token ที่ผ่านการตรวจ** — ถ้า API รับ token ทุกอันที่แนบมา การตรวจก็ไม่มีความหมาย · ระบบ auth จริงอยู่นอกขอบเขต แต่การตรวจต้องมี

## พิสูจน์กฎ

- [BR-miniloan-030@v1](../rules/BR-miniloan-030@v1.md) ✅ ปัจจุบัน — Web เรียก API ต้องแนบ token จำลองทุกครั้ง และ API ต้องตรวจสอบ token ก่อนให้บริการ (ระบบ auth จริงอยู่นอกขอบเขต)
