---
type: Deferred Question
title: หัวหน้าของ Loan Officer เห็นใบสมัครที่มอบหมายให้ลูกน้องไหม — และมีลำดับชั้นของเจ
description: ระบบนี้มีลำดับชั้นของพนักงานเป็นข้อมูล แต่ลำดับชั้นนั้นไม่ให้สิทธิ์มองเห็นเพิ่ม — ENT-004 Staff มีฟิลด์ supervisorId (reference -> ENT-004, optional) บันทึกผู้บังคับบัญชาโดยตรงไว้ ส่วนขอบเขตสิทธิ์ตัดสินที่ rbac.json: delegation.enabled = false และ scopeInheritance = null จึงไม่มีการสืบทอดขอบเขตจากลูกน้องขึ้นมาหาหัวหน้า และ ROLE-003 ไม่มี ACL ข้อใดให้ดูใบสมัครที่ถูกมอบหมายให้ ROLE-002 ไปแล้ว (ACL-003 มอบหมายได้ตอน UnderReview · ACL-007 ยกเลิกได้เฉพาะใบที่ยังไม่ถูกมอบหมาย Draft/Submitted · ACL-031 เห็นเฉพาะคิวใบที่ยังไม่ถูกมอบหมาย) เมื่อรวมกับ defaultEffect: deny คำตอบคือ หัวหน้าของ Loan Officer ไม่เห็นใบสมัครที่มอบหมายให้ลูกน้องไปแล้ว
resource: ../rules/BR-miniloan-033@v1.md
tags: [miniloan, question, data_scope]
id: DQ-miniloan-002
state: answered
raised_by: BR-miniloan-033@v1
answer_phase: domain
timestamp: 2026-09-01T17:30:00+07:00
spec_hash: sha256:85d71eb85ae0d0c5f98b8c89add5c1f39429ee96e3e039e553177948a7cdc02e
---

# DQ-miniloan-002

## คำถามที่เลื่อนไป
หัวหน้าของ Loan Officer เห็นใบสมัครที่มอบหมายให้ลูกน้องไหม — และมีลำดับชั้นของเจ้าหน้าที่ในระบบนี้หรือเปล่า

| เรื่อง | ค่า |
|---|---|
| สถานะ | ✅ answered |
| ตั้งขึ้นจาก | [BR-miniloan-033@v1](../rules/BR-miniloan-033@v1.md) |
| หมวด | data_scope |
| ตอบตอนไหน | domain — `/design:datamodel` (`design`) |
| ติดอยู่ที่ | `entity:LoanOfficer (ยังไม่มี entity หรือลำดับชั้นของ role)` |

## คำตอบ
ระบบนี้มีลำดับชั้นของพนักงานเป็นข้อมูล แต่ลำดับชั้นนั้นไม่ให้สิทธิ์มองเห็นเพิ่ม — ENT-004 Staff มีฟิลด์ supervisorId (reference -> ENT-004, optional) บันทึกผู้บังคับบัญชาโดยตรงไว้ ส่วนขอบเขตสิทธิ์ตัดสินที่ rbac.json: delegation.enabled = false และ scopeInheritance = null จึงไม่มีการสืบทอดขอบเขตจากลูกน้องขึ้นมาหาหัวหน้า และ ROLE-003 ไม่มี ACL ข้อใดให้ดูใบสมัครที่ถูกมอบหมายให้ ROLE-002 ไปแล้ว (ACL-003 มอบหมายได้ตอน UnderReview · ACL-007 ยกเลิกได้เฉพาะใบที่ยังไม่ถูกมอบหมาย Draft/Submitted · ACL-031 เห็นเฉพาะคิวใบที่ยังไม่ถูกมอบหมาย) เมื่อรวมกับ defaultEffect: deny คำตอบคือ หัวหน้าของ Loan Officer ไม่เห็นใบสมัครที่มอบหมายให้ลูกน้องไปแล้ว

ตอบเมื่อ 2026-09-05

## ผลที่ตามมา

- `ENT-004`
