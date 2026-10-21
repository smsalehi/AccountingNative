import { gregorianToJalali, jalaliToGregorian, getJalaliMonthLength, addJalaliMonths, formatJalaliDate, getFullJalaliDate, getShortJalaliDate, normalizePersianDate, toPersianDigits, toEnglishDigits, formatCurrency } from '../src/utils/persianDate';
import { buildRateIndex, rateAtDay, rateAtMs, parseRateFile, mergeRates, buildUsdViewDb, dateKeyToDay, msToDay } from '../src/utils/exchangeRates';
import { computeAccountBalances } from '../src/utils/balance';
import { parseDueDateStr, resolveAnchor, nextDueDateMs, buildPaidIndex, remainingInstallmentsCount, paidInstallmentsCount, resolveTotalInstallments, isLoanSettled } from '../src/utils/loanSchedule';
import { checkUrgency, checkRemaining, checkFundedAmount, pendingChecksTotals, pendingOwnChecksByAccount, pendingPersonChecksByDrawer, checkDeadlineMs } from '../src/utils/checks';
import { normalizeDatabase } from '../src/utils/storage';

const r6 = (x: number) => Math.round(x * 1e6) / 1e6;
const lines: string[] = [];
const out = (k: string, v: unknown) => lines.push(`${k}=${JSON.stringify(v)}`);

// 1. Jalali: every day 2015-03-01..2035-03-31 (sampled), month lengths, add months
let acc: string[] = [];
for (let t = Date.UTC(2015, 2, 1); t <= Date.UTC(2035, 2, 31); t += 86400000 * 3) {
  const d = new Date(t);
  const [jy, jm, jd] = gregorianToJalali(d.getUTCFullYear(), d.getUTCMonth() + 1, d.getUTCDate());
  const g = jalaliToGregorian(jy, jm, jd);
  acc.push(`${jy}/${jm}/${jd}>${g.gy}-${g.gm}-${g.gd}`);
}
out('jalali_roundtrip', acc);
acc = [];
for (let y = 1380; y <= 1480; y++) acc.push([1,2,3,4,5,6,7,8,9,10,11,12].map((m) => getJalaliMonthLength(y, m)).join(''));
out('month_lengths', acc);
out('add_months', [[1403, 12, 1], [1403, 1, -1], [1400, 6, 25], [1399, 12, 13], [1401, 3, 0]].map(([y, m, n]) => addJalaliMonths(y, m, n)));
const stamps = [1700000000000, 1735689600000, 1760000000000, 1577836800000, 1711929600000];
out('format_dates', stamps.map((s) => [formatJalaliDate(s), getFullJalaliDate(new Date(s)), getShortJalaliDate(new Date(s))]));
out('digits', [toPersianDigits('abc 123 4.5'), toEnglishDigits('۱۴۰۳/۰۶/۱۵ ٣'), normalizePersianDate('۱۴۰۳/۶/۵'), normalizePersianDate('03-6-5'), normalizePersianDate('')]);

// 2. currency
const amts = [0, 1, -1, 999, 1234567, -1234567.5, 0.5, 2.5, 123456789012, 1234.567, -0.004, 99999.995, NaN];
out('currency', amts.flatMap((a) => (['toman', 'rial', 'usd'] as const).flatMap((u) => [formatCurrency(a, u, true), formatCurrency(a, u, false)])));

