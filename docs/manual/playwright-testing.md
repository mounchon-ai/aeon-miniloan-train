# คู่มือเขียน UI Test ด้วย Playwright — miniloan

สรุปวิธีสั่งงาน เปิดหน้าเพื่อทดสอบ บันทึกเทสด้วยมือ (human-record) และจัดโครงสร้างไฟล์เทส
ทั้งหมดนี้เขียนขึ้นจากของจริงที่ทำในโปรเจกต์นี้ (branch `poc/dev-tester`) ไม่ใช่ทฤษฎีทั่วไป

## 1. สแตกที่ต้องมีก่อนรันเทส

เทสทุกตัวยิงใส่แอปที่ build จริงผ่าน docker compose เท่านั้น — ห้ามใช้ `ng serve` (พอร์ต 4200)
เพราะ Angular ฝัง API base URL ไว้ตอน build เวลาไม่ตรงพอร์ตจะยิง API ไม่ถูกเงียบๆ (ดู CLAUDE.md
หัวข้อ "Angular + Docker Compose gotcha")

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

| service | URL | ใช้ทำอะไร |
|---|---|---|
| `web` | `http://localhost:3000` | `baseURL` ของ playwright.config.ts |
| `api` | `http://localhost:5000` | เรียกตรงจาก `support/api.ts` เวลาต้อง setup ข้อมูล |
| `db` | `localhost:5432` | Postgres — ข้อมูลค้างข้าม run ถ้าไม่ล้างเอง |

## 2. โครงสร้างโปรเจกต์เทส

เก็บทุกอย่างไว้ใต้ `apps/web/e2e/` แยกเป็นสองชั้น: ไฟล์เทส (บนสุด) กับไฟล์ช่วย (`support/`)
ที่ไม่มี `test()` อยู่ในตัวเองสักบรรทัด

```
apps/web/
├─ playwright.config.ts       — baseURL, reporter, projects (chromium / demo)
└─ e2e/
   ├─ applicant-application.spec.ts        — SCN-001/004/006
   ├─ loan-officer-review.spec.ts          — SCN-016/021/023/033
   ├─ loan-account-downstream.spec.ts      — SCN-042/044/045/047
   ├─ assignment-queue-known-gap.spec.ts   — GAP-017/018 (ดูข้อ 7)
   ├─ role-switch-regression.spec.ts
   ├─ recorded-application-smoke.spec.ts   — human-record + data-driven
   └─ support/
      ├─ roles.ts                 — ROLE id, token, loginAs()
      ├─ api.ts                   — เรียก apps/api ตรง ใช้เป็น setup เท่านั้น
      └─ application-examples.ts  — ก้อนข้อมูลสำหรับ data-driven
```

**กติกาการตั้งชื่อ:** ชื่อเทสขึ้นต้นด้วย id จริงเสมอเมื่อมี — `SCN-miniloan-xxx` จาก
`.aeon/design/modules/miniloan/scenarios.json`. ถ้าไม่มี scenario รองรับ (เช่นเทส regression
หรือเทสยืนยันช่องว่างที่ design ยังไม่ตอบ) ให้บอกที่มาตรงๆ ในชื่อ/comment แทนที่จะเลี่ยงไปตั้งเลขเอง

## 3. เปิดหน้าในฐานะแต่ละบทบาท

โปรเจกต์นี้ยังไม่มีหน้า login จริง — auth เป็น token จำลองคงที่ต่อบทบาท
(`Authorization: Bearer <token>`) เก็บอยู่ที่ `current-role.service.ts` ฝั่งเว็บอ่านค่าจาก
`sessionStorage` คีย์ `miniloan.demo-role` ตอนสร้างคอมโพเนนต์ครั้งแรกเท่านั้น

| role id | บทบาท | mock token | หน้าแรกที่เห็น |
|---|---|---|---|
| `ROLE-001` | ผู้สมัคร | `mock-role-001` | `/applications` |
| `ROLE-002` | เจ้าหน้าที่สินเชื่อ | `mock-role-002` | `/dashboard` |
| `ROLE-003` | หัวหน้าเจ้าหน้าที่สินเชื่อ | `mock-role-003` | `/assignment-queue` |
| `ROLE-004` | ฝ่ายปฏิบัติการ | `mock-role-004` | `/loan-accounts` |
| `ROLE-005` | ผู้อนุมัติการปรับปรุงบัญชี | `mock-role-005` | ไม่มีหน้า entry — เข้าตรง `/adjustments` |

มีสองวิธีเปิดหน้าในฐานะบทบาทหนึ่ง แล้วแต่ว่ากำลังทดสอบอะไร:

