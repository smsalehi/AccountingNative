#!/usr/bin/env python3
"""Builds a sample database (original backup format) that reproduces what the reference screenshots show:
group totals 108,065,129 / 1,841,360,194 / 0, the three visible cards, «پس از چک‌ها: -135,989,008» and a bell badge of 1."""
import json, sys, time

now_ms = int(time.time() * 1000)
DAY = 86_400_000

def acc(uid, title, group, bal, bank=None):
    return {"UID": uid, "Title": title, "GroupUID": group, "OldBalance": bal, "BankUID": bank, "BankBranchName": None,
            "CardNumber": None, "BankAccountNumber": None, "IsHidden": 0, "GpsLocation": None,
            "MinimumBalanceAlert": 0, "ShowOnNotification": 0, "Desc": None}

accounts = [
    acc("acc_1", "ملت جاری", "grp_1", 10992, "mellat"),
    acc("acc_2", "ملت(کارت)", "grp_1", 13987710, "mellat"),
    acc("acc_3", "مفیدکارت", "grp_1", 75411246, "hekmat"),
    acc("acc_4", "حساب نمونه ۱", "grp_1", 6000000),
    acc("acc_5", "حساب نمونه ۲", "grp_1", 6000000),
    acc("acc_6", "حساب نمونه ۳", "grp_1", 6655181),
] + [acc(f"acc_{i}", f"پس‌انداز نمونه {i-6}", "grp_2", 250000000) for i in range(7, 13)] + [
    acc("acc_13", "پس‌انداز نمونه ۷", "grp_2", 341360194),
    acc("acc_14", "کیف پول", "grp_3", 0),
]

db = {
    "version": 1, "exportDate": "2026-10-16T00:00:00.000Z", "app": "حسابداری", "appVersion": "1.0.0",
    "AccountGroups": [
        {"UID": "grp_1", "Title": "اعتباری", "Desc": None},
        {"UID": "grp_2", "Title": "وام و پس انداز", "Desc": None},
        {"UID": "grp_3", "Title": "نقدی", "Desc": None},
    ],
    "Accounts": accounts,
    "Banks": [{"UID": "mellat", "Title": "ملت"}, {"UID": "hekmat", "Title": "حکمت ایرانیان"}],
    "Subjects": [], "Projects": [], "Persons": [], "PersonGroups": [], "Families": [],
    "Transactions": [], "TransactionDetails": [], "Invoices": [],
    # one loan whose next installment is due tomorrow → bell badge «۱», not overdue → no red dot on the loans tab
    "Depts": [{"UID": "dept_1", "Title": "وام نمونه", "Value": 1000000, "PaymentStartDate": now_ms + DAY, "PeriodDays": 30,
               "PeriodValue": 100000, "ReminderDaysBeforeDueDate": 2, "IsHidden": 0, "Desc": ""}],
    # pending own check on «ملت جاری» → «پس از چک‌ها: -135,989,008»
    "Checks": [{"UID": "chk_1", "Kind": "own", "Amount": 136000000, "DueDate": "1406/12/29", "AccountUID": "acc_1",
                "Status": "pending", "Deposits": [], "CreatedAt": now_ms}],
    "Tags": [], "Configs": [], "ExchangeRates": [],
}
json.dump(db, open(sys.argv[1] if len(sys.argv) > 1 else "sample_db.json", "w", encoding="utf-8"), ensure_ascii=False)