// 3. exchange rates
const rates = [
  { Date: '2024-01-01', Rate: 50000 }, { Date: '2024-01-03', Rate: 52000 }, { Date: '2024-01-10', Rate: 60000 },
  { Date: '2024-02-29', Rate: 61000 }, { Date: '2023-02-29', Rate: 1 }, { Date: 'bad', Rate: 5 }, { Date: '2024-03-01', Rate: -4 },
];
const idx = buildRateIndex(rates)!;
out('rate_index', [idx.days, idx.rates, idx.count, idx.firstDate, idx.lastDate, idx.lastRate]);
const d0 = dateKeyToDay('2024-01-01')!;
out('rate_at_day', [-3, 0, 1, 2, 3, 4, 8, 9, 10, 11, 58, 59, 60, 200].map((o) => r6(rateAtDay(idx, d0 + o))));
out('day_key', [dateKeyToDay('2024-02-29'), dateKeyToDay('2023-02-29'), dateKeyToDay('0050-01-01'), dateKeyToDay('2024-13-01'), dateKeyToDay('1970-01-02')]);
out('rate_at_ms', [1704067200000, 1704150000000, 1704153600000 + 3600000, 1706000000000, 1999999999999].map((m) => [msToDay(m), r6(rateAtMs(idx, m))]));
const fileA = '<Ticker>,<Per>,<DTYYYYMMDD>,<TIME>,<Open>,<High>,<Low>,<Close>,<Vol>\nDOLLAR,D,20240101,000000,500000,510000,490000,505000,1\nDOLLAR,D,20240103,000000,0,0,0,"520,000",1\nDOLLAR,D,20240104,000000,0,0,0,abc,1\nDOLLAR,D,20241399,000000,0,0,0,1,1\n';
const fileB = 'date;rate\n۱۴۰۲/۰۱/۰۱;5\n2024-1-5;71000\n2024/01/06;-1\n';
const fileC = '20240101,500000\n20240102,510000\n';
for (const [n, f, u] of [['A', fileA, 'rial'], ['B', fileB, 'toman'], ['C', fileC, 'rial']] as const) {
  const p = parseRateFile(f, u as any);
  out('parse_' + n, [p.rows, p.totalLines, p.invalid, p.columns]);
}
const mg = mergeRates([{ Date: '2024-01-01', Rate: 50000 }, { Date: '2024-01-05', Rate: 70000 }], [{ Date: '2024-01-01', Rate: 50500 }, { Date: '2024-01-02', Rate: 1 }, { Date: '2024-01-05', Rate: 70000 }], true);
out('merge_over', [mg.merged, mg.added, mg.updated, mg.skippedExisting, mg.firstNewDate, mg.lastNewDate]);
const mg2 = mergeRates([{ Date: '2024-01-01', Rate: 50000 }], [{ Date: '2024-01-01', Rate: 50500 }, { Date: '2024-01-09', Rate: 3 }], false);
out('merge_keep', [mg2.merged, mg2.added, mg2.updated, mg2.skippedExisting, mg2.firstNewDate, mg2.lastNewDate]);

// 4. balances + usd view
const db: any = {
  version: 1, exportDate: 'x', app: 'a', appVersion: '1',
  AccountGroups: [{ UID: 'g1', Title: 'G', Desc: null }],
  Accounts: [
    { UID: 'a1', Title: 'A1', GroupUID: 'g1', OldBalance: 1000 }, { UID: 'a2', Title: 'A2', GroupUID: 'g1', OldBalance: 500 },
    { UID: 'a3', Title: 'A3', GroupUID: 'g1', OldBalance: 0 },
  ],
  Banks: [], Subjects: [], Projects: [], Persons: [{ UID: 'p1', FullName: 'P', GroupUID: '', OldBalance: 700 }], PersonGroups: [], Families: [],
  Transactions: [
    { UID: 't1', TransactionType: 1, TotalValue: 100, DateTime: 1704110000000 },                      // expense with leg
    { UID: 't2', TransactionType: 2, TotalValue: 250, DateTime: 1704200000000 },                      // income with leg
    { UID: 't3', TransactionType: 3, TotalValue: 0, DateTime: 1704300000000 },                        // transfer legs
    { UID: 't4', TransactionType: 1, TotalValue: -40, DateTime: 1704400000000, SourceAccountUID: 'a2' }, // fallback expense (abs)
    { UID: 't5', TransactionType: 3, TotalValue: 70, DateTime: 1704500000000, SourceAccountUID: 'a1', DestinationAccountUID: 'a3' },
    { UID: 't6', TransactionType: 0, TotalValue: 30, DateTime: 0, SourceAccountUID: 'a3' },
    { UID: 't7', TransactionType: 9, TotalValue: 30, DateTime: 1, SourceAccountUID: 'a3' },
    { UID: 't8', TransactionType: 1, TotalValue: 12, DateTime: 1704600000000, SourceAccountUID: 'zzz' },
    { UID: 't9', TransactionType: 1, TotalValue: 60, DateTime: 1704700000000, DeptUID: 'd1' },
    { UID: 't10', TransactionType: 1, TotalValue: 60, DateTime: 1704800000000, DeptUID: 'd1' },
    { UID: 't11', TransactionType: 1, TotalValue: 5, DateTime: 1704900000000, DeptUID: 'd2' },
  ],
  TransactionDetails: [
    { UID: 'x1', TransactionUID: 't1', AccountUID: 'a1', Value: 100, SubType: 0 },
    { UID: 'x2', TransactionUID: 't2', AccountUID: 'a2', Value: 250, SubType: 0 },
    { UID: 'x3', TransactionUID: 't3', AccountUID: 'a1', Value: 300, SubType: 0 },
    { UID: 'x4', TransactionUID: 't3', AccountUID: 'a3', Value: 300, SubType: 1 },
    { UID: 'x5', TransactionUID: 't3', AccountUID: 'nope', Value: 300, SubType: 1 },
    { UID: 'x6', TransactionUID: 't1', AccountUID: 'a2', Value: 0, SubType: 0 },
  ],
  Invoices: [],
  Depts: [], Checks: [], Tags: [], Configs: [], ExchangeRates: [],
};
const bal = computeAccountBalances(db);
out('balances', Array.from(bal.entries()));
const usd = buildUsdViewDb(db, idx);
const ubal = computeAccountBalances(usd);
out('usd_balances', Array.from(ubal.entries()).map(([k, v]) => [k, r6(v)]));
out('usd_tx', usd.Transactions.map((t: any) => r6(t.TotalValue)));
out('usd_persons', usd.Persons.map((p: any) => r6(p.OldBalance)));

