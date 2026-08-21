---
type: Requirement
title: อนุมัติและเบิกจ่าย
description: ให้เจ้าหน้าที่พิจารณาอนุมัติหรือปฏิเสธใบสมัครแบบ 1 ระดับ แล้วสั่งเบิกจ่ายเพื่อเปิดบัญชีสินเชื่อ
resource: ../../requirements/REQ-miniloan-002.md
tags: [miniloan, requirement]
id: REQ-miniloan-002
status: draft
actor: Loan Officer (พิจารณา อนุมัติ ปฏิเสธ เบิกจ่าย) / หัวหน้าเจ้าหน้าที่สินเชื่อ (สั่งมอบหมายใบสมัคร)
rules: [BR-miniloan-010, BR-miniloan-011, BR-miniloan-012, BR-miniloan-013, BR-miniloan-014, BR-miniloan-031, BR-miniloan-032, BR-miniloan-047]
domain_concepts: [UL-miniloan-001, UL-miniloan-003, UL-miniloan-004, UL-miniloan-010, UL-miniloan-011, UL-miniloan-013, UL-miniloan-019, UL-miniloan-020]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:406490d2ec35d5920925b444343dcd5f82877ab513a03151c61f2b4b7c9d06ad
---

# REQ-miniloan-002

## เป้าหมาย
ให้เจ้าหน้าที่พิจารณาอนุมัติหรือปฏิเสธใบสมัครแบบ 1 ระดับ แล้วสั่งเบิกจ่ายเพื่อเปิดบัญชีสินเชื่อ

**actor:** Loan Officer (พิจารณา อนุมัติ ปฏิเสธ เบิกจ่าย) / หัวหน้าเจ้าหน้าที่สินเชื่อ (สั่งมอบหมายใบสมัคร) · **ความสำคัญ:** high · **มีหน้าจอ:** ใช่

## คุณค่าทางธุรกิจ
มีจุดตัดสินใจของคนที่ตรวจสอบย้อนหลังได้ และกันการเดินสถานะข้ามขั้นที่ทำให้สัญญาเริ่มโดยไม่มีคนอนุมัติ

## กฎที่ยังใช้อยู่

