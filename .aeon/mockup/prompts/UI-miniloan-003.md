# UI-miniloan-003 · รายละเอียดใบสมัครของฉัน

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-003`
- **จุดประสงค์:** ผู้สมัครดูสถานะและผลการประเมินของใบสมัครหนึ่งใบ
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-001`
- **ชนิดของหน้า:** `form` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | รายละเอียดใบสมัครของฉัน | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `application-summary` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | สถานะ | — | `ui-miniloan-003-ent-002-status` |
| field | จำนวนเงินกู้ที่ขอ | — | `ui-miniloan-003-ent-002-requested-amount` |
| field | จำนวนงวดที่ขอ | — | `ui-miniloan-003-ent-002-requested-term-months` |
| field | วันที่ยื่น | — | `ui-miniloan-003-ent-002-submitted-at` |
| action | กลับไปที่รายการ | → หน้ารายการใบสมัครและบัญชีของฉัน | `ui-miniloan-003-back-to-list` |

### โซน `assessment-result` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | Credit Band | — | `ui-miniloan-003-ent-003-band` |
| field | วงเงินอนุมัติสูงสุด | — | `ui-miniloan-003-ent-003-max-approvable-amount` |
| field | เหตุผลผลการประเมิน | — | `ui-miniloan-003-ent-003-reasons` |
| field | เหตุผลที่ปฏิเสธ | — | `ui-miniloan-003-ent-002-rejection-reason` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-003-back-to-list`
- `ui-miniloan-003-ent-002-rejection-reason`
- `ui-miniloan-003-ent-002-requested-amount`
- `ui-miniloan-003-ent-002-requested-term-months`
- `ui-miniloan-003-ent-002-status`
- `ui-miniloan-003-ent-002-submitted-at`
- `ui-miniloan-003-ent-003-band`
- `ui-miniloan-003-ent-003-max-approvable-amount`
- `ui-miniloan-003-ent-003-reasons`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | ไม่เกิด | ไม่เกิด — เข้าหน้านี้ได้ต่อเมื่อมีใบสมัครอยู่แล้วเท่านั้น |
| `loading` | เกิด | แสดง skeleton ระหว่างโหลดรายละเอียด |
| `error` | เกิด | แสดงข้อความโหลดไม่สำเร็จ พร้อมปุ่มลองใหม่ |
| `unauthorized` | เกิด | แสดงข้อความว่าไม่มีสิทธิ์ดูใบสมัครนี้ เมื่อพยายามเปิดใบสมัครของผู้อื่นตาม BR-miniloan-033@v1 |
| `overflow` | ไม่เกิด | ไม่เกิด — เนื้อหาหน้าคงที่ ไม่มีรายการที่ล้น |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