// 5. loans
const base = Date.UTC(2024, 0, 15, 8, 0, 0);
const depts: any[] = [
  { UID: 'd1', Title: 'L1', Value: 600, PaymentStartDate: base, PeriodDays: 30, PeriodValue: 100, FirstDueDate: '1402/10/25' },
  { UID: 'd2', Title: 'L2', Value: 1000, PaymentStartDate: base, PeriodDays: 30, PeriodValue: 100, DueDay: 5 },
  { UID: 'd3', Title: 'L3', Value: 1000, PaymentStartDate: base, PeriodDays: 15, PeriodValue: 100, TotalInstallments: 12 },
  { UID: 'd4', Title: 'L4', Value: 1000, PaymentStartDate: base, PeriodDays: 0, PeriodValue: 0 },
  { UID: 'd5', Title: 'L5', Value: 100, PaymentStartDate: base, PeriodDays: 30, PeriodValue: 100, FirstDueDate: '۱۴۰۲/۱۲/۳۰' },
  { UID: 'd6', Title: 'L6', Value: 100, PaymentStartDate: base, PeriodDays: 30, PeriodValue: 100, FirstDueDate: '1403/12/30' },
  { UID: 'd7', Title: 'L7', Value: 100, PaymentStartDate: base, PeriodDays: 30, PeriodValue: 100, DueDay: 31 },
];
const paid = buildPaidIndex(db.Transactions);
out('paid_index', Array.from(paid.entries()));
out('loans', depts.map((d) => [d.UID, resolveTotalInstallments(d), paidInstallmentsCount(d, paid), remainingInstallmentsCount(d, paid), resolveAnchor(d), nextDueDateMs(d, paid), isLoanSettled(d, paid)]));
out('due_parse', ['1403/12/25', ' ۱۴۰۳/۱/۳۱ ', '1403/7/31', '1299/1/1', '1403/13/1', '1403/12/30', '1404/12/30', 'abc', '1403/12', '1403//5'].map((s) => parseDueDateStr(s)));

