# UI-miniloan-009 · แดชบอร์ดภาพรวมสถานะ

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-009`
- **จุดประสงค์:** Loan Officer ดูภาพรวมจำนวนใบสมัครและบัญชีสินเชื่อแยกตามสถานะ
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-002`
- **ชนิดของหน้า:** `form` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | แดชบอร์ดภาพรวมสถานะ | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `summary-counts` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | จำนวนใบสมัคร Submitted | — | `ui-miniloan-009-count-applications-submitted` |
| field | จำนวนใบสมัคร UnderReview | — | `ui-miniloan-009-count-applications-under-review` |
| field | จำนวนใบสมัคร Approved | — | `ui-miniloan-009-count-applications-approved` |
| field | จำนวนบัญชี Active | — | `ui-miniloan-009-count-accounts-active` |
| field | จำนวนบัญชี Closed | — | `ui-miniloan-009-count-accounts-closed` |
| action | รีเฟรชข้อมูล | → อยู่ที่หน้าเดิม | `ui-miniloan-009-refresh` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-009-count-accounts-active`
- `ui-miniloan-009-count-accounts-closed`
- `ui-miniloan-009-count-applications-approved`
- `ui-miniloan-009-count-applications-submitted`
- `ui-miniloan-009-count-applications-under-review`
- `ui-miniloan-009-refresh`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | เกิด | ยังไม่มีใบสมัครหรือบัญชีสินเชื่อเลยสักรายการ — ทุกยอดแสดงเป็นเลข 0 ตาม BR-miniloan-024@v1 |
| `loading` | เกิด | แสดง skeleton ของตัวเลขระหว่างโหลด |
| `error` | เกิด | แสดงข้อความโหลดแดชบอร์ดไม่สำเร็จ พร้อมปุ่มลองใหม่ |
| `unauthorized` | ไม่เกิด | ไม่เกิด — หน้านี้เปิดให้เฉพาะ Loan Officer ที่ล็อกอินอยู่แล้วเสมอ |
| `overflow` | ไม่เกิด | ไม่เกิด — จำนวนตัวเลขสรุปคงที่ที่ 5 ค่าตาม BR-miniloan-024@v1 |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

