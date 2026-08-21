---
type: Example
title: happy — **คำขอมี token แนบไปด้วยทุกครั้ง** และ API ตรวจ token ผ่านแล
description: **คำขอมี token แนบไปด้วยทุกครั้ง** และ API ตรวจ token ผ่านแล้วจึงให้บริการ · ข้อมูลถูกส่งกลับตามปกติ
resource: ../rules/BR-miniloan-030@v1.md
tags: [miniloan, example, happy]
id: EX-miniloan-133
status: draft
kind: happy
proves: [BR-miniloan-030@v1]
has_ui: true
timestamp: 2026-08-15T15:18:00+07:00
spec_hash: sha256:a0892803cc40d60916c98d537d72af85e2a4b9d3e4858ee934e9dae109942937
---

# EX-miniloan-133

## กำหนดให้ (given)
ผู้ใช้ที่ล็อกอินแล้วและมี token จำลองที่ถูกต้อง

## เมื่อ (when)
หน้าเว็บเรียก API เพื่อดึงรายการใบสมัคร

## แล้ว (then)
**คำขอมี token แนบไปด้วยทุกครั้ง** และ API ตรวจ token ผ่านแล้วจึงให้บริการ · ข้อมูลถูกส่งกลับตามปกติ

## พิสูจน์กฎ

- [BR-miniloan-030@v1](../rules/BR-miniloan-030@v1.md) ✅ ปัจจุบัน — Web เรียก API ต้องแนบ token จำลองทุกครั้ง และ API ต้องตรวจสอบ token ก่อนให้บริการ (ระบบ auth จริงอยู่นอกขอบเขต)