// 6. checks
const now = Date.UTC(2024, 5, 10, 9, 30, 0);
const jd = (ms: number) => formatJalaliDate(ms);
const checkTxs: any[] = [{ UID: 'c1', TransactionType: 1, TotalValue: -300 }, { UID: 'c2', TransactionType: 1, TotalValue: 200 }];
const checks: any[] = [
  { UID: 'k1', Kind: 'own', Amount: 1000, DueDate: jd(now + 86400000 * 5), AccountUID: 'a1', Status: 'pending', CreatedAt: 1 },
  { UID: 'k2', Kind: 'own', Amount: 500, DueDate: jd(now - 86400000 * 2), AccountUID: 'a1', Status: 'pending', CreatedAt: 1 },
  { UID: 'k3', Kind: 'own', Amount: 700, DueDate: jd(now), AccountUID: 'a2', Status: 'pending', CreatedAt: 1 },
  { UID: 'k4', Kind: 'own', Amount: 700, DueDate: jd(now + 86400000 * 2), AccountUID: 'a2', Status: 'passed', CreatedAt: 1 },
  { UID: 'k5', Kind: 'person', Amount: 1000, DueDate: jd(now + 86400000 * 4), DrawerPersonUID: 'p1', Status: 'pending', CreatedAt: 1, Deposits: [{ UID: 'dd1', AccountUID: 'a1', Amount: 1, DateTime: 1, TransactionUID: 'c1' }, { UID: 'dd2', AccountUID: 'a1', Amount: 1, DateTime: 1, TransactionUID: 'gone' }] },
  { UID: 'k6', Kind: 'person', Amount: 500, DueDate: jd(now + 86400000 * 30), FundByDate: jd(now + 86400000 * 3), DrawerPersonUID: 'p1', Status: 'pending', CreatedAt: 1, Deposits: [{ UID: 'dd3', AccountUID: 'a1', Amount: 1, DateTime: 1, TransactionUID: 'c2' }] },
  { UID: 'k7', Kind: 'person', Amount: 200, DueDate: jd(now + 86400000 * 1), DrawerPersonUID: 'p2', Status: 'pending', CreatedAt: 1, Deposits: [{ UID: 'dd4', AccountUID: 'a1', Amount: 1, DateTime: 1, TransactionUID: 'c2' }] },
  { UID: 'k8', Kind: 'own', Amount: 5, DueDate: 'garbage', AccountUID: 'a3', Status: 'pending', CreatedAt: 1 },
];
out('check_urgency', checks.map((c) => [c.UID, checkUrgency(c, checkTxs, now), r6(checkRemaining(c, checkTxs)), r6(checkFundedAmount(c, checkTxs)), checkDeadlineMs(c)]));
const cdb: any = { ...db, Checks: checks, Transactions: checkTxs };
out('check_totals', pendingChecksTotals(cdb));
out('check_own_by_acc', Array.from(pendingOwnChecksByAccount(cdb).entries()));
out('check_by_drawer', Array.from(pendingPersonChecksByDrawer(cdb).entries()));

// 7. normalize
const messy: any = {
  version: 1,
  Accounts: [{ UID: 'a1', Title: 'x', OldBalance: '1500', GroupUID: 'gone' }, { UID: 'a2', OldBalance: 'abc' }],
  Subjects: [{ UID: 's1', Title: 'S', IsCost: 1, ParentUID: null }, null],
  Transactions: [
    { UID: 't1', TransactionType: '1', TotalValue: '100', DateTime: '1704110000000', PersianDate: 14030101 },
    { UID: 't2', TransactionType: 3, TotalValue: 0, DateTime: 5 },
    { UID: 't3', TransactionType: 2, TotalValue: 0, DateTime: 6 },
    { UID: 't4', TransactionType: 77, TotalValue: 'x' },
    7,
  ],
  TransactionDetails: [
    { UID: 'd1', TransactionUID: 't1', AccountUID: 'a1', Value: 100, SubType: 0 },
    { UID: 'd2', TransactionUID: 't2', AccountUID: 'a1', Value: 900, SubType: 0 },
    { UID: 'd3', TransactionUID: 't2', AccountUID: 'a2', Value: 800, SubType: 1 },
    { UID: 'd4', TransactionUID: 't3', AccountUID: 'a2', Value: 40, SubType: 0 },
    { UID: 'd5', TransactionUID: 't3', AccountUID: 'a1', Value: -60, SubType: 0 },
  ],
  Checks: [{ UID: 'k1', Kind: 'own', Amount: '10', DueDate: '1403/01/01', Status: 'pending' }, { UID: 'k2', Amount: 'x' }, { Kind: 'own', Amount: 1 }],
  ExchangeRates: [{ Date: '2024-01-01', Rate: 5 }, { Date: '2024-02-30', Rate: 5 }, { Date: '2024-01-02', Rate: 0 }, { Date: '2024-01-03', Rate: '7' }],
};
const norm = normalizeDatabase(messy);
const nd = norm.db as any;
out('norm_counts', [nd.Accounts.length, nd.Transactions.length, nd.TransactionDetails.length, nd.Checks.length, nd.ExchangeRates.length, nd.AccountGroups.length, nd.AccountGroups[0]?.UID]);
out('norm_tx', nd.Transactions.map((t: any) => [t.UID, t.TransactionType, t.TotalValue, t.DateTime, t.PersianDate, t.SourceAccountUID ?? null, t.DestinationAccountUID ?? null]));
out('norm_acc', nd.Accounts.map((a: any) => [a.UID, a.OldBalance, a.GroupUID]));
out('norm_bal', Array.from(computeAccountBalances(nd).entries()));
out('norm_checks_rates', [nd.Checks.map((c: any) => c.UID), nd.ExchangeRates]);
out('norm_repaired_count', norm.repaired.length);

console.log(lines.join('\n'));
