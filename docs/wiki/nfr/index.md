# NFR — ข้อกำหนดที่ไม่ใช่ฟังก์ชัน

> สารบัญนี้ถูก generate จาก `spec.json` — **ห้ามแก้ด้วยมือ** · 6 หน้า

| NFR | ชนิด | ข้อกำหนด | พิสูจน์ด้วย |
|---|---|---|---|
| [NFR-miniloan-001](NFR-miniloan-001.md) | other | ความถูกต้องของการเงิน: จำนวนเงินเป็นชนิดข้อมูลที่แม่นยำ (ไม่ใช้ floating point) และปัดเศษสม่ำเสมอทั้งระบบ | unit_test |
| [NFR-miniloan-002](NFR-miniloan-002.md) | compliance | ตรวจสอบย้อนหลัง: ทุกการเปลี่ยนสถานะบันทึกผู้กระทำและเวลา | audit_log_review |
| [NFR-miniloan-003](NFR-miniloan-003.md) | security | ตรวจสอบ input ทั้งฝั่ง Web และฝั่ง API โดย API เป็นด่านสุดท้ายเสมอ และไม่เก็บข้อมูลอ่อนไหวจริง | api_contract_test |
| [NFR-miniloan-004](NFR-miniloan-004.md) | other | business logic อยู่ที่ฝั่ง API และรวมศูนย์อยู่ในชั้น domain (domain-centric / layered) — Web ไม่ทำ business logic | architecture_review |
| [NFR-miniloan-005](NFR-miniloan-005.md) | other | API เป็นแบบ stateless สื่อสารด้วยรูปแบบมาตรฐาน (เช่น REST/JSON) · Web กับ API deploy และทดสอบแยกกันได้อิสระ · มีสัญญา API (เช่น OpenAPI) ให้อ้างอิงและทดสอบ · ตั้งค่า CORS/Origin ให้ Web เรียก API ได้ถูกต้อง | deployment_test |
| [NFR-miniloan-006](NFR-miniloan-006.md) | other | ความเป็นอิสระจากภาษา: requirement ไม่ผูกภาษา/เฟรมเวิร์ก ทีมเลือกได้อิสระ ตราบใดที่พฤติกรรมตรงตาม Acceptance Criteria | acceptance_test |
