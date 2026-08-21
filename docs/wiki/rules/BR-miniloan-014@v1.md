---
type: Business Rule
title: เบิกจ่ายได้เฉพาะใบสมัครสถานะ Approved · เมื่อเบิกจ่ายสำเร็จ ใบสมัครเปลี่ยนเป็น D
description: เบิกจ่ายได้เฉพาะใบสมัครสถานะ Approved · เมื่อเบิกจ่ายสำเร็จ ใบสมัครเปลี่ยนเป็น Disbursed และระบบสร้าง LoanAccount สถานะ Active หนึ่งบัญชีต่อหนึ่งใบสมัคร
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, invariant]
id: BR-miniloan-014@v1
status: draft
belongs_to: REQ-miniloan-002
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-058, EX-miniloan-059, EX-miniloan-060]
golden: []
provenance: [SRC-001, SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:729293e48d06d2f17242370ae5eeeb18c4415fa1ae048c05e52b48633b4a4d20
---

# BR-miniloan-014@v1

## ข้อความของกฎ
เบิกจ่ายได้เฉพาะใบสมัครสถานะ Approved · เมื่อเบิกจ่ายสำเร็จ ใบสมัครเปลี่ยนเป็น Disbursed และระบบสร้าง LoanAccount สถานะ Active หนึ่งบัญชีต่อหนึ่งใบสมัคร

## ที่มา

> "**Given** ใบสมัครสถานะ `Approved` **When** สั่งเบิกจ่าย **Then** สถานะเปลี่ยนเป็น `Disbursed` และสร้าง LoanAccount สถานะ `Active` · **Given** ใบสมัครยังไม่ `Approved` **When** สั่งเบิกจ่าย **Then** ระบบปฏิเสธ"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-07

> "1 ผู้สมัครมีได้หลายใบสมัคร แต่ 1 ใบสมัครที่ `Disbursed` สร้าง 1 บัญชี"
> — [SRC-001](../sources/SRC-001.md) หน้า — §9 · สมมติฐาน

## พิสูจน์โดย

- [EX-miniloan-058](../examples/EX-miniloan-058.md) — happy: ใบสมัครเปลี่ยนเป็น "เบิกจ่ายแล้ว (Disbursed)" และระบบสร้างบัญชีสินเชื่อสถานะ "กำลังผ่อนชำระ (Active)" ขึ้นหนึ่งบัญชี พร้อมตารางผ่อนตาม BR-miniloan-015@v1 · หน้าจอแสดง "เบิกจ่ายเรียบร้อย — เปิดบัญชีสินเชื่อเลขที่ {เลขบัญชี}"
- [EX-miniloan-059](../examples/EX-miniloan-059.md) — exception: ระบบปฏิเสธ — "เบิกจ่ายไม่ได้ — ใบสมัครนี้ยังไม่ได้รับอนุมัติ" · ไม่มีบัญชีสินเชื่อถูกสร้าง และสถานะใบสมัครไม่เปลี่ยน
- [EX-miniloan-060](../examples/EX-miniloan-060.md) — boundary: ระบบปฏิเสธ — "เบิกจ่ายไม่ได้ — ใบสมัครนี้เบิกจ่ายไปแล้วเมื่อ {วันที่เวลา}" · **ไม่มีบัญชีสินเชื่อใบที่สองเกิดขึ้น** จำนวนบัญชีของใบสมัครนี้ยังเป็น 1

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-014@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
