# UI-miniloan-012 · รายละเอียดบัญชีสินเชื่อ

> ไฟล์นี้ generate จาก wireframe — **ห้ามแก้ด้วยมือ** แก้ที่ต้นทางแล้วสร้างใหม่ (ประตู 23)
> วางข้อความทั้งไฟล์นี้ให้เครื่องมือออกแบบ แล้วเอาผลลัพธ์กลับมาที่ `<state-dir>/mockup/handoff/<slug>/`

## 1 · บริบทของหน้า

- **หน้าจอ:** `UI-miniloan-012`
- **จุดประสงค์:** Operations ดูรายละเอียดบัญชี บันทึกการชำระ ออกตารางผ่อนใหม่ทับ หรือยื่นคำขอปรับปรุงบัญชีที่ปิดแล้ว
- **ผู้ใช้ที่เห็นหน้านี้:** `ROLE-004`
- **ชนิดของหน้า:** `form` · **ระดับที่มีอยู่แล้ว:** `L1`

## 2 · สิ่งที่ต้องมี

### โซน `header` (header)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| title | รายละเอียดบัญชีสินเชื่อ | — | — ยังไม่มีใครตั้งชื่อ |

### โซน `account-summary` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | เงินต้นตั้งต้น | — | `ui-miniloan-012-ent-006-principal-amount` |
| field | เงินต้นคงเหลือ | — | `ui-miniloan-012-ent-006-outstanding-principal` |
| field | จำนวนงวดทั้งหมด | — | `ui-miniloan-012-ent-006-term-months` |
| field | สถานะบัญชี | — | `ui-miniloan-012-ent-006-status` |
| field | วันที่เบิกจ่าย | — | `ui-miniloan-012-ent-006-disbursed-at` |
| field | วันที่ปิดบัญชี | — | `ui-miniloan-012-ent-006-closed-at` |
| action | ออกตารางผ่อนฉบับใหม่ทับ | → อยู่ที่หน้าเดิม แสดงตารางผ่อนฉบับใหม่ | `ui-miniloan-012-reschedule` |

### โซน `schedule-table` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | งวดที่ | — | `ui-miniloan-012-ent-008-installment-number` |
| field | วันครบกำหนด | — | `ui-miniloan-012-ent-008-due-date` |
| field | ค่างวด (EMI) | — | `ui-miniloan-012-ent-008-emi-amount` |
| field | สถานะงวด | — | `ui-miniloan-012-ent-008-status` |

### โซน `payment-form` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | ยอดชำระ | — | `ui-miniloan-012-ent-009-amount` |
| action | บันทึกการชำระ | → อยู่ที่หน้าเดิม แสดงยอดที่อัปเดต | `ui-miniloan-012-record-payment` |
| action | บันทึกการชำระยอดปิดบัญชีก่อนกำหนด | → อยู่ที่หน้าเดิม แสดงสถานะบัญชีที่ปิดแล้ว | `ui-miniloan-012-record-payoff-payment` |

### โซน `adjustment-form` (form)

| ชนิด | ข้อความ | ปลายทาง | `data-testid` |
|---|---|---|---|
| field | ชื่อฟิลด์ที่ขอแก้ | — | `ui-miniloan-012-ent-010-field-name` |
| field | ค่าเดิม | — | `ui-miniloan-012-ent-010-old-value` |
| field | ค่าใหม่ | — | `ui-miniloan-012-ent-010-new-value` |
| action | ยื่นคำขอปรับปรุงบัญชีที่ปิดแล้ว | → อยู่ที่หน้าเดิม แสดงสถานะคำขอที่รออนุมัติ | `ui-miniloan-012-submit-adjustment` |

**ทุก `data-testid` ข้างล่างนี้ต้องกลับมาครบในหน้าที่ส่งกลับ** — id พวกนี้เปลี่ยนไม่ได้หลังเกิด (ประตู 37) และ `qa` ใช้มันเขียนเทส

- `ui-miniloan-012-ent-006-closed-at`
- `ui-miniloan-012-ent-006-disbursed-at`
- `ui-miniloan-012-ent-006-outstanding-principal`
- `ui-miniloan-012-ent-006-principal-amount`
- `ui-miniloan-012-ent-006-status`
- `ui-miniloan-012-ent-006-term-months`
- `ui-miniloan-012-ent-008-due-date`
- `ui-miniloan-012-ent-008-emi-amount`
- `ui-miniloan-012-ent-008-installment-number`
- `ui-miniloan-012-ent-008-status`
- `ui-miniloan-012-ent-009-amount`
- `ui-miniloan-012-ent-010-field-name`
- `ui-miniloan-012-ent-010-new-value`
- `ui-miniloan-012-ent-010-old-value`
- `ui-miniloan-012-record-payment`
- `ui-miniloan-012-record-payoff-payment`
- `ui-miniloan-012-reschedule`
- `ui-miniloan-012-submit-adjustment`

## 3 · ห้าสถานะ

| สถานะ | เกิดกับหน้านี้ไหม | ข้อความที่ต้องแสดง |
|---|---|---|
| `empty` | ไม่เกิด | ไม่เกิด — เข้าหน้านี้ได้ต่อเมื่อมีบัญชีสินเชื่ออยู่แล้วเท่านั้น |
| `loading` | เกิด | ระหว่างบันทึกการชำระหรือออกตารางใหม่ ปุ่มที่กดถูก disable และแสดง spinner ป้องกันกดซ้ำ |
| `error` | เกิด | แสดงข้อความ error ตามเหตุ เช่น ชำระไม่ครบยอดงวด (BR-miniloan-019@v1) หรือพยายามออกตารางทับบัญชีที่ปิดแล้ว (BR-miniloan-045@v1) |
| `unauthorized` | เกิด | แสดงข้อความว่าไม่มีสิทธิ์ดำเนินการกับบัญชีนี้ เมื่อ Operations ที่ไม่ได้ถูก assign พยายามเข้าถึงตาม BR-miniloan-054@v1 |
| `overflow` | เกิด | จำนวนงวดในตารางผ่อนมากไม่มีการตัดหน้า เพราะจำนวนงวดสูงสุดมีเพดานจาก BR-miniloan-004@v1 อยู่แล้ว จึงเลื่อนดูในตารางเดียวได้เสมอ |

_ข้อความในตารางนี้คัดมาคำต่อคำจากที่ design ประกาศไว้ **ห้ามเขียนใหม่ให้สวยขึ้น**_

## 4 · กติกา UI ที่บังคับ

- **UIC-001** — ปุ่มประจำแถวอยู่คอลัมน์แรกของแถว เรียงลำดับตายตัว: view · edit · delete · manage · command
  - ใช้กับ: ทุกหน้าที่แสดงเป็นรายการแถว — ปุ่มประจำแถวทุกปุ่ม (ชนิด view · edit · delete · manage · command — ชนิดเป็นของ design ตั้งแต่ประตู 35)
- **UIC-002** — แสดงเป็นไอคอน ไม่ใช่ปุ่มตัวหนังสือ และใช้ชื่อ action ที่ design ประกาศไว้ใน actions[].name เป็นชื่อสำหรับ screen reader
  - ใช้กับ: ปุ่มประจำแถวทุกปุ่ม
- **UIC-003** — เปิดเป็น modal ทับรายการ ไม่พาไปหน้ารายละเอียดแยก
  - ใช้กับ: ปุ่มประจำแถวชนิด view

