# Requirement — ลูกค้าอยากได้อะไร

> สารบัญนี้ถูก generate จาก `spec.json` — **ห้ามแก้ด้วยมือ** · 6 หน้า

| requirement | actor | กฎที่ยังใช้ | ยังไม่มีตัวอย่าง |
|---|---|---|---|
| [REQ-miniloan-001](REQ-miniloan-001.md) รับและประเมินใบสมัคร | Applicant / System | 9 | 0 |
| [REQ-miniloan-002](REQ-miniloan-002.md) อนุมัติและเบิกจ่าย | Loan Officer (พิจารณา อนุมัติ ปฏิเสธ เบิกจ่าย) / หัวหน้าเจ้าหน้าที่สินเชื่อ (สั่งมอบหมายใบสมัคร) | 8 | 0 |
| [REQ-miniloan-003](REQ-miniloan-003.md) สร้างบัญชีและตารางผ่อน | System (สร้างตารางผ่อน) / Applicant (ดูตารางของบัญชีตัวเองอย่างเดียว) | 8 | 0 |
| [REQ-miniloan-004](REQ-miniloan-004.md) รับชำระและปิดบัญชี | Operations (บันทึกการชำระและปิดบัญชี) / Applicant (ขอยอดปิดบัญชีและดูสถานะเท่านั้น — ปิดบัญชีเองไม่ได้) | 17 | 🔴 1 |
| [REQ-miniloan-005](REQ-miniloan-005.md) ภาพรวมสถานะ (แดชบอร์ด) | Loan Officer | 1 | 0 |
| [REQ-miniloan-006](REQ-miniloan-006.md) สถาปัตยกรรมแยก Web กับ API | Web (client) / API | 10 | 0 |
