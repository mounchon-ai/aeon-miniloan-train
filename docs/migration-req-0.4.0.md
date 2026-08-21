# บันทึกการย้ายโครงสร้าง miniloan → req plugin 0.4.0 (สาย aeon-marketplace-train)

> **owner:** user · **วันที่ทำ:** 2026-08-21 · **ผู้ทำ:** Claude (ตามคำสั่งเจ้าของ)
> **ต้นทางที่ใช้เทียบ:** `D:\ProjectClaude\AEON-Work\aeon-marketplace-train` — req 0.4.0 · design 0.10.0
> **เจ้าของยืนยันเลือกสายนี้เอง** หลังได้รับรายงาน §5 (มี marketplace อีกชุดที่ติดตั้งจริงและขัดกัน)
> **สำรองก่อนแก้:**
> `C:\Users\mounc\AppData\Local\Temp\claude\D--ProjectClaude-AEON-Work-aeon-miniloan-upd\9a4b9fc5-9aa4-4709-bce2-2c4283191c66\scratchpad\backup-miniloan-upd`
> 483 ไฟล์ ตรงกับต้นฉบับทั้งจำนวน · **backup อยู่ใน scratchpad ของ session นี้เท่านั้น จะหายเมื่อ session จบ**
> โปรเจกต์นี้ **ยังไม่ใช่ git repo** — ถ้าจะทำงานต่อ ควร `git init` ก่อนอย่างอื่น

## 1. ตัดสินอะไร และเพราะอะไร

โปรเจกต์นี้มี wiki bundle **สองชุดซ้อนกัน** — `docs/wiki/**` (แบน) กับ `docs/wiki/req/**` (ซ้อนใต้ `req/`)
เมื่อยึด `aeon-marketplace-train` ชุดที่ถูกคือ **แบน** เพราะสามอย่างนี้ตรงกันหมด:

| หลักฐาน | บอกอะไร |
|---|---|
| `wiki.mjs` บรรทัด 34 → `export const WIKI_DIR = "docs/wiki"` | ตัว render เขียนลง `docs/wiki` ตรงๆ |
| `verify-rules.mjs` เช็ค 12 | รายงานทุกหน้าใต้ `docs/wiki/req/**` ว่าเป็น **orphan page** (294 หน้า) |
| ธง `--out` ใช้ไม่ได้จริง | `wiki.mjs` บรรทัด 739 อ่าน `values["--out"]` แต่ `state-dir.mjs` ไม่ได้ใส่ `--out` ไว้ใน `VALUE_FLAGS` → สั่งแล้วได้ exit 2 |

**ก่อนลบ `docs/wiki/req/` เทียบ byte กับชุดแบนที่ generate ใหม่แล้ว** — ต่างกัน **9 ไฟล์** และทุกจุดที่ต่าง
เป็นผลของความลึก path เท่านั้น (`../../../../docs/...` เทียบกับ `../../docs/...`), หัวตาราง `BUNDLE.md`,
และเลข schema · ไม่มีไฟล์ที่มีอยู่ข้างเดียว · `log.md` ทั้งสองชุดเหมือนกันทุก byte จึงไม่มีประวัติสูญ

## 2. ทำอะไรไปบ้าง

| # | รายการ | ผลลัพธ์ |
|---|---|---|
| 1 | `schemas/spec.schema.json` → ฉบับ 0.4.0 จาก marketplace-train | เพิ่ม `stakeholders[]` · `$defs.stakeholder` · `$defs.stkId` (diff 39 บรรทัด ไม่มีอย่างอื่น) |
| 2 | `.aeon/spec.json` — `meta.schema_version` 0.3.0 → **0.4.0** และแทรก `"stakeholders": []` ต่อจาก `glossary` | ผ่าน ajv (`--spec=draft2020 --all-errors`) → **valid** |
| 3 | `wiki.mjs --write` ลง `docs/wiki/` (แก้ที่เดิม **ไม่ลบทิ้ง** เพราะ `log.md` เป็นชั้น C ต่อท้ายอย่างเดียว) | 293 หน้า · dry-run ตามหลัง = `bundle already matches spec` |
| 4 | ลบ `docs/wiki/req/` ทั้งต้น | orphan 294 หน้า → 0 |
| 5 | `export-contract.mjs --write` | ได้ `.aeon/req/{requirements,glossary,stakeholders,change-set}.json` — ไฟล์ชุดเดียวที่ design plugin อ่านได้ |
| 6 | `.claude/settings.json` เปิด `design@aeon` เพิ่ม | `req@aeon` + `design@aeon` |
| 7 | `.gitattributes` ใส่คำอธิบายเหตุผล LF ให้ตรงกับ marketplace-train | กติกาเดิม `* text=auto eol=lf` ไม่เปลี่ยน |

