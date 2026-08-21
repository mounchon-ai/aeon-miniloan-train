---
type: Example
title: happy — เห็นครบทั้ง 2 ใบและ 1 บัญชีของตัวเองตามปกติ
description: เห็นครบทั้ง 2 ใบและ 1 บัญชีของตัวเองตามปกติ
resource: ../rules/BR-miniloan-033@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-136
status: draft
kind: happy
proves: [BR-miniloan-033@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:82a4e676734297df38d3d3bf74bd7d615aeb49fa7836b7d200d7bf9badb75d2b
---

# EX-miniloan-136

## กำหนดให้ (given)
ผู้สมัคร ก. ที่มีใบสมัคร 2 ใบและบัญชีสินเชื่อ 1 บัญชีเป็นของตัวเอง

## เมื่อ (when)
ก. เรียกดูรายการใบสมัครและบัญชีของตัวเอง

## แล้ว (then)
เห็นครบทั้ง 2 ใบและ 1 บัญชีของตัวเองตามปกติ

## พิสูจน์กฎ

- [BR-miniloan-033@v1](../rules/BR-miniloan-033@v1.md) ✅ ปัจจุบัน — Applicant เห็นและเรียกดูได้เฉพาะใบสมัครและบัญชีสินเชื่อที่ตัวเองเป็นเจ้าของเท่านั้น · ขอบเขตนี้ต้องถูกบังคับที่ฝั่ง API ไม่ใช่แค่ซ่อนบนหน้าจอ — เรียก API ด้วย id ของคนอื่นต้องถูกปฏิเสธ
