# UI-miniloan-007 · รายละเอียดใบสมัคร (เจ้าหน้าที่สินเชื่อ)

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-007`
- **จุดประสงค์:** ให้ Loan Officer ที่ถูกมอบหมายพิจารณา อนุมัติ ปฏิเสธ ยกเลิก หรือสั่งเบิกจ่ายใบสมัคร
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-002`
- **ชนิดของหน้า:** `form` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | รายละเอียดใบสมัคร (เจ้าหน้าที่สินเชื่อ) | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `applicant-info` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | ชื่อผู้สมัคร | — | `ui-miniloan-007-ent-002-name` |
| field | อายุ | — | `ui-miniloan-007-ent-003-age` |
| field | รายได้ต่อเดือน | — | `ui-miniloan-007-ent-003-monthly-income` |

### โซน `application` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | จำนวนเงินกู้ที่ขอ | — | `ui-miniloan-007-ent-001-requested-amount` |
| field | สถานะ | — | `ui-miniloan-007-ent-001-status` |
| field | จำนวนเงินที่อนุมัติ | — | `ui-miniloan-007-ent-001-approved-amount` |
| action | อนุมัติ | → อยู่ที่หน้าเดิม | `ui-miniloan-007-approve` |
| action | สั่งเบิกจ่าย | → อยู่ที่หน้าเดิม | `ui-miniloan-007-disburse` |

### โซน `assessment` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | Credit Band | — | `ui-miniloan-007-ent-004-band` |
| field | DTI | — | `ui-miniloan-007-ent-004-dti-percent` |
| field | วงเงินอนุมัติสูงสุด | — | `ui-miniloan-007-ent-004-max-approvable-amount` |

### โซน `decision` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | เหตุผลปฏิเสธ | — | `ui-miniloan-007-ent-001-rejection-reason` |
| field | เหตุผลยกเลิก | — | `ui-miniloan-007-ent-001-cancellation-reason` |
| action | ปฏิเสธ | → อยู่ที่หน้าเดิม | `ui-miniloan-007-reject` |
| action | ยกเลิก | → อยู่ที่หน้าเดิม | `ui-miniloan-007-cancel` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-007-approve`
- `ui-miniloan-007-cancel`
- `ui-miniloan-007-disburse`
- `ui-miniloan-007-ent-001-approved-amount`
- `ui-miniloan-007-ent-001-cancellation-reason`
- `ui-miniloan-007-ent-001-rejection-reason`
- `ui-miniloan-007-ent-001-requested-amount`
- `ui-miniloan-007-ent-001-status`
- `ui-miniloan-007-ent-002-name`
- `ui-miniloan-007-ent-003-age`
- `ui-miniloan-007-ent-003-monthly-income`
- `ui-miniloan-007-ent-004-band`
- `ui-miniloan-007-ent-004-dti-percent`
- `ui-miniloan-007-ent-004-max-approvable-amount`
- `ui-miniloan-007-reject`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | ไม่เกิด | ไม่เกิด — เข้าถึงจากรายการงานของฉันเท่านั้น |
| `loading` | เกิด | แสดง skeleton/spinner ทับตำแหน่งข้อมูล ปุ่มที่ยิง action ถูกปิดใช้งานชั่วคราวระหว่างรอ |
| `error` | เกิด | แสดงข้อความ "เกิดข้อผิดพลาด ลองใหม่อีกครั้ง" พร้อมปุ่มลองใหม่ ข้อมูลเดิมที่เคยโหลดสำเร็จยังคงอยู่บนจอ ไม่หายไป |
| `unauthorized` | เกิด | แสดงข้อความ "ใบสมัครนี้ไม่ได้ถูกมอบหมายให้คุณ" แทนข้อมูล |
| `overflow` | ไม่เกิด | ไม่เกิด — ข้อมูลใบสมัครหนึ่งใบมีขนาดคงที่ |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

