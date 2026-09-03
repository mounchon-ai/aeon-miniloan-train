# UI-miniloan-003 · รายละเอียดใบสมัคร

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-003`
- **จุดประสงค์:** ให้ผู้สมัครดูสถานะ ผลประเมิน และเหตุผลของใบสมัครใบหนึ่งของตัวเอง
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-001`
- **ชนิดของหน้า:** `form` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | รายละเอียดใบสมัคร | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `application` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | ชื่อผู้สมัคร | — | `ui-miniloan-003-ent-002-name` |
| field | จำนวนเงินกู้ที่ขอ | — | `ui-miniloan-003-ent-001-requested-amount` |
| field | สถานะ | — | `ui-miniloan-003-ent-001-status` |
| field | เหตุผลที่ปฏิเสธ | — | `ui-miniloan-003-ent-001-rejection-reason` |
| field | เหตุผลที่ยกเลิก | — | `ui-miniloan-003-ent-001-cancellation-reason` |

### โซน `assessment` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | ผลจัดกลุ่มความเสี่ยง (Band) | — | `ui-miniloan-003-ent-004-band` |
| field | DTI | — | `ui-miniloan-003-ent-004-dti-percent` |
| field | วงเงินอนุมัติสูงสุด | — | `ui-miniloan-003-ent-004-max-approvable-amount` |

### โซน `account` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | สถานะบัญชีสินเชื่อ | — | `ui-miniloan-003-ent-007-status` |
| action | ดูตารางผ่อน | → ไปที่หน้าตารางผ่อน | `ui-miniloan-003-view-schedule` |
| action | ขอยอดปิดบัญชีก่อนกำหนด | → ไปที่หน้ายอดปิดบัญชีก่อนกำหนด | `ui-miniloan-003-view-closure-quote` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-003-ent-001-cancellation-reason`
- `ui-miniloan-003-ent-001-rejection-reason`
- `ui-miniloan-003-ent-001-requested-amount`
- `ui-miniloan-003-ent-001-status`
- `ui-miniloan-003-ent-002-name`
- `ui-miniloan-003-ent-004-band`
- `ui-miniloan-003-ent-004-dti-percent`
- `ui-miniloan-003-ent-004-max-approvable-amount`
- `ui-miniloan-003-ent-007-status`
- `ui-miniloan-003-view-closure-quote`
- `ui-miniloan-003-view-schedule`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | ไม่เกิด | ไม่เกิด — เข้าถึงหน้านี้ได้จากรายการเท่านั้น จึงมีใบสมัครอยู่เสมอ |
| `loading` | เกิด | แสดง skeleton/spinner ทับตำแหน่งข้อมูล ปุ่มที่ยิง action ถูกปิดใช้งานชั่วคราวระหว่างรอ |
| `error` | เกิด | แสดงข้อความ "เกิดข้อผิดพลาด ลองใหม่อีกครั้ง" พร้อมปุ่มลองใหม่ ข้อมูลเดิมที่เคยโหลดสำเร็จยังคงอยู่บนจอ ไม่หายไป |
| `unauthorized` | เกิด | แสดงข้อความ "คุณไม่มีสิทธิ์ดูใบสมัครนี้" แทนข้อมูล — ครอบคลุมกรณีพยายามเปิดใบของคนอื่นด้วย id ตรง |
| `overflow` | ไม่เกิด | ไม่เกิด — ข้อมูลใบสมัครหนึ่งใบมีขนาดคงที่ |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

