---
type: Requirement
title: รับและประเมินใบสมัคร
description: รับใบสมัครสินเชื่อส่วนบุคคลตั้งแต่ร่างจนยื่น และให้ระบบประเมินตามกฎที่กำหนดเพื่อจัด Credit Band พร้อมเหตุผล
resource: ../../requirements/REQ-miniloan-001.md
tags: [miniloan, requirement]
id: REQ-miniloan-001
status: draft
actor: Applicant / System
rules: [BR-miniloan-001, BR-miniloan-002, BR-miniloan-003, BR-miniloan-004, BR-miniloan-005, BR-miniloan-006, BR-miniloan-007, BR-miniloan-008, BR-miniloan-009]
domain_concepts: [UL-miniloan-001, UL-miniloan-002, UL-miniloan-003, UL-miniloan-009, UL-miniloan-010, UL-miniloan-011, UL-miniloan-012, UL-miniloan-014, UL-miniloan-023, UL-miniloan-024]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:486ad9b8616ea3a3dd9127b1893a9f58be338d167556b89a1c6b650004c5cbdd
---

# REQ-miniloan-001

## เป้าหมาย
รับใบสมัครสินเชื่อส่วนบุคคลตั้งแต่ร่างจนยื่น และให้ระบบประเมินตามกฎที่กำหนดเพื่อจัด Credit Band พร้อมเหตุผล

**actor:** Applicant / System · **ความสำคัญ:** high · **มีหน้าจอ:** ใช่

## คุณค่าทางธุรกิจ
คัดกรองผู้สมัครที่ไม่ผ่านเกณฑ์ออกอัตโนมัติ และให้เจ้าหน้าที่เห็นเหตุผลของคะแนนก่อนตัดสินใจ

## กฎที่ยังใช้อยู่

| กฎ | ชนิด | ข้อความ | ตัวอย่าง |
|---|---|---|---|
| [BR-miniloan-001@v1](../rules/BR-miniloan-001@v1.md) | constraint | ผู้สมัครต้องมีอายุ 20–60 ปี · รายได้ต่อเดือน ≥ 15,000 บาท · อายุงานปัจจุบัน ≥ 4 เดือน | 4 |
| [BR-miniloan-002@v1](../rules/BR-miniloan-002@v1.md) | calculation | Debt-to-Income หลังรวมงวดใหม่ ต้อง ≤ 70% ของรายได้ต่อเดือน โดย DTI = (ภาระหนี้เดิมต่อเดือน + งวดใหม่) / รายได้ต่อเดือน · "งวดใหม่" คือค่างวด EMI ที่คำนวณด้วยสูตรเดียวกับ BR-miniloan-016@v1 จากจำนวนเงินกู้ที่ขอ จำนวนงวดที่ขอ และอัตราดอกเบี้ยที่มีผล ณ วันประเมิน — ไม่มีสูตรประมาณแยกอีกชุด · DTI คำนวณครั้งเดียวตอนยื่นจากจำนวนเงินกู้ที่ขอ และไม่คำนวณใหม่แม้เจ้าหน้าที่จะปรับวงเงินลงตาม BR-miniloan-012@v1 | 4 |
| [BR-miniloan-003@v1](../rules/BR-miniloan-003@v1.md) | calculation | วงเงินอนุมัติสูงสุด = 5 เท่าของรายได้ต่อเดือน และไม่เกิน 1,000,000 บาท | 4 |
| [BR-miniloan-004@v1](../rules/BR-miniloan-004@v1.md) | constraint | ใบสมัครรับได้เฉพาะจำนวนเงินกู้ 10,000 – 1,000,000 บาท และจำนวนงวด 6 – 60 งวด (รายเดือน) | 4 |
| [BR-miniloan-005@v1](../rules/BR-miniloan-005@v1.md) | policy | อัตราดอกเบี้ยเป็นแบบลดต้นลดดอก 25% ต่อปี | 2 |
| [BR-miniloan-006@v1](../rules/BR-miniloan-006@v1.md) | policy | จัด Credit Band จากผลประเมิน: Band A = ผ่านทุกเกณฑ์ + DTI ≤ 50% → อนุมัติอัตโนมัติได้ (ยังต้องให้เจ้าหน้าที่ยืนยัน) · Band B = ผ่านเกณฑ์ + DTI มากกว่า 50% ถึง 70% → ส่งเจ้าหน้าที่พิจารณา · Band C = ผิดเกณฑ์ข้อใดข้อหนึ่ง → ปฏิเสธพร้อมเหตุผล · DTI เท่ากับ 50% พอดีได้ Band A ไม่ใช่ Band B — ขอบทั้งสองแบนด์ไม่ซ้อนกันอีกต่อไป | 4 |
| [BR-miniloan-007@v1](../rules/BR-miniloan-007@v1.md) | invariant | ยื่นใบสมัครได้เฉพาะเมื่อกรอกครบ (ชื่อ อายุ รายได้ อายุงาน จำนวนเงินกู้ จำนวนงวด) — ครบแล้วสถานะเปลี่ยนเป็น Submitted และล็อกการแก้ไข · ไม่ครบ ระบบต้องปฏิเสธพร้อมระบุ field ที่ขาด และคงสถานะ Draft | 3 |
| [BR-miniloan-008@v1](../rules/BR-miniloan-008@v1.md) | policy | บันทึกร่างใบสมัครได้แม้กรอกไม่ครบ — ขั้นร่างยังไม่ตรวจ business rule เต็ม และได้ใบสมัครสถานะ Draft | 2 |
| [BR-miniloan-009@v1](../rules/BR-miniloan-009@v1.md) | invariant | เมื่อใบสมัครถูกยื่น ระบบต้องประเมินอัตโนมัติและสร้าง CreditAssessment ที่มีคะแนน Band (A/B/C) วงเงินที่อนุมัติได้ และเหตุผลที่ผ่าน/ไม่ผ่านรายเกณฑ์ | 3 |

## คำศัพท์ที่ผูกกับ requirement นี้

- [UL-miniloan-001 · ใบสมัครสินเชื่อ](../glossary/UL-miniloan-001.md)
- [UL-miniloan-002 · ข้อมูลผู้สมัคร](../glossary/UL-miniloan-002.md)
- [UL-miniloan-003 · ผลการประเมินสินเชื่อ](../glossary/UL-miniloan-003.md)
- [UL-miniloan-009 · จำนวนเงิน](../glossary/UL-miniloan-009.md)
- [UL-miniloan-010 · วงเงินอนุมัติสูงสุด](../glossary/UL-miniloan-010.md)
- [UL-miniloan-011 · จำนวนเงินกู้ที่ขอ](../glossary/UL-miniloan-011.md)
- [UL-miniloan-012 · จำนวนงวด](../glossary/UL-miniloan-012.md)
- [UL-miniloan-014 · อัตราดอกเบี้ย](../glossary/UL-miniloan-014.md)
- [UL-miniloan-023 · ยอดที่อนุมัติจริง](../glossary/UL-miniloan-023.md)
- [UL-miniloan-024 · ผู้สมัคร](../glossary/UL-miniloan-024.md)

## ฉบับที่คนอ่าน
[docs/requirements/REQ-miniloan-001.md](../../requirements/REQ-miniloan-001.md) — เนื้อความเต็มภาษาไทย
