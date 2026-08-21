---
type: Requirement
title: สถาปัตยกรรมแยก Web กับ API
description: ให้ business logic และกฎธุรกิจทั้งหมดอยู่หลัง API เท่านั้น โดย Web เป็น client ที่เรียกใช้และไม่ถือกฎเอง เพื่อให้ทั้งสองส่วน deploy และทดสอบแยกกันได้
resource: ../../requirements/REQ-miniloan-006.md
tags: [miniloan, requirement]
id: REQ-miniloan-006
status: draft
actor: Web (client) / API
rules: [BR-miniloan-025, BR-miniloan-026, BR-miniloan-027, BR-miniloan-028, BR-miniloan-029, BR-miniloan-030, BR-miniloan-033, BR-miniloan-035, BR-miniloan-042, BR-miniloan-043]
domain_concepts: []
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:005b7f8c5e245ed2a0a95771548e6784d79a20e7148adc16c70380790a0cb759
---

# REQ-miniloan-006

## เป้าหมาย
ให้ business logic และกฎธุรกิจทั้งหมดอยู่หลัง API เท่านั้น โดย Web เป็น client ที่เรียกใช้และไม่ถือกฎเอง เพื่อให้ทั้งสองส่วน deploy และทดสอบแยกกันได้

**actor:** Web (client) / API · **ความสำคัญ:** high · **มีหน้าจอ:** ไม่

## คุณค่าทางธุรกิจ
เปลี่ยนหรือทดสอบ Web และ API ได้อิสระ และกันไม่ให้กฎธุรกิจถูกบังคับเฉพาะฝั่งหน้าจอ ซึ่งข้ามได้ด้วยการเรียก API ตรง

## กฎที่ยังใช้อยู่

| กฎ | ชนิด | ข้อความ | ตัวอย่าง |
|---|---|---|---|
| [BR-miniloan-025@v1](../rules/BR-miniloan-025@v1.md) | constraint | ทุกความสามารถใน Epic 1–8 ต้องมี API endpoint รองรับ และ business rule ต้องถูกบังคับที่ฝั่ง API เสมอ | 2 |
| [BR-miniloan-026@v1](../rules/BR-miniloan-026@v1.md) | constraint | เรียก API ด้วยข้อมูลที่ผิด business rule → API ต้องปฏิเสธพร้อมรหัสและข้อความ error ที่ชัดเจน โดยไม่พึ่งการ validate ของ Web เพียงอย่างเดียว | 2 |
| [BR-miniloan-027@v1](../rules/BR-miniloan-027@v1.md) | constraint | Web ไม่ตัดสินใจเชิงธุรกิจเอง — เมื่อต้องตัดสิน (เช่น อนุมัติได้ไหม วงเงินเท่าไร) ต้องเรียก API เท่านั้น ห้ามคำนวณเอง | 2 |
| [BR-miniloan-028@v1](../rules/BR-miniloan-028@v1.md) | constraint | เมื่อ API ปิดให้บริการ Web ต้องแสดงสถานะข้อผิดพลาดอย่างเหมาะสม และต้องไม่ crash | 2 |
| [BR-miniloan-029@v1](../rules/BR-miniloan-029@v1.md) | constraint | API ต้องมีสัญญาที่ทดสอบได้ — คำอธิบาย endpoint และ request/response schema (เช่น OpenAPI) — และ request/response จริงต้องตรงตาม schema ที่ประกาศไว้ | 2 |
| [BR-miniloan-030@v1](../rules/BR-miniloan-030@v1.md) | constraint | Web เรียก API ต้องแนบ token จำลองทุกครั้ง และ API ต้องตรวจสอบ token ก่อนให้บริการ (ระบบ auth จริงอยู่นอกขอบเขต) | 3 |
| [BR-miniloan-033@v1](../rules/BR-miniloan-033@v1.md) | constraint | Applicant เห็นและเรียกดูได้เฉพาะใบสมัครและบัญชีสินเชื่อที่ตัวเองเป็นเจ้าของเท่านั้น · ขอบเขตนี้ต้องถูกบังคับที่ฝั่ง API ไม่ใช่แค่ซ่อนบนหน้าจอ — เรียก API ด้วย id ของคนอื่นต้องถูกปฏิเสธ | 3 |
| [BR-miniloan-035@v1](../rules/BR-miniloan-035@v1.md) | constraint | ค่าเงินทุกจุดที่เกิดขึ้นในระบบต้องปัดทันทีที่เกิดด้วยวิธี round half up ไม่ใช่เก็บค่าเต็มไว้แล้วปัดตอนแสดงผล — EMI ปัดก่อน ดอกเบี้ยรายงวดปัด แล้วเงินต้นของงวด = EMI − ดอกเบี้ยที่ปัดแล้ว · จำนวนตำแหน่งทศนิยมยังไม่ถูกกำหนด (ดู DQ-miniloan-001) | 2 |
| [BR-miniloan-042@v1](../rules/BR-miniloan-042@v1.md) | policy | เมื่อ Web เรียก API แล้วล้มเหลวหรือ timeout ระบบต้องล้มทันทีและแจ้งผู้ใช้ให้สั่งใหม่เอง — ไม่มีคิว retry อัตโนมัติ และห้ามค้างรอจนกว่าจะสำเร็จ · รูปแบบการแสดงข้อผิดพลาดเป็นไปตาม BR-miniloan-028@v1 | 2 |
| [BR-miniloan-043@v1](../rules/BR-miniloan-043@v1.md) | policy | ทุกคำสั่งที่เขียนข้อมูล (สร้างรายการใหม่ หรือเปลี่ยนสถานะ) ต้องกันการยิงซ้ำด้วยข้อจำกัดไม่ซ้ำ (unique constraint) ที่ฐานข้อมูล — คำสั่งซ้ำต้องถูกปฏิเสธพร้อม error ที่ผู้ใช้เห็น ไม่ใช่คืนผลของครั้งแรกเงียบๆ และไม่ใช่ปล่อยให้เกิดรายการซ้ำแล้วให้ผู้ใช้ไปลบเอง · ฟิลด์ที่ใช้เป็น key กันซ้ำของแต่ละคำสั่งยังไม่ถูกกำหนด (ดู DQ-miniloan-009) | 2 |

