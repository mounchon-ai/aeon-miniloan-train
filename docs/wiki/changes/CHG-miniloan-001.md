---
type: Change Set
title: หัวหน้าเจ้าหน้าที่สินเชื่อถูกตัดสินแล้วว่าเป็น actor แยก ไม่ใช่ Loan Officer คนห
description: กระทบ 1 โหนด · มีผล 2026-08-14
resource: ../rules/BR-miniloan-031@v2.md
tags: [miniloan, change]
id: CHG-miniloan-001
requested_by: เจ้าของสเปก (ตอบในแชท รอบ /req:ask ตอบการ์ดแดง Q-miniloan-012)
approved_by: เจ้าของสเปก
effective_from: 2026-08-14
affects: [BR-miniloan-031@v2]
invalidates: []
triggered_by: [SRC-013, SRC-015]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:4df7980cc75c4ec8be989a5ea0995f37a9bce9097d220428ba88e321da460c15
---

# CHG-miniloan-001

## ทำไมถึงเปลี่ยน
หัวหน้าเจ้าหน้าที่สินเชื่อถูกตัดสินแล้วว่าเป็น actor แยก ไม่ใช่ Loan Officer คนหนึ่ง (คำตอบ Q-miniloan-012) · ประโยคเดิมที่ว่า "ยกเลิกได้เฉพาะ Loan Officer เท่านั้น" จึงแคบเกินความจริง และทำให้เส้นยกเลิกใบสถานะ Draft กับ Submitted ที่ BR-miniloan-010@v1 เปิดไว้ ไม่มีใครเดินได้

| เรื่อง | ค่า |
|---|---|
| ใครขอ | เจ้าของสเปก (ตอบในแชท รอบ /req:ask ตอบการ์ดแดง Q-miniloan-012) |
| ใครอนุมัติ | เจ้าของสเปก |
| มีผลตั้งแต่ | 2026-08-14 |
| เอกสารที่ทำให้เปลี่ยน | [SRC-013](../sources/SRC-013.md) · [SRC-015](../sources/SRC-015.md) |

## เปลี่ยนอะไร

| โหนด | จาก | เป็น |
|---|---|---|
| [BR-miniloan-031@v2](../rules/BR-miniloan-031@v2.md) | [BR-miniloan-031@v1](../rules/BR-miniloan-031@v1.md) สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitte | สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitte |

## หมายเหตุ
ไม่มีเลขเฉลยถูกทำให้ใช้ไม่ได้ เพราะยังไม่มี CALC และยังไม่มี golden dataset ในโมดูลนี้เลย · EX-miniloan-063 ไม่ถูกยกไป @v2 ตามการตัดสินของเจ้าของ — ข้อความที่ผู้ใช้เห็น "กรุณาติดต่อเจ้าหน้าที่" ไม่ตรงกับกฎใหม่ที่ให้เฉพาะหัวหน้ายกเลิกใบที่ยังไม่ถูกมอบหมาย · ผลคือท่อนที่เปลี่ยน (เส้นยกเลิกใบที่ยังไม่ถูกมอบหมาย) ยังไม่มีใครพิสูจน์ ต้องตามด้วย /req:example BR-miniloan-031@v2
