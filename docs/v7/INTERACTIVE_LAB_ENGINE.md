# Interactive Lab Engine

هر Lab یک state machine است:
INIT -> BASELINE -> TASK -> VALIDATE -> FAULT -> INVESTIGATE -> FIX -> VERIFY -> SCORE -> RESET

## انواع Lab
- Guided: راهنمای مرحله‌ای
- Practice: فقط هدف و محدودیت
- Incident: فقط symptom و evidence اولیه
- Exam: بدون hint
- AI Coach: hint تطبیقی
- Red/Blue: سناریوی دفاع و تشخیص در محیط کنترل‌شده

## امتیازدهی
40% correctness
20% evidence quality
15% troubleshooting path
15% validation
10% documentation

## Fault Injection
نمونه‌ها:
- wrong VLAN
- trunk allowed-list mismatch
- DNS record error
- DHCP scope exhaustion
- route missing
- MTU/MSS mismatch
- NAT rule mismatch
- firewall rule ordering
- packet loss/jitter
- expired certificate
- failed service

هر Fault باید reset و rollback امن داشته باشد.