**ลำดับสำคัญ:** ข้อ 2 ต้องมาก่อนข้อ 5 — export จาก spec 0.3.0 จะได้ `stakeholders.json` ที่ไม่มีช่องนั้น
ติดไปกับสัญญาส่งต่อ แล้ว design plugin จะ bind กับสัญญาที่ผิดตั้งแต่วันแรก

**`docs/requirements/REQ-miniloan-00x.md` ไม่ถูกแตะ** — `doc-hash.mjs` ให้ค่าเท่าเดิมทั้ง 6 ใบหลังย้าย
(hash คิดจาก node ไม่ได้คิดจาก `meta`) เอกสารฝั่งคนอ่านจึงยังตรงกับสเปก · `rollup` ก็ไม่ขยับ

## 3. ด่านตรวจ ก่อน → หลัง (วัดด้วย marketplace-train req 0.4.0)

```
ก่อน:  181 error(s), 285 warning(s)   ← เช็ค 12 (wiki bundle) พังยกชุด
หลัง:    3 error(s),   4 warning(s)   ← เช็ค 12 เขียว
```

**3 error ที่เหลือเป็นงานเนื้อหา มีอยู่ก่อนการย้ายครั้งนี้ ไม่ใช่ผลของมัน** — และห้ามปิดด้วยการเดา:

- เช็ค 2 · `BR-miniloan-053@v1` ยังไม่มีตัวอย่างพิสูจน์ → `/req:example BR-miniloan-053@v1`
- เช็ค 3 · การ์ดแดง `Q-miniloan-015` (โปะแล้วเงินต้นติดลบ) และ `Q-miniloan-016` (ฐานของคำว่า "เงินต้นคงเหลือ") ยังเปิดค้าง
- เตือน เช็ค 13 · กฎ kind=calculation 4 ข้อยังไม่มีเลขเฉลย → `/req:golden` (BR-002 · BR-003 · BR-022 · BR-050)

## 4. ค้างไว้ ต้องตัดสินใจเอง

1. **`stakeholders[]` ยังว่าง** — schema ยอมให้ว่าง (ไม่มี `minItems`) และไม่มีเช็คไหนใน `verify-rules.mjs`
   แตะมันเลย (grep แล้วไม่เจอสักที่) แต่ **design plugin กฎ V23 บังคับให้ทุก role สาวกลับไปหา stakeholder ได้**
   → `/design:rbac` จะไม่ผ่านจนกว่าจะมีข้อมูล · ทางเติมคือ `/req:ask` ชั้น framing (ถามว่าใครแตะข้อมูลนี้บ้าง)
   **ไม่เติมให้เอง** เพราะ stakeholder ทุกตัวต้องมี provenance ที่มี quote จริงกับ locator จริง — แต่งขึ้นมาไม่ได้

2. **`docs/requirement/requirements.md`** (เอกพจน์) เก็บเอกสารต้นทางของ `SRC-001` อยู่
   ส่วน fixture ของ plugin วางไฟล์ต้นทางไว้ที่ `docs/sources/` หรือ `docs/tor/`
   ตอนนี้ด่านตรวจไม่ร้อง (path อยู่ใต้ `docs/` และ hash ตรง) จึง**ไม่ย้าย** — ถ้าจะย้าย ต้องแก้
   `spec.sources[0].path` ในการแก้ครั้งเดียวกัน แล้ว render wiki ใหม่ ไม่งั้น SRC-001 จะชี้ไฟล์ที่ไม่มี

3. **ลิงก์เสียในไฟล์นั้น** — `docs/requirement/requirements.md` อ้าง `../standard/DOC-STANDARD.md`
   ซึ่งไม่มีในโปรเจกต์นี้ (ต้นฉบับอยู่ที่ `docs/standard/DOC-STANDARD.md` ของ marketplace-train)
   ไม่ copy มาให้เพราะจะกลายเป็นเอกสารสองฉบับที่ค่อยๆ ไม่ตรงกัน — ควรแก้เป็นลิงก์ไปต้นฉบับแทน

4. **`docs/req-field-review-v1.md` อ้าง `docs/wiki/req/` ที่ถูกลบไปแล้ว 3 จุด** (บรรทัด 36 · 51 · 145)
   **ไม่แก้ให้** เพราะหัวไฟล์เขียนไว้เองว่า *รอบถัดไปให้เขียนเป็น `req-field-review-v2.md` — ห้ามแก้ทับฉบับนี้*
   มันเป็นบันทึกสภาพ ณ 2026-08-16 ที่ถูกต้องตามเวลานั้น · ถ้าจะตรวจรอบใหม่ ให้เขียน v2

