# UI-miniloan-007 · พิจารณาใบสมัคร

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-007`
- **จุดประสงค์:** Loan Officer เปิดดูใบสมัครและผลประเมิน แล้วอนุมัติ ปฏิเสธ ยกเลิก หรือสั่งเบิกจ่าย
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-002`
- **ชนิดของหน้า:** `rows` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | พิจารณาใบสมัคร | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `applicant-info` (columns)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | ชื่อผู้สมัคร | — | `ui-miniloan-007-ent-001-full-name` |
| field | อายุ | — | `ui-miniloan-007-ent-002-age` |
| field | รายได้ต่อเดือน | — | `ui-miniloan-007-ent-002-monthly-income` |
| field | อายุงาน (เดือน) | — | `ui-miniloan-007-ent-002-current-employment-months` |
| field | ภาระหนี้เดิมต่อเดือน | — | `ui-miniloan-007-ent-002-existing-monthly-debt` |

### โซน `loan-request` (columns)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | จำนวนเงินกู้ที่ขอ | — | `ui-miniloan-007-ent-002-requested-amount` |
| field | จำนวนงวดที่ขอ | — | `ui-miniloan-007-ent-002-requested-term-months` |

### โซน `assessment` (columns)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | Credit Band | — | `ui-miniloan-007-ent-003-band` |
| field | วงเงินอนุมัติสูงสุด | — | `ui-miniloan-007-ent-003-max-approvable-amount` |
| field | DTI | — | `ui-miniloan-007-ent-003-dti-ratio` |
| field | เหตุผลผลการประเมิน | — | `ui-miniloan-007-ent-003-reasons` |

### โซน `decision` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | เหตุผลที่ปฏิเสธ | — | `ui-miniloan-007-ent-002-rejection-reason` |
| field | เหตุผลที่ยกเลิก | — | `ui-miniloan-007-ent-002-cancellation-reason` |
| action | อนุมัติ | → อยู่ที่หน้าเดิม แสดงสถานะใหม่ | `ui-miniloan-007-approve` |
| action | ปฏิเสธ | → อยู่ที่หน้าเดิม แสดงสถานะใหม่ | `ui-miniloan-007-reject` |
| action | ยกเลิกใบสมัคร | → กลับไปที่คิวใบสมัครที่มอบหมายให้ฉัน | `ui-miniloan-007-cancel` |
| action | สั่งเบิกจ่าย | → อยู่ที่หน้าเดิม แสดงสถานะใหม่ | `ui-miniloan-007-disburse` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-007-approve`
- `ui-miniloan-007-cancel`
- `ui-miniloan-007-disburse`
- `ui-miniloan-007-ent-001-full-name`
- `ui-miniloan-007-ent-002-age`
- `ui-miniloan-007-ent-002-cancellation-reason`
- `ui-miniloan-007-ent-002-current-employment-months`
- `ui-miniloan-007-ent-002-existing-monthly-debt`
- `ui-miniloan-007-ent-002-monthly-income`
- `ui-miniloan-007-ent-002-rejection-reason`
- `ui-miniloan-007-ent-002-requested-amount`
- `ui-miniloan-007-ent-002-requested-term-months`
- `ui-miniloan-007-ent-003-band`
- `ui-miniloan-007-ent-003-dti-ratio`
- `ui-miniloan-007-ent-003-max-approvable-amount`
- `ui-miniloan-007-ent-003-reasons`
- `ui-miniloan-007-reject`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | ไม่เกิด | ไม่เกิด — เข้าหน้านี้ได้ต่อเมื่อมีใบสมัครที่ถูกมอบหมายอยู่แล้วเท่านั้น |
| `loading` | เกิด | ระหว่างกดอนุมัติ ปฏิเสธ ยกเลิก หรือเบิกจ่าย ปุ่มที่กดถูก disable และแสดง spinner ป้องกันกดซ้ำตาม BR-miniloan-043@v1 |
| `error` | เกิด | แสดงข้อความ error ตามเหตุ เช่น เกินวงเงินอนุมัติสูงสุด (BR-miniloan-012@v1) หรือไม่ระบุเหตุผล (BR-miniloan-013@v1) โดยไม่เปลี่ยนสถานะใบสมัคร |
| `unauthorized` | เกิด | แสดงข้อความว่าไม่มีสิทธิ์ดำเนินการกับใบสมัครนี้ เมื่อพยายามเปิดใบที่มอบหมายให้ Loan Officer คนอื่นตาม BR-miniloan-032@v1 |
| `overflow` | ไม่เกิด | ไม่เกิด — เนื้อหาต่อหนึ่งใบสมัครมีจำนวนคงที่ |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