## NFR

- [NFR-miniloan-001](../nfr/NFR-miniloan-001.md) — ความถูกต้องของการเงิน: จำนวนเงินเป็นชนิดข้อมูลที่แม่นยำ (ไม่ใช้ floating point) และปัดเศษสม่ำเสมอทั้งระบบ
- [NFR-miniloan-002](../nfr/NFR-miniloan-002.md) — ตรวจสอบย้อนหลัง: ทุกการเปลี่ยนสถานะบันทึกผู้กระทำและเวลา
- [NFR-miniloan-003](../nfr/NFR-miniloan-003.md) — ตรวจสอบ input ทั้งฝั่ง Web และฝั่ง API โดย API เป็นด่านสุดท้ายเสมอ และไม่เก็บข้อมูลอ่อนไหวจริง
- [NFR-miniloan-004](../nfr/NFR-miniloan-004.md) — business logic อยู่ที่ฝั่ง API และรวมศูนย์อยู่ในชั้น domain (domain-centric / layered) — Web ไม่ทำ business logic
- [NFR-miniloan-005](../nfr/NFR-miniloan-005.md) — API เป็นแบบ stateless สื่อสารด้วยรูปแบบมาตรฐาน (เช่น REST/JSON) · Web กับ API deploy และทดสอบแยกกันได้อิสระ · มีสัญญา API (เช่น OpenAPI) ให้อ้างอิงและทดสอบ · ตั้งค่า CORS/Origin ให้ Web เรียก API ได้ถูกต้อง
- [NFR-miniloan-006](../nfr/NFR-miniloan-006.md) — ความเป็นอิสระจากภาษา: requirement ไม่ผูกภาษา/เฟรมเวิร์ก ทีมเลือกได้อิสระ ตราบใดที่พฤติกรรมตรงตาม Acceptance Criteria

## ฉบับที่คนอ่าน
[docs/requirements/REQ-miniloan-006.md](../../requirements/REQ-miniloan-006.md) — เนื้อความเต็มภาษาไทย
