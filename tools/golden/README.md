# Parity data (original TypeScript → Kotlin)

`app/src/test/resources/gold_ts.txt` holds values computed by the ORIGINAL TypeScript utilities
(`src/utils/{persianDate,currency,exchangeRates,balance,loanSchedule,checks,storage}.ts`). The Kotlin port must reproduce them
exactly (`GoldenParityTest`).

Regenerate (needs Node 22 + esbuild):

```
cp tools/golden/gold.ts <original-repo>/tools/gold.ts
cd <tmp> && npm i esbuild
./node_modules/.bin/esbuild <original-repo>/tools/gold.ts --bundle --platform=node --format=esm --outfile=gold.mjs
TZ=Asia/Tehran node gold.mjs > app/src/test/resources/gold_ts.txt
```

Time zone matters (dates are computed in local time) — the Kotlin harness pins `Asia/Tehran` explicitly.
