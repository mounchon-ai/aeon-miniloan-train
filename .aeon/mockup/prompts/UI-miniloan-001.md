# UI-miniloan-001 · กรอกและยื่นใบสมัครสินเชื่อ

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-001`
- **จุดประสงค์:** ผู้สมัครกรอกข้อมูลใบสมัคร บันทึกเป็นร่าง และยื่นเมื่อกรอกครบ
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-001`
- **ชนิดของหน้า:** `form` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | กรอกและยื่นใบสมัครสินเชื่อ | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `personal-info` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | ชื่อ-นามสกุล | — | `ui-miniloan-001-ent-001-full-name` |
| field | อายุ | — | `ui-miniloan-001-ent-002-age` |
| field | อายุงาน (เดือน) | — | `ui-miniloan-001-ent-002-current-employment-months` |

### โซน `financial-info` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | รายได้ต่อเดือน | — | `ui-miniloan-001-ent-002-monthly-income` |
| field | ภาระหนี้เดิมต่อเดือน | — | `ui-miniloan-001-ent-002-existing-monthly-debt` |

### โซน `loan-request` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | จำนวนเงินกู้ที่ขอ | — | `ui-miniloan-001-ent-002-requested-amount` |
| field | จำนวนงวดที่ขอ | — | `ui-miniloan-001-ent-002-requested-term-months` |
| field | วงเงินอนุมัติสูงสุดโดยประมาณ | — | `ui-miniloan-001-estimated-max-approved-amount` |

### โซน `form-actions` (toolbar)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| action | บันทึกร่าง | → อยู่ที่หน้าเดิม | `ui-miniloan-001-save-draft` |
| action | ยื่นใบสมัคร | → หน้ารายละเอียดใบสมัครของฉัน | `ui-miniloan-001-submit` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-001-ent-001-full-name`
- `ui-miniloan-001-ent-002-age`
- `ui-miniloan-001-ent-002-current-employment-months`
- `ui-miniloan-001-ent-002-existing-monthly-debt`
- `ui-miniloan-001-ent-002-monthly-income`
- `ui-miniloan-001-ent-002-requested-amount`
- `ui-miniloan-001-ent-002-requested-term-months`
- `ui-miniloan-001-estimated-max-approved-amount`
- `ui-miniloan-001-save-draft`
- `ui-miniloan-001-submit`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | ไม่เกิด | ไม่เกิด — ฟอร์มกรอกใหม่แสดงช่องว่างเปล่าเสมอ ไม่มีสถานะ empty แยกต่างหาก |
| `loading` | เกิด | ระหว่างบันทึกร่างหรือยื่น ปุ่มที่กดถูก disable และแสดง spinner บนปุ่มนั้น ป้องกันกดซ้ำสองครั้งติดกันตาม BR-miniloan-043@v1 |
| `error` | เกิด | แสดงข้อความ error รายฟิลด์ใต้ช่องที่ผิด (เช่น จำนวนเงินกู้นอกช่วง) พร้อมคงข้อมูลที่กรอกไว้ทั้งหมด ไม่ล้างฟอร์ม |
| `unauthorized` | ไม่เกิด | ไม่เกิด — ผู้สมัครที่ล็อกอินแล้วเข้าหน้านี้ได้เสมอเพื่อกรอกใบสมัครของตัวเอง |
| `overflow` | ไม่เกิด | ไม่เกิด — ฟอร์มมีจำนวนฟิลด์คงที่ ไม่มีรายการที่ล้นจอ |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

