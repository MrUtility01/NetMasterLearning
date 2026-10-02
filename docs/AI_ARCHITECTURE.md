# NetMaster AI Architecture

## هدف
AI در این پروژه یک «دستیار قابل کنترل» است، نه یک چت‌بات آزاد که مستقیماً روی زیرساخت تغییر ایجاد کند.

### لایه‌ها
1. **AI Mentor UI** — پرسش، علائم، هدف و context.
2. **Prompt/Task Router** — تشخیص نوع کار: آموزش، Troubleshooting، خلاصه‌سازی، تولید Command، تحلیل Log.
3. **Knowledge/RAG** — جستجو در curriculum، notes، runbooks، command reference و مستندات کاربر.
4. **Model Provider** — قابل اتصال به APIهای ابری یا مدل محلی؛ API key هرگز داخل APK قرار نگیرد.
5. **Tool Gateway** — ابزارهای read-only مثل ping, DNS lookup, log search, Zabbix query و packet-analysis؛ عملیات تغییر‌دهنده نیازمند تأیید صریح.
6. **Validator** — syntax/config validation، policy check و safety gates.
7. **Audit** — ثبت prompt، context، model version، tool calls، نتیجه و approval.

## RAG
Pipeline پیشنهادی:
`Documents → Parse → Chunk → Metadata → Embedding → Vector/Hybrid Search → Rerank → Context → LLM → Citation`

Metadata: topic, platform, version, vendor, difficulty, date, source, confidentiality.

## Agent Safety
- least privilege
- allowlist ابزارها
- read-only by default
- human approval برای config/change
- timeout/retry/idempotency
- audit trail
- prompt-injection defense
- data minimization
- secret redaction

## AI برای شبکه
- تحلیل Syslog و Zabbix
- correlation رخدادها
- خلاصه incident
- پیشنهاد hypothesis
- تولید runbook
- بررسی config قبل از deployment
- تحلیل Wireshark/tcpdump summaries
- capacity forecasting
- change-risk analysis

## Private AI
برای سازمان‌ها، مسیر Local/Private AI شامل مدل محلی، embedding محلی، RAG داخلی، کنترل دسترسی و audit است. ابزارهایی مانند Ollama فقط به‌عنوان provider اختیاری در نظر گرفته می‌شوند و نباید وابستگی اجباری به یک vendor ایجاد شود.