5. **`.aeon/design/` ยังไม่มี** — ถูกต้องแล้ว · `design/check.mjs` ตอบว่า
   *this project has no design yet — run /design:init* ซึ่งเป็นสถานะที่ควรเป็นของโปรเจกต์ที่ยังไม่ปิด CP1

## 5. marketplace ที่ติดตั้งจริง ไม่ใช่ตัวที่โปรเจกต์นี้ถูกจัดให้ตรงแล้ว

`C:\Users\mounc\.claude\plugins\installed_plugins.json` ระบุว่า `req@aeon` ของโปรเจกต์นี้มาจาก
`D:\ProjectClaude\AEON-Work\aeon-marketplace` (**คนละ repo** กับ `-train`) เวอร์ชัน **0.4.1**
สองสายนี้แยกกันเดิน ไม่ใช่รุ่นก่อน-หลังของกันและกัน:

| | `aeon-marketplace-train` (โครงที่ยึด) | `aeon-marketplace` (ที่ติดตั้งอยู่) |
|---|---|---|
| req | 0.4.0 | 0.4.1 |
| ปลั๊กอินในชุด | req · design 0.10.0 | req · design 0.2.1 · screen 0.2.0 · dev 0.1.2 |
| `WIKI_DIR` | `docs/wiki` | `docs/wiki/req` |
| ทางส่งต่อ design | `.aeon/req/*.json` (`export-contract.mjs`) | ไม่มี `export-contract.mjs` เลย |
| ช่องใหม่ใน schema | `stakeholders[]` | `source.ask_round` · `asked[].answer_source` (**ไม่มี `stakeholders`**) |
| ที่อยู่ schema | `schemas/spec.schema.json` (root ของ repo) | `plugins/req/schemas/spec.schema.json` |

**ผลที่จะเกิดถ้าไม่ทำอะไรต่อ:** พิมพ์ `/req:capture` หรือ `/req:ask` ในโปรเจกต์นี้ จะรัน `wiki.mjs` ของ 0.4.1
ซึ่ง render ลง `docs/wiki/req/` → **bundle จะกลับมาซ้อนกันสองชุดอีกครั้ง** และ `/req:check` จะรายงานเลขคนละชุดกับ §3

**ทางแก้ที่เจ้าของต้องเลือกเอง (ยังไม่ได้ทำให้):**

```
/plugin marketplace add D:\ProjectClaude\AEON-Work\aeon-marketplace-train
```

แล้วสลับ `req@aeon` / `design@aeon` ของโปรเจกต์นี้ให้ชี้ marketplace ตัวใหม่
(ตรวจได้ที่ `C:\Users\mounc\.claude\plugins\installed_plugins.json` → `installPath` ต้องไม่ใช่
`cache\aeon\req\0.4.1` อีก) · **ถ้ายังไม่สลับ ห้ามรัน `/req:*` ในโปรเจกต์นี้**

## 6. ข้อบกพร่องที่เจอใน marketplace-train (ไม่ได้แก้ อยู่คนละ repo)

- `plugins/req/scripts/state-dir.mjs` — `VALUE_FLAGS` ไม่มี `--out` แต่ `wiki.mjs` บรรทัด 739 อ่าน
  `values["--out"]` และ header บรรทัด 23 โฆษณา `[--out <dir>]` ไว้ → ธงนี้ใช้ไม่ได้เลย (ได้ exit 2)
- `docs/manual/req-manual.md` เขียน `docs/wiki/req/` ไว้ 6 จุด (บรรทัด 98 · 173 · 183 · 532 · 560 · 596)
  ขัดกับโค้ดที่ render ลง `docs/wiki/` แบน — คู่มือกับตัว render พูดคนละอย่างในตัว repo เอง

## 7. คำสั่งที่ใช้ซ้ำได้

```bash
P="D:/ProjectClaude/AEON-Work/aeon-marketplace-train/plugins/req/scripts"
node "$P/verify-rules.mjs"     --root . --cp1     # ด่าน CP1
node "$P/wiki.mjs"             --root .           # 0 = bundle ตรงกับ spec
node "$P/export-contract.mjs"  --root .           # 0 = สัญญาส่งต่อยัง fresh
node "$P/rollup.mjs"           --root .           # 0 = ตัวเลขสรุปถูก
npx --yes ajv-cli@5 validate -s schemas/spec.schema.json -d .aeon/spec.json \
  --spec=draft2020 --strict=false --all-errors
```
