---
type: Business Rule
title: เมื่อเบิกจ่ายสำเร็จ ระบบต้องสร้าง RepaymentSchedule ให้บัญชีนั้นทันทีตามกฎการคำน
description: เมื่อเบิกจ่ายสำเร็จ ระบบต้องสร้าง RepaymentSchedule ให้บัญชีนั้นทันทีตามกฎการคำนวณงวดผ่อน
resource: ../requirements/REQ-miniloan-003.md
tags: [miniloan, invariant]
id: BR-miniloan-015@v1
status: draft
belongs_to: REQ-miniloan-003
kind: invariant
is_current: true
test_design: [state_transition]
proven_by: [EX-miniloan-094, EX-miniloan-095, EX-miniloan-096]
golden: []
provenance: [SRC-001]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:b957c7837e08591c4014ade812f44407de1872f786f63b4b61b7dbd5d4ee1867
---

# BR-miniloan-015@v1

## ข้อความของกฎ
เมื่อเบิกจ่ายสำเร็จ ระบบต้องสร้าง RepaymentSchedule ให้บัญชีนั้นทันทีตามกฎการคำนวณงวดผ่อน

## ที่มา

> "**Given** สร้าง LoanAccount **When** เบิกจ่ายสำเร็จ **Then** ระบบสร้าง RepaymentSchedule ตาม BR-06 ทันที"
> — [SRC-001](../sources/SRC-001.md) หน้า — §7 · US-07

## พิสูจน์โดย

- [EX-miniloan-094](../examples/EX-miniloan-094.md) — happy: ใบสมัครเปลี่ยนเป็น "เบิกจ่ายแล้ว (Disbursed)" · เกิดบัญชีสินเชื่อสถานะ "ใช้งานอยู่ (Active)" หนึ่งบัญชี · **และตารางผ่อนถูกสร้างพร้อมกันในจังหวะเดียวกัน ไม่ใช่งานที่รอทำทีหลัง** · หน้าจอแสดง **"เบิกจ่ายเรียบร้อย — เปิดบัญชีสินเชื่อเลขที่ {เลขบัญชี} พร้อมตารางผ่อน {จำนวนงวด} งวด"**
- [EX-miniloan-095](../examples/EX-miniloan-095.md) — boundary: เห็นตารางผ่อนครบทุกงวดแล้ว · **ไม่มีสถานะ "กำลังสร้างตารางผ่อน" ไม่มีหน้าว่าง และไม่ต้องกดรีเฟรชรอ** · "ทันที" หมายถึงเห็นได้ในคำขอถัดไป ไม่ใช่ภายในไม่กี่วินาที
- [EX-miniloan-096](../examples/EX-miniloan-096.md) — exception: ระบบแจ้ง **"ยังไม่มีตารางผ่อน — ตารางผ่อนจะถูกสร้างเมื่อเบิกจ่ายเรียบร้อยแล้ว"** · ไม่มีบัญชีสินเชื่อและไม่มีตารางผ่อนเกิดขึ้นก่อนการเบิกจ่าย

## ประวัติ

| เวอร์ชัน | มีผลตั้งแต่ | เหตุผล | change set |
|---|---|---|---|
| **BR-miniloan-015@v1** (หน้านี้) ✅ | — | ตั้งต้น | — |
