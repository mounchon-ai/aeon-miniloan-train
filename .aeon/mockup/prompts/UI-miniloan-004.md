# UI-miniloan-004 · ตารางผ่อนชำระของฉัน

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-004`
- **จุดประสงค์:** ผู้สมัครดูตารางผ่อนชำระทั้งตารางพร้อมสถานะรายงวด
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-001`
- **ชนิดของหน้า:** `form` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | ตารางผ่อนชำระของฉัน | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `schedule-table` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | งวดที่ | — | `ui-miniloan-004-ent-008-installment-number` |
| field | วันครบกำหนด | — | `ui-miniloan-004-ent-008-due-date` |
| field | ค่างวด (EMI) | — | `ui-miniloan-004-ent-008-emi-amount` |
| field | ส่วนดอกเบี้ย | — | `ui-miniloan-004-ent-008-interest-portion` |
| field | ส่วนเงินต้น | — | `ui-miniloan-004-ent-008-principal-portion` |
| field | ยอดคงเหลือ | — | `ui-miniloan-004-ent-008-remaining-balance` |
| field | สถานะงวด | — | `ui-miniloan-004-ent-008-status` |

### โซน `schedule-summary` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | เงินต้นรวมทั้งตาราง | — | `ui-miniloan-004-ent-007-total-principal` |
| action | ขอยอดปิดบัญชีก่อนกำหนด | → หน้ายอดปิดบัญชีก่อนกำหนดของฉัน | `ui-miniloan-004-request-payoff-quote` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-004-ent-007-total-principal`
- `ui-miniloan-004-ent-008-due-date`
- `ui-miniloan-004-ent-008-emi-amount`
- `ui-miniloan-004-ent-008-installment-number`
- `ui-miniloan-004-ent-008-interest-portion`
- `ui-miniloan-004-ent-008-principal-portion`
- `ui-miniloan-004-ent-008-remaining-balance`
- `ui-miniloan-004-ent-008-status`
- `ui-miniloan-004-request-payoff-quote`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | ไม่เกิด | ไม่เกิด — เข้าหน้านี้ได้ต่อเมื่อบัญชีมีตารางผ่อนอยู่แล้วเสมอ |
| `loading` | เกิด | แสดง skeleton ของตารางระหว่างโหลด |
| `error` | เกิด | แสดงข้อความโหลดตารางไม่สำเร็จ พร้อมปุ่มลองใหม่ |
| `unauthorized` | เกิด | แสดงข้อความว่าไม่มีสิทธิ์ดูตารางผ่อนของบัญชีนี้ตาม BR-miniloan-033@v1 |
| `overflow` | เกิด | จำนวนงวดมากไม่มีการตัดหน้า เพราะจำนวนงวดสูงสุดมีเพดานจาก BR-miniloan-004@v1 อยู่แล้ว จึงเลื่อนดูในตารางเดียวได้เสมอ |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

