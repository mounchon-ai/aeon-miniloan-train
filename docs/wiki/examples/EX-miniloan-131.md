---
type: Example
title: happy — **response ผ่านการตรวจกับ schema ทุกฟิลด์** — ชื่อฟิลด์ ชนิด
description: **response ผ่านการตรวจกับ schema ทุกฟิลด์** — ชื่อฟิลด์ ชนิดข้อมูล และฟิลด์ที่บังคับ ตรงกับที่ประกาศทั้งหมด · **สัญญาที่ประกาศไว้ทดสอบได้จริงด้วยเครื่อง ไม่ใช่เอกสารที่เขียนไว้เฉยๆ**
resource: ../rules/BR-miniloan-029@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-131
status: draft
kind: happy
proves: [BR-miniloan-029@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:4cd73f55471cb647ddceb9caf26c8b070acc6a9553d4e34d6816e6b81e8116f9
---

# EX-miniloan-131

## กำหนดให้ (given)
ระบบที่ประกาศสัญญา API ไว้เป็นเอกสาร OpenAPI ครอบ endpoint ทั้งหมด

## เมื่อ (when)
เรียก endpoint สร้างใบสมัครด้วย request ที่ตรงตาม schema แล้วนำ response ที่ได้ไปตรวจกับ schema ที่ประกาศไว้

## แล้ว (then)
**response ผ่านการตรวจกับ schema ทุกฟิลด์** — ชื่อฟิลด์ ชนิดข้อมูล และฟิลด์ที่บังคับ ตรงกับที่ประกาศทั้งหมด · **สัญญาที่ประกาศไว้ทดสอบได้จริงด้วยเครื่อง ไม่ใช่เอกสารที่เขียนไว้เฉยๆ**

## พิสูจน์กฎ

- [BR-miniloan-029@v1](../rules/BR-miniloan-029@v1.md) ✅ ปัจจุบัน — API ต้องมีสัญญาที่ทดสอบได้ — คำอธิบาย endpoint และ request/response schema (เช่น OpenAPI) — และ request/response จริงต้องตรงตาม schema ที่ประกาศไว้
