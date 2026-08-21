# รายงานตรวจผลการใช้งาน `req` plugin ในโปรเจกต์สนาม miniloan — รอบที่ 1 (v1)

> **owner:** user · **วันที่ตรวจ:** 2026-08-16 · **ผู้ตรวจ:** Claude (ตามคำสั่งเจ้าของ ก่อนเริ่มขั้น design)
> **ขอบเขต:** ตรวจว่า command ทำงานตรงตามแบบ + เก็บข้อมูลได้ครบ · **การตรวจเป็น read-only ทั้งหมด** —
> ไฟล์เดียวที่ถูกเขียนในโปรเจกต์นี้คือรายงานฉบับนี้ · ตัวเลขทุกตัวมาจากสคริปต์ (คำสั่งซ้ำได้อยู่ §9)
> รอบถัดไป (หลังแก้รายการ §7) ให้เขียนเป็น `req-field-review-v2.md` — **ห้ามแก้ทับฉบับนี้**

---

## 1. บทสรุป

**คำสั่งทั้ง 8 ของ req ทำงานตรงตามแบบของเวอร์ชันที่ติดตั้ง (v0.3.0) และข้อมูลที่เก็บครบตามสัญญา schema 0.3.0** —
spec ผ่าน ajv ของ schema 0.3.0 · bundle ตรง spec เป๊ะ · rollup ตรง · hash ต้นทางยังตรง · golden script รันได้จริง

แต่มี**ความจริงสามข้อที่ต้องรู้ก่อนเข้า design:**

