---
type: Requirement
title: ภาพรวมสถานะ (แดชบอร์ด)
description: เห็นจำนวนใบสมัครและบัญชีแยกตามสถานะในหน้าเดียว เพื่อจัดลำดับงาน
resource: ../../requirements/REQ-miniloan-005.md
tags: [miniloan, requirement]
id: REQ-miniloan-005
status: draft
actor: Loan Officer
rules: [BR-miniloan-024]
domain_concepts: [UL-miniloan-001, UL-miniloan-004]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:2e8cd5460ce4135aa7d2c841f217f71b394a08d4adb98b0e28b1fda0c1ae5cbf
---

# REQ-miniloan-005

## เป้าหมาย
เห็นจำนวนใบสมัครและบัญชีแยกตามสถานะในหน้าเดียว เพื่อจัดลำดับงาน

**actor:** Loan Officer · **ความสำคัญ:** low · **มีหน้าจอ:** ใช่

## คุณค่าทางธุรกิจ
เจ้าหน้าที่รู้ว่ามีงานค้างอยู่กี่ใบและอยู่ขั้นไหน โดยไม่ต้องไล่เปิดทีละใบ

## กฎที่ยังใช้อยู่

| กฎ | ชนิด | ข้อความ | ตัวอย่าง |
|---|---|---|---|
| [BR-miniloan-024@v1](../rules/BR-miniloan-024@v1.md) | policy | แดชบอร์ดของ Loan Officer แสดงจำนวนแยกตามสถานะ Submitted / UnderReview / Approved / Active / Closed โดยนับข้ามทั้ง LoanApplication และ LoanAccount | 3 |

## คำศัพท์ที่ผูกกับ requirement นี้

- [UL-miniloan-001 · ใบสมัครสินเชื่อ](../glossary/UL-miniloan-001.md)
- [UL-miniloan-004 · บัญชีสินเชื่อ](../glossary/UL-miniloan-004.md)

## ฉบับที่คนอ่าน
[docs/requirements/REQ-miniloan-005.md](../../requirements/REQ-miniloan-005.md) — เนื้อความเต็มภาษาไทย
