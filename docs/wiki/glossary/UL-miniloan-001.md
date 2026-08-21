---
type: Glossary Term
title: ใบสมัครสินเชื่อ (LoanApplication)
description: ใบสมัครสินเชื่อ ตั้งแต่ร่างจนถึงเบิกจ่าย เป็น Aggregate ฝั่ง Origination · 1 ผู้สมัครมีได้หลายใบสมัคร
resource: ../requirements/REQ-miniloan-001.md
tags: [miniloan, glossary]
id: UL-miniloan-001
status: draft
term_th: ใบสมัครสินเชื่อ
term_en: LoanApplication
not_to_confuse_with: [UL-miniloan-004]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:1165dbc6d29d5f27ec49464a7d8927ce2b06238dc5aada971957019974f7f888
---

# UL-miniloan-001

## นิยาม
ใบสมัครสินเชื่อ ตั้งแต่ร่างจนถึงเบิกจ่าย เป็น Aggregate ฝั่ง Origination · 1 ผู้สมัครมีได้หลายใบสมัคร

| เรื่อง | ค่า |
|---|---|
| คำไทย | ใบสมัครสินเชื่อ |
| ชื่อในระบบ | LoanApplication |
| เรียกอีกอย่างว่า | ใบสมัคร · คำขอสินเชื่อ |
| กลายเป็น entity | — |

## ห้ามสับสนกับ

- [UL-miniloan-004](UL-miniloan-004.md)

## ใช้ที่ไหน

- [REQ-miniloan-001 · รับและประเมินใบสมัคร](../requirements/REQ-miniloan-001.md)
- [REQ-miniloan-002 · อนุมัติและเบิกจ่าย](../requirements/REQ-miniloan-002.md)
- [REQ-miniloan-005 · ภาพรวมสถานะ (แดชบอร์ด)](../requirements/REQ-miniloan-005.md)