| กฎ | ชนิด | ข้อความ | ตัวอย่าง |
|---|---|---|---|
| [BR-miniloan-010@v1](../rules/BR-miniloan-010@v1.md) | invariant | LoanApplication เดินสถานะได้เฉพาะตามเส้นทางที่กำหนด: Draft → Submitted → UnderReview → Approved → Disbursed และ UnderReview → Rejected · มีเส้นยกเลิกเพิ่ม: Draft, Submitted, UnderReview และ Approved → Cancelled ได้ · ใบที่ Disbursed แล้วยกเลิกไม่ได้ทุกกรณี ต้องไปทางปิดบัญชีตาม BR-miniloan-021@v1 แทน · Rejected และ Cancelled เป็นสถานะสุดท้ายทั้งคู่ ไม่มีเส้นออก · เปลี่ยนสถานะได้ผ่าน method บน Aggregate เท่านั้น (Submit(), Approve(), Reject(), Disburse(), Cancel()) — ถอยสถานะกลับไม่ได้ วิธีแก้เมื่อเดินผิดคือยกเลิกแล้วสร้างใบใหม่ | 4 |
| [BR-miniloan-011@v1](../rules/BR-miniloan-011@v1.md) | invariant | อนุมัติได้เฉพาะใบสมัครสถานะ UnderReview และเมื่ออนุมัติต้องบันทึกผู้อนุมัติและเวลาไว้ด้วย | 3 |
| [BR-miniloan-012@v1](../rules/BR-miniloan-012@v1.md) | invariant | ถ้าจำนวนเงินกู้ที่ขอ > วงเงินอนุมัติสูงสุดตาม BR-miniloan-003@v1 ระบบต้องเตือนและให้ปรับวงเงินก่อน จึงจะอนุมัติได้ | 3 |
| [BR-miniloan-013@v1](../rules/BR-miniloan-013@v1.md) | invariant | ปฏิเสธใบสมัครต้องระบุเหตุผลเสมอ และสถานะ Rejected เป็นสถานะสุดท้าย — พยายามอนุมัติใบที่ Rejected แล้วต้องถูกปฏิเสธเป็น invalid transition | 3 |
| [BR-miniloan-014@v1](../rules/BR-miniloan-014@v1.md) | invariant | เบิกจ่ายได้เฉพาะใบสมัครสถานะ Approved · เมื่อเบิกจ่ายสำเร็จ ใบสมัครเปลี่ยนเป็น Disbursed และระบบสร้าง LoanAccount สถานะ Active หนึ่งบัญชีต่อหนึ่งใบสมัคร | 3 |
| [BR-miniloan-031@v2](../rules/BR-miniloan-031@v2.md) | invariant | สิทธิ์เดินสถานะ LoanApplication แยกตามเส้น: Draft → Submitted ทำได้เฉพาะ Applicant ที่เป็นเจ้าของใบสมัครนั้น · Submitted → UnderReview เกิดจากผลการประเมินของ System ไม่มีคนกด · UnderReview → Approved, UnderReview → Rejected และ Approved → Disbursed ทำได้เฉพาะ Loan Officer · **การยกเลิก (→ Cancelled) แยกตามว่าใบนั้นถูกมอบหมายแล้วหรือยัง — ใบที่ถูกมอบหมายแล้ว ยกเลิกได้เฉพาะ Loan Officer ที่ถูกมอบหมายตาม BR-miniloan-032@v1 · ใบที่ยังไม่ถูกมอบหมาย (Draft และ Submitted) ยกเลิกได้เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อ (UL-miniloan-019) เท่านั้น Loan Officer ทั่วไปทำไม่ได้** · Applicant ทำ Approve, Reject, Disburse และ Cancel ไม่ได้ทุกกรณีทุกสถานะ — ต้องการยกเลิกต้องแจ้งเจ้าหน้าที่ และถ้าใบยังไม่ถูกมอบหมายต้องถึงหัวหน้า | 7 |
| [BR-miniloan-032@v1](../rules/BR-miniloan-032@v1.md) | invariant | ใบสมัครที่เข้าสู่การพิจารณาต้องถูกมอบหมาย (assign) ให้ Loan Officer หนึ่งคน โดย**หัวหน้าเป็นผู้สั่งมอบหมาย** — ระบบไม่กระจายงานเอง และเจ้าหน้าที่หยิบงานเองไม่ได้ · เฉพาะคนที่ถูกมอบหมายเท่านั้นที่กดอนุมัติหรือปฏิเสธใบสมัครนั้นได้ Loan Officer คนอื่นทำไม่ได้แม้จะมีสิทธิ์ระดับเดียวกัน · ใบสมัครที่ยังไม่ถูกมอบหมายจึงอนุมัติหรือปฏิเสธไม่ได้เลย ต้องรอหัวหน้าจ่ายงานก่อน | 3 |
| [BR-miniloan-047@v1](../rules/BR-miniloan-047@v1.md) | invariant | ยกเลิกใบสมัครต้องระบุเหตุผลเสมอ และเก็บเหตุผลนั้นไว้กับใบสมัคร — เช่นเดียวกับที่ BR-miniloan-013@v1 บังคับกับการปฏิเสธ · สั่งยกเลิกโดยไม่ระบุเหตุผลต้องถูกปฏิเสธและใบสมัครไม่เปลี่ยนสถานะ | 2 |

## คำศัพท์ที่ผูกกับ requirement นี้

- [UL-miniloan-001 · ใบสมัครสินเชื่อ](../glossary/UL-miniloan-001.md)
- [UL-miniloan-003 · ผลการประเมินสินเชื่อ](../glossary/UL-miniloan-003.md)
- [UL-miniloan-004 · บัญชีสินเชื่อ](../glossary/UL-miniloan-004.md)
- [UL-miniloan-010 · วงเงินอนุมัติสูงสุด](../glossary/UL-miniloan-010.md)
- [UL-miniloan-011 · จำนวนเงินกู้ที่ขอ](../glossary/UL-miniloan-011.md)
- [UL-miniloan-013 · การมอบหมายใบสมัคร](../glossary/UL-miniloan-013.md)
- [UL-miniloan-019 · หัวหน้าเจ้าหน้าที่สินเชื่อ](../glossary/UL-miniloan-019.md)
- [UL-miniloan-020 · การยกเลิกใบสมัคร](../glossary/UL-miniloan-020.md)

## ฉบับที่คนอ่าน
[docs/requirements/REQ-miniloan-002.md](../../requirements/REQ-miniloan-002.md) — เนื้อความเต็มภาษาไทย
