---
type: Glossary Term
title: การยกเลิกใบสมัคร (ApplicationCancellation)
description: การจบใบสมัครก่อนถึงการเบิกจ่าย โดยใบนั้นไปอยู่สถานะสุดท้าย Cancelled — ทำได้ตั้งแต่ Draft ถึง Approved เท่านั้น ใบที่ Disbursed แล้วยกเลิกไม่ได้ · **สั่งได้เฉพาะเจ้าหน้าที่ (Loan Officer) และต้องระบุเหตุผลเสมอ** — Applicant ยกเลิกใบของตัวเองไม่ได้ ต้องแจ้งเจ้าหน้าที่ · เป็นวิธีแก้เดียวเมื่อใบสมัครเดินสถานะผิด เพราะ BR-miniloan-010@v1 ไม่มีเส้นถอยกลับ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมายได้ ยังไม่ตัดสิน — ดู Q-miniloan-012
resource: ../requirements/REQ-miniloan-002.md
tags: [miniloan, glossary]
id: UL-miniloan-020
status: draft
term_th: การยกเลิกใบสมัคร
term_en: ApplicationCancellation
not_to_confuse_with: [UL-miniloan-008]
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:d392a3c833ab02b14e931e044c775c78e6eca955b064463b51f89e301cfa6da8
---

# UL-miniloan-020

## นิยาม
การจบใบสมัครก่อนถึงการเบิกจ่าย โดยใบนั้นไปอยู่สถานะสุดท้าย Cancelled — ทำได้ตั้งแต่ Draft ถึง Approved เท่านั้น ใบที่ Disbursed แล้วยกเลิกไม่ได้ · **สั่งได้เฉพาะเจ้าหน้าที่ (Loan Officer) และต้องระบุเหตุผลเสมอ** — Applicant ยกเลิกใบของตัวเองไม่ได้ ต้องแจ้งเจ้าหน้าที่ · เป็นวิธีแก้เดียวเมื่อใบสมัครเดินสถานะผิด เพราะ BR-miniloan-010@v1 ไม่มีเส้นถอยกลับ · ใครยกเลิกใบที่ยังไม่ถูกมอบหมายได้ ยังไม่ตัดสิน — ดู Q-miniloan-012

| เรื่อง | ค่า |
|---|---|
| คำไทย | การยกเลิกใบสมัคร |
| ชื่อในระบบ | ApplicationCancellation |
| เรียกอีกอย่างว่า | ยกเลิกใบสมัคร · Cancel |
| กลายเป็น entity | — |

## ห้ามสับสนกับ

- [UL-miniloan-008](UL-miniloan-008.md)

## ใช้ที่ไหน

- [REQ-miniloan-002 · อนุมัติและเบิกจ่าย](../requirements/REQ-miniloan-002.md)