1. **สนามนี้ยังไม่เคยทดสอบ v0.4.x เลย** — plugin ที่ติดตั้งถูก pin ไว้ที่ 0.3.0 ตั้งแต่วันติดตั้ง (§2)
   ฟีเจอร์ชุด v0.4 (`ask_round{}` · `answer_source` ⭐/✍️/🛑 · `/req:ask resolve` · `coverage.mjs` · check #15)
   **ไม่มีข้อมูลสนามพิสูจน์แม้แต่ตัวเดียว**
2. **spec สนามตกด่าน ajv ของ plugin ปัจจุบัน** (`schema_version` 0.3.0 ≠ const 0.4.1) และ**ยังไม่มีเครื่องมือ migrate**
   — ถ้าเริ่ม design ทั้งอย่างนี้ นิยาม "upstream clean" ของ design (ด่าน req ผ่าน) จะแดงตั้งแต่ยังไม่เริ่ม
3. **รอบการ์ดแดงรอบสุดท้ายค้างครึ่งทาง** — คำตอบ Q-015/Q-016 ถูกจดลง SRC-017 แล้ว (⭐ ทั้งสาม)
   แต่ตัวคำถามยัง `open` และ BR-053 ที่เกิดจากคำตอบยังไม่มีตัวอย่าง — **ด่านจับได้ครบทุกใบ ซึ่งเป็นข่าวดี**

ด่านปัจจุบัน (v0.4.1) รันทับข้อมูลสนามได้: **13 error / 4 warn** — ทุกใบสาวกลับหาโหนดจริงได้ ไม่มี false positive (§3)

---

## 2. สภาพแวดล้อมที่ตรวจเจอ — สนามรันด้วยของสองยุคปนกัน

| ของ | ค่าที่เจอ | หลักฐาน |
|---|---|---|
| plugin ที่ติดตั้ง | **`req@aeon` v0.3.0** · scope `project` (โปรเจกต์นี้) · ติดตั้ง 2026-08-14T10:57+07:00 | `installed_plugins.json` → cache `aeon/req/0.3.0` |
| `.aeon/spec.json` | `meta.schema_version: "0.3.0"` · scale FULL · สร้าง 08-14 11:21 · แตะล่าสุด 08-15 15:18 | อ่านจากไฟล์ |
| bundle เก่า `docs/wiki/*` (163 หน้า) | render โดย plugin 0.3.0 (WIKI_DIR เก่า = `docs/wiki`) — **ค้างเป็น snapshot กลางทาง เนื้อหาไม่ตรงปัจจุบัน** | diff กับหน้าเดียวกันใน `req/` = ต่างกัน |
| bundle ใหม่ `docs/wiki/req/**` (293 หน้า) | render โดย `wiki.mjs` **ตัวปัจจุบันจากฝั่ง marketplace** — ตรง spec เป๊ะ (dry-run: already matches) | §3 แถว wiki |
| `schemas/spec.schema.json` ที่ root | สำเนา schema 0.3.0 ตาม flow เก่า (check ยุค 0.3 สั่ง copy) — **ตำแหน่งปัจจุบันไม่ใช้แล้ว** (schema เดินทางกับ plugin) | `check.md` ใน cache 0.3.0 |

ข้อสังเกตเชิงระบบ: `/plugin install` **pin เวอร์ชันตอนติดตั้ง** — การที่ marketplace ขยับเป็น 0.4.1 ไม่ทำให้
โปรเจกต์ที่ติดตั้งแล้วได้ของใหม่เอง ต้องสั่ง update/reinstall เอง (ไม่ใช่บั๊กของ req แต่เป็นพฤติกรรมที่สนามต้องรู้)

---

## 3. ตัวเลขด่านทั้งหมด (รันด้วยสคริปต์ปัจจุบันของ marketplace · 2026-08-16)

| ด่าน | ผล | exit |
|---|---|---|
| `verify-rules.mjs` (ALL) | **13 error / 4 warn** — #2 ×1 (BR-053 ไม่มี EX) · #3 ×2 (Q-015/Q-016 การ์ดแดงเปิดค้าง) · #5 ×10 (DQ spillover ทั้งคิว — ของ Phase 2 โดยธรรมชาติ) · #13 warn ×4 (BR-002/003/022/050 kind=calculation ยังไม่มี GD) | 1 |
| `verify-rules.mjs --cp1` | 3 error / 4 warn (ตัด #5 ออกตาม gate) | 1 |
| `verify-rules.mjs --cp2` | 10 error / 0 warn (#5 ล้วน) | 1 |
| `wiki.mjs` (dry-run) | `bundle already matches spec` · 293 หน้า @ `docs/wiki/req` | 0 |
| `rollup.mjs` | `already correct` (53 current · example 52 · coverage 98% · green_test 0*) | 0 |
| `coverage.mjs` | **0 round** — tier1 0/4 · tier2 0/10 (ตาบอดโดยเหตุ §5 ข้อ 3 ไม่ใช่เพราะไม่ได้ถาม) | 0 |
| ajv vs สำเนา schema 0.3.0 (ที่ root โปรเจกต์) | **valid** | 0 |
| ajv vs schema ปัจจุบัน 0.4.1 (ใน plugin) | **invalid** — `schema_version` must be equal to constant `0.4.1` | 1 |
| `.aeon/golden/CALC-miniloan-001@v1.mjs` | รันได้จริง พิมพ์ตารางผ่อนครบทุก case | 0 |
| ฝั่ง marketplace (เกณฑ์ ง) | `verify-rules` fixture clean = PASS · `verify-design.mjs --root .` = PASS — **ตัวเลขสัญญาไม่ขยับ** | 0 |

\* `rules_with_green_test = 0` **ถูกต้องโดยแบบ** — นับจาก `traces_down.test_cases` ซึ่งเป็นของ Phase 4/5 ที่ยังไม่เกิด ไม่เกี่ยวกับ GD

---

## 4. ตรวจรายคำสั่ง — ทำงานตรงไหม

| คำสั่ง | ผล | หลักฐานในข้อมูล |
|---|---|---|
| `/req:capture` | ✅ ตรง | SRC-001 (kind=file) มี `hash_at_import` และ**วันนี้ hash ยังตรงกับไฟล์ต้นทางเป๊ะ** · แตกเป็น REQ-001..006 ที่ `docs/requirements/` พร้อมหัว GENERATED + spec-hash |
| `/req:ask` | ✅ ตรงตามแบบ 0.3 | 16 คำถาม ตอบแล้ว 14 · ทุกรอบถูกจดเป็น SRC kind=chat (SRC-002..015, 017) ระบุใน `captured_by` ว่ารอบไหนตอบใบไหน · ชั้นคลังคำถามถูกใช้ 1 รอบ (SRC-014 · language ×3) |
| `/req:example` | ✅ ตรง | 144 EX ครอบ 52/53 กฎ current — ขาดเฉพาะ BR-053 ที่เพิ่งเกิดจากรอบค้าง |
| `/req:calc` | ✅ ตรง | CALC-001 สัญญาครบทุกช่องที่แบบเรียกร้อง: `rounding_mode` HALF_UP · working scale 30 · `residual_policy` · `boundary_behavior` · `constrains` BR-016 |
| `/req:golden` | ✅ ตรง **และคุ้มค่าที่สุด** | GD-001 `validated` · 7 แถว · `mismatches: []` · verified_by เจ้าของจริง · **การรันเลขจริงทำให้ Q-016 โผล่** (คอลัมน์แสดง vs บัญชีภายใน ต่างกันระดับสตางค์–0.17 บาท) — นี่คือของที่อ่านเอกสารเฉย ๆ ไม่มีวันเห็น |
| `/req:change` | ✅ ตรง | CHG-001 ครบวงจร: เหตุผล · `approved_by` · `triggered_by` SRC-013/015 → BR-031@v1 `superseded` + BR-031@v2 เกิด — **ทางเดียวที่ผลิต @v2 ทำงานจริง** |
| `/req:check` | ✅ ตรง | ด่านชี้ใบจริงทุกใบ ไม่มีใบหลอน (ตรวจสอบแล้วทั้ง 13+4 ใบ) |
| `/req:help` | — | ไม่มีร่องรอยในข้อมูล (คำสั่งอ่านอย่างเดียว ไม่มีอะไรให้ตรวจ) |
| **ชุด v0.4**: `ask_round{}` · `answer_source` · `resolve` · `coverage` · check #15 | ⛔ **ยังไม่เคยลงสนาม** | ติดตั้งเป็น 0.3.0 ทั้งเส้น (§2) — SRC-017 จดไว้เองตรง ๆ ว่า *"สเปกนี้อยู่ที่ schema 0.3.0 ซึ่งยังไม่มีช่องนั้น"* |

## 5. ความครบของข้อมูล

**คลังข้อมูลใน spec:** sources 17 (file 1 · chat 16) · glossary 25 · requirements 6 · rules 54
(current 53 = constraint 11 · calculation 5 · policy 18 · invariant 19 · superseded 1) · calculations 1 ·
golden 1 · examples 144 · questions 16 (answered 14 · open 2) · deferred_questions 10 (open ทั้งหมด) · changes 1

ข้อค้นพบด้านความครบ:

1. **ครบตามสัญญา 0.3.0** — ajv ผ่าน · ทุก join ที่ด่านตรวจ (EX↔BR · GD↔CALC↔BR · CHG↔BR@v2 · SRC↔ไฟล์ต้นทาง) สาวได้จริง
2. **ไม่ครบตามสัญญา 0.4.1 และถอยหลังเติมไม่ได้อย่างซื่อสัตย์** — รอบถาม 15 รอบเป็นร้อยแก้วใน SRC
   ไม่มี `ask_round{}`/`answer_source` แบบ structured · การ migrate ย้อนหลังทำได้แค่ระดับ "จดว่าไม่มี" ห้ามประดิษฐ์ข้อมูลว่ารอบไหนรับดาว
3. **ผลข้างเคียงที่มองไม่เห็นถ้าไม่รู้ข้อ 2:** `coverage.mjs` รายงาน 0/4 · 0/10 ทั้งที่สนามถามจริง 16 คำถาม
   (SRC-014 คือรอบ language ของ tier 1 ที่ reporter มองไม่เห็น) และ **check #15 ผ่านแบบจักรวาลว่าง** — ไม่ใช่ผ่านเพราะข้อมูลดี
4. **งานค้างหนึ่งรอบ** — SRC-017 (08-15 15:03) จดคำตอบการ์ดแดง Q-015/Q-016 ครบ (⭐×3) และ BR-052 ได้ EX-117..119 /
   BR-053 เกิดแล้ว แต่ **Q ทั้งสองใบยัง `state: open` และ BR-053 `examples: []`** — วงจรเขียนกลับไม่จบ session
5. **DQ 10 ใบคือวัตถุดิบพร้อมใช้ของ design** — คำถามคุณภาพสูงทั้งคิว (decimal precision · ขอบเขต transaction ·
   idempotency key · สิทธิ์การเห็นข้อมูล ฯลฯ) · หมายเหตุ: ป้ายปลายทางเขียนว่า `/domain:ask` · `/screen:ask`
   (ชื่อ roster ยุค 0.3) — ผู้รับจริงปัจจุบันคือ `/design:data` → `resolves[]` → `/req:ask resolve` · ป้ายเก่าไม่ขวางการทำงาน แต่หลอกคนอ่าน

---

## 6. สิ่งที่สนามสอนกลับไปที่ฝั่ง marketplace (ของเจ้าของ ไม่ใช่ของโปรเจกต์นี้)

| # | บทเรียน | ควรไปลงที่ไหน |
|---|---|---|
| 1 | **ไม่มีเส้นทาง migrate schema 0.3→0.4** — โปรเจกต์สนามแรกของจริงติดกับดักนี้ทันที · plugin v0.4 เจอ spec 0.3 แล้ว degrade เป็นร้อยแก้ว (ซื่อสัตย์ดี) แต่ไม่มีใครบอกทางไปต่อ | ประตูใหม่ของเจ้าของ: ทำ migration tool หรือประกาศ "โปรเจกต์เก่าจบที่เวอร์ชันเดิม" |
| 2 | **การย้าย WIKI_DIR (งาน 19 ⑤) ทิ้ง bundle เก่าไว้เงียบ ๆ** — 163 หน้า stale อยู่นอกสายตาทุกด่าน (ไม่ ORPHAN เพราะอยู่นอก WIKI_DIR) — คนเปิด `docs/wiki/rules/` จะอ่านของเก่าโดยไม่รู้ตัว | จดใน field-test-log · อาจเพิ่มคำเตือนใน USER-GUIDE ของ req เรื่องอัปเกรดข้ามตำแหน่ง bundle |
| 3 | **`/plugin install` pin เวอร์ชัน** — สนามคิดว่าใช้ของใหม่แต่จริง ๆ รัน 0.3.0 ทั้งเส้น | คำเตือนหนึ่งบรรทัดใน USER-GUIDE: อัปเกรด plugin ต้องสั่งเอง แล้วตรวจด้วย `/req:check` |
| 4 | **เกณฑ์ ก ของ field test มีหลักฐานจริงแล้ว**: `/req:golden` ทำให้ Q-016 โผล่ (เงินจริงระดับสตางค์–0.17 บาท/สัญญา) และการไล่กฎชนกันทำให้ Q-015 โผล่ (ช่องเงินต้นติดลบ) — *"ของจริงที่เอกสารทั่วไปไม่มีวันเห็น"* เกิดขึ้นตามที่แบบสัญญา | เจ้าของถอดลง `docs/field-test-log.md` (มือเขียนตามกติกาไฟล์นั้น) — ร่างอยู่ §8 |
| 5 | เกณฑ์ **ข** (คนนอกเล่าคืน) และ **ค** (วัด token จริง) **ยังไม่ได้ทำ** — ย้อนวัดไม่ได้แล้วสำหรับรอบนี้ ต้องวัดในรอบ v0.4.1 | ทำในรอบสนามถัดไป |

---

## 7. ต้องทำก่อนเริ่ม design phase — เรียงลำดับแล้ว

1. **อัปเดต plugin เป็น v0.4.1** ในโปรเจกต์นี้ (uninstall/reinstall หรือ plugin update) — ก่อนแตะอย่างอื่น
2. **เคาะประตู migration กับเจ้าของ**: ยก spec เป็น 0.4.1 อย่างซื่อสัตย์ —
   บั๊ม `schema_version` + เติมช่องที่ schema บังคับ · รอบถามเก่าจดตามจริงว่าเป็นร้อยแก้ว (ห้ามประดิษฐ์ `ask_round{}` ย้อนหลัง) ·
   เสร็จแล้ว ajv กับด่านต้องเขียวก่อนไปข้อถัดไป
3. **ปิดรอบการ์ดแดงที่ค้าง**: เดิน `/req:ask` ให้จบตามคำตอบที่จดไว้แล้วใน SRC-017 —
   Q-015/Q-016 → answered · ตรวจว่า BR-052/BR-053 สะท้อนคำตอบครบ · เติม EX ให้ BR-053 (`/req:example`)
   → error #2/#3 ต้องเหลือ 0
4. **ลบของตกค้างสองก้อน** (งานมือ เจ้าของยืนยันก่อนลบ): bundle เก่า `docs/wiki/*` ทุกโฟลเดอร์**ยกเว้น `req/`**
   (examples · glossary · nfr · questions · requirements · rules · sources + `BUNDLE.md` ราก) และ `schemas/` ที่ root
5. *(แนะนำ ไม่บังคับ)* เติม GD ให้กฎ calculation อีก 4 ใบ (BR-002/003/022/050) — ปิด warn #13 ให้เหลือ 0
6. แล้วจึงเริ่ม design — คิว DQ 10 ใบพร้อมเป็นอินพุตของ `/design:data` · จุดนี้ควรเริ่ม**วัด token จริง** (เกณฑ์ ค)

หลังทำข้อ 1–4 ครบ รัน `/req:check` ใหม่: ต้องเหลือ **0 error (CP1) · #5 ×10 ยังอยู่ตาม gate CP2 จนกว่า design จะปิด DQ** — แล้วบันทึกเป็น `req-field-review-v2.md`

---

## 8. ร่างสำหรับเจ้าของถอดลง `field-test-log.md` (ฝั่ง marketplace · มือเขียน)

> 2026-08-14..15 · โปรเจกต์ `aeon-miniloan` (repo แยก · ติดตั้ง `req@aeon` v0.3.0) เดินทั้งเส้น capture→ask→example→calc→golden→change→check
> กับ requirement จริง (MiniLoan) · เกณฑ์ **ก ผ่าน**: `/req:golden` ทำให้คำถามที่ไม่มีใครเคยถามโผล่จริง 2 ใบ (Q-015 เงินต้นติดลบ · Q-016 สองนิยามของ "เงินต้นคงเหลือ" ต่างกัน 0.17 บาท/สัญญา)
> · เกณฑ์ **ข/ค ยังไม่ได้วัด** · เกณฑ์ **ง ผ่าน** (ตัวเลข marketplace นิ่งครบ ตรวจ 2026-08-16)
> · บทเรียน: ไม่มีทาง migrate 0.3→0.4 / bundle เก่าค้างหลังย้าย WIKI_DIR / install pin เวอร์ชัน — รายละเอียดที่ `aeon-miniloan/docs/req-field-review-v1.md`
> · **v0.4.1 ยังไม่เคยลงสนาม** — ห้ามนับรอบนี้เป็นการพิสูจน์ v0.4

---

## 9. คำสั่งตรวจซ้ำ (รันจากราก `aeon-marketplace` · ตัวเลขในรายงานนี้มาจากชุดนี้)

```bash
ML="D:\ProjectClaude\AEON-Work\aeon-miniloan"
node plugins/req/scripts/verify-rules.mjs --root "$ML"          # exit 1 · 13 error / 4 warn
node plugins/req/scripts/verify-rules.mjs --root "$ML" --cp1    # exit 1 · 3 / 4
node plugins/req/scripts/verify-rules.mjs --root "$ML" --cp2    # exit 1 · 10 / 0
node plugins/req/scripts/wiki.mjs   --root "$ML"                # exit 0 · already matches · 293 pages @ docs/wiki/req
node plugins/req/scripts/rollup.mjs --root "$ML"                # exit 0 · already correct
node plugins/req/scripts/coverage.mjs --root "$ML"              # exit 0 · 0 round (ตาบอดบนข้อมูล 0.3 — §5 ข้อ 3)
npx --yes ajv-cli@5 validate -s "$ML/schemas/spec.schema.json" -d "$ML/.aeon/spec.json" --spec=draft2020 --strict=false   # valid (0.3.0)
npx --yes ajv-cli@5 validate -s plugins/req/schemas/spec.schema.json -d "$ML/.aeon/spec.json" --spec=draft2020 --strict=false   # invalid (const 0.4.1)
node "$ML/.aeon/golden/CALC-miniloan-001@v1.mjs"                # exit 0 · ตารางผ่อนครบ
```
