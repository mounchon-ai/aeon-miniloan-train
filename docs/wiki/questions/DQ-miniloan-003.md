---
type: Deferred Question
title: ถ้ามอบหมายใบสมัครใหม่ให้เจ้าหน้าที่คนอื่น คนเดิมยังเห็นใบสมัครนั้นอยู่ไหม — และป
description: เก็บประวัติการมอบหมายทุกครั้งเป็นแถวใหม่ใน ApplicationAssignment ไม่ทับของเดิม — ส่วนสิทธิ์การมองเห็น/อนุมัติของใบสมัคร อ้างอิงเฉพาะ LoanApplication.assignedLoanOfficerId ปัจจุบันเท่านั้น เจ้าหน้าที่คนเดิมที่ถูกถอดออกจะไม่เห็นใบสมัครนั้นอีกผ่านเส้นทางปกติ
resource: ../rules/BR-miniloan-033@v1.md
tags: [miniloan, question, data_scope]
id: DQ-miniloan-003
state: answered
raised_by: BR-miniloan-033@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:25c578db6f9e346d5ca470d885528f331c67227cda7d5b1bef637b43a0c2bcfd
---

# DQ-miniloan-003

## คำถามที่เลื่อนไป
ถ้ามอบหมายใบสมัครใหม่ให้เจ้าหน้าที่คนอื่น คนเดิมยังเห็นใบสมัครนั้นอยู่ไหม — และประวัติการมอบหมายต้องเก็บไว้หรือทับของเดิม

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-033@v1](../rules/BR-miniloan-033@v1.md) |
| หมวด | data_scope |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:ApplicationAssignment (UL-miniloan-013 ยังไม่ถูกผูกกับ entity)` |

## คำตอบ
เก็บประวัติการมอบหมายทุกครั้งเป็นแถวใหม่ใน ApplicationAssignment ไม่ทับของเดิม — ส่วนสิทธิ์การมองเห็น/อนุมัติของใบสมัคร อ้างอิงเฉพาะ LoanApplication.assignedLoanOfficerId ปัจจุบันเท่านั้น เจ้าหน้าที่คนเดิมที่ถูกถอดออกจะไม่เห็นใบสมัครนั้นอีกผ่านเส้นทางปกติ

ตอบเมื่อ 2026-09-03T03:39:19Z

## ผลที่ตามมา

- `ENT-013`