**วิธีที่ 1 — ยัด sessionStorage เอง** (เร็ว ใช้ตอนไม่ได้ทดสอบตัวสลับบทบาท)

```ts
// goto('/') ก่อนเพื่อให้อยู่ origin เดียวกับที่ sessionStorage ผูกอยู่
await page.goto('/');
await page.evaluate(
  ({ key, id }) => sessionStorage.setItem(key, id),
  { key: 'miniloan.demo-role', id: 'ROLE-002' },
);
await page.goto('/dashboard'); // navigation ใหม่ = component สร้างใหม่ อ่านค่าที่เพิ่ง set
```

**วิธีที่ 2 — คลิกตัวสลับบทบาทจริงบนเชลล์** (ใช้เมื่อ "การสลับ" คือสิ่งที่ทดสอบ)

```ts
await page.locator('.app-nav__role-switcher select').selectOption('ROLE-002');
// onRoleChange() เขียน sessionStorage แล้วสั่ง location.reload() เอง — ไม่ต้อง goto ซ้ำ
```

## 4. คำสั่งที่ใช้บ่อย

| คำสั่ง | ใช้เมื่อ |
|---|---|
| `npm run e2e` | รันทั้งชุด headless เต็มความเร็ว (project `chromium`) |
| `npm run e2e:demo` | รันแบบเปิดเบราว์เซอร์จริง + ช้าลง 900ms/action ให้คนดูทัน (project `demo`) |
| `npx playwright test loan-officer-review` | รันเฉพาะไฟล์ที่ชื่อขึ้นต้นแบบนี้ |
| `npx playwright test -g "SCN-miniloan-016"` | รันเฉพาะเทสที่ชื่อมีคำนี้ ข้ามไฟล์ก็ได้ |
| `npx playwright test --debug` | เปิด Inspector หยุดทีละบรรทัด กด step เอง |
| `npx playwright show-report` | เปิดรายงาน HTML ของรันล่าสุด |

**ทำไมแยกเป็นสอง project แทนใช้ `--headed` เฉยๆ:** `playwright.config.ts` ประกาศ project
`demo` แยกจาก `chromium` — ตั้ง `headless: false` และ `slowMo: 900` ไว้ในตัวเอง คนอื่นไม่ต้องจำ
flag หรือ env var เอง แค่พิมพ์ `npm run e2e:demo` คำสั่งเดียว ส่วน `npm run e2e` ปกติ (ที่ CI ใช้)
ไม่ถูกกระทบเลยเพราะ pin ไว้ที่ `--project=chromium` ตายตัว อยากได้ความเร็วอื่นก็แก้ตัวเลข
`slowMo` ใน config ได้ตรงๆ หรือรันเฉพาะไฟล์เดียวแบบ `npm run e2e:demo -- recorded-application-smoke`

## 5. Human-record เทสด้วย codegen

เมื่ออยากลองคลิกเองแล้วให้ Playwright แปลงเป็นโค้ดให้ — ไม่ใช่เขียนเองทุกบรรทัด:

```bash
npx playwright codegen http://localhost:3000/applications/new \
  --output e2e/codegen-output.spec.ts
```

คำสั่งนี้เปิดหน้าต่างสองบาน (เบราว์เซอร์จริง + แผง Inspector) ให้คนคลิกเอง — เป็นเครื่องมือ GUI
ที่ต้องมีคนนั่งจับเมาส์ ไม่มีทางสั่งอัตโนมัติแทนได้

1. คลิก/พิมพ์ในเบราว์เซอร์ตามปกติ — Inspector แปลงเป็นโค้ดสดๆ ทางขวา
2. ปิดหน้าต่างเบราว์เซอร์เมื่อคลิกจบ — โค้ดจะถูกเซฟลงไฟล์ที่ระบุใน `--output` ทันที
3. เปิดไฟล์นั้นมา "ทำความสะอาด" เสมอ — ดูหัวข้อถัดไป

**สิ่งที่ codegen ให้มาไม่ครบ:**
- **ไม่มี `expect()` เลยสักบรรทัด** — มันบันทึกแค่ "action" ไม่รู้ว่าทำแล้วต้องเห็นผลอะไร ต้องเติมเอง
- มักมี click/dblclick/fill ซ้ำที่ไม่มีความหมาย (พิมพ์ผิดแล้วแก้ตอน record) — ตัดทิ้งให้เหลือ action
  ที่ตั้งใจจริง

## 6. ทำ data-driven test หลายเคสจากไฟล์เดียว

Playwright ไม่มี parametrize built-in แบบ pytest — ใช้ loop ธรรมดาสร้าง `test()` ทีละตัวจาก array
ข้อมูล แต่ละตัวจะขึ้นชื่อแยกกันในรายงานเอง

**แหล่งข้อมูล: ใช้ตัวอย่างที่ req รับรองไว้แล้ว ไม่เดาตัวเลขเอง** — `.aeon/req/requirements.json`
เก็บตัวอย่างจาก Example Mapping ไว้ (`EX-miniloan-xxx`) พร้อม given/when/then ที่ทีมตกลงกันแล้ว
ดึงมาใช้เป็น test data ตรงๆ แทนคิดค่าเอง

```ts
// e2e/support/application-examples.ts
// ตัวเลขมาจาก EX-miniloan-016 (requirements.json) — ชนขอบล่างพอดีทั้งสามเกณฑ์
{ exId: 'EX-miniloan-016', age: '20', monthlyIncome: '15000', currentEmploymentMonths: '4', ... }
```

**ใช้งาน: loop สร้างเทสทีละเคส**

```ts
// e2e/recorded-application-smoke.spec.ts
for (const example of ELIGIBLE_EXAMPLES) {
  test(`${example.exId}: ${example.label}`, async ({ page }) => {
    await fillAndSubmit(page, example.fields);
    await expect(page.getByTestId('ui-miniloan-003-ent-002-status'))
      .toHaveText(/Submitted|UnderReview/);
  });
}
```

**ก่อนเขียน assertion ของเคส "ควรพัง":** อย่าเดา assertion ของเคสที่ตกเกณฑ์ — รันจริงดูก่อนว่า
แอปตอบยังไง (เช่นเคสอายุ 19 ปี ไม่ได้ auto-reject แต่ได้ Band C ค้างรอเจ้าหน้าที่ตาม
SCN-miniloan-010) แล้วค่อยเขียน assertion ให้ตรงพฤติกรรมจริงนั้น

## 7. กับดักที่เจอจริงระหว่างเขียนเทสชุดนี้

**1 · ปุ่ม "ยื่นใบสมัคร" กดไม่ได้ตั้งแต่เปิดหน้า** — disabled จนกว่าจะกด "บันทึกร่าง" ก่อน
เพราะต้องมี id ของใบสมัครมาก่อนถึงจะยื่นได้ (โครงสร้าง API เอง) ไม่มีข้อความบอกเหตุผลบนหน้าจอเลย
— ปัญหานี้มีคนเปิด gap card ไว้แล้วคือ `GAP-miniloan-025`

**2 · ข้อความ "บันทึกการชำระเรียบร้อย" จับไม่ทันด้วย `expect()`** — กดบันทึกการชำระสำเร็จแล้ว
component จะยิง event ให้หน้าพ่อ reload ทันทีในรอบเดียวกัน ซึ่งไปสั่ง `loading = true` ทำให้
Angular unmount ฟอร์มลูกก่อนข้อความจะ render ทัน — ไม่ใช่บั๊ก flaky ของเทส แต่เป็นพฤติกรรมจริงของแอป
ให้ตรวจผลที่ persist แทน (แถวเปลี่ยนเป็น "จ่ายแล้ว", ยอดคงเหลือเปลี่ยน)

**3 · หน้าคิวมอบหมาย (`/assignment-queue`) กดปุ่มแล้วสำเร็จไม่ได้เลย** — ปุ่ม "มอบหมายให้ Loan
Officer" และ "ยกเลิกใบสมัคร" ส่งค่าว่างเสมอ เพราะหน้าจอไม่มีช่องให้เลือกเจ้าหน้าที่ และไม่มี
endpoint คืนรายชื่อให้เลือก (`GAP-miniloan-017`/`018` เปิดค้างกับ design อยู่) เทสที่ต้องใช้
ใบสมัคร "ถูกมอบหมายแล้ว" จึงเรียก API ตรงเป็น setup แทนการคลิกจริง — ดู `support/api.ts`

## 8. เช็กก่อน commit เทสใหม่

- ชื่อ testid มาจาก `.aeon/dev/features.json` (`manifest.ui_controls`) จริง ไม่ได้เดาจากหน้าตา
- รันซ้ำอย่างน้อย 2 รอบติดกัน — เทสยิงใส่ DB จริง ต้องไม่พังเพราะข้อมูล run ก่อนหน้าค้างอยู่
- เคสที่ผ่านและเคสที่ควรพัง ทั้งคู่ต้องรันแล้วดูผลจริงก่อนเขียน assertion ไม่ใช่เดาจากเอกสาร
- ถ้าเทสพึ่งพา endpoint ตรง (`support/api.ts`) ต้อง comment บอกว่าทำไม UI ถึงไปไม่ถึงจุดนั้นเอง

---

สรุปจาก session เขียน UI test อัตโนมัติของ miniloan · branch `poc/dev-tester` · Playwright 1.63.0
