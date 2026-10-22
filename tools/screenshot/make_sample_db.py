#!/usr/bin/env python3
"""Builds a sample database (original backup format) that reproduces what the reference screenshots show:
group totals 108,065,129 / 1,881,360,194 / 0, the three visible cards, «پس از چک‌ها: -135,989,008» and a bell badge of 1."""
import json, sys, time
import datetime
from jalali import jalali_str

now_ms = int(time.time() * 1000)
DAY = 86_400_000
today = datetime.date.today()

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
] + [acc(f"acc_{i}", f"پس‌انداز نمونه {i-6}", "grp_2", 290000000 if i == 12 else 250000000) for i in range(7, 13)] + [
    acc("acc_13", "پس‌انداز نمونه ۷", "grp_2", 341360194),
    acc("acc_14", "کیف پول", "grp_3", 0),
]

def tx(uid, ttype, value, day_offset, time_str, desc, subject=None, src="acc_1", dst=None,
       person=None, project=None, dept=None):
    d = today - datetime.timedelta(days=day_offset)
    return {
        "UID": uid, "SubjectUID": subject if ttype != 3 else None, "TransactionType": ttype,
        "TotalValue": value, "DateTime": int(time.mktime(d.timetuple()) * 1000),
        "PersianDate": jalali_str(d.year, d.month, d.day), "Time": time_str,
        "EventUID": None, "ProjectUID": project, "PersonUID": person, "FamilyUID": None,
        "InvoiceUID": None, "DeptUID": dept, "SourceAccountUID": src,
        "DestinationAccountUID": dst, "Desc": desc, "IsHidden": 0,
    }

# Four days of history so the day-group headers, the daily balance chips and the
# «۷ روز اخیر / امروز» presets all have something to show.
transactions = [
    tx("tx_01", 1, -350000, 0, "09:15", "بنزین", "subj_21"),
    tx("tx_02", 1, -1250000, 0, "11:40", "قبض برق", "subj_12"),
    tx("tx_03", 2, 45000000, 0, "12:05", "حقوق مهر", "subj_3", src="acc_3"),
    tx("tx_04", 3, -5000000, 0, "18:30", "انتقال وجه", src="acc_1", dst="acc_2"),
    tx("tx_05", 1, -890000, 1, "10:00", "شارژ ساختمان", "subj_12", person="prs_1"),
    tx("tx_06", 1, -2400000, 1, "14:20", "اجاره", "subj_11", project="prj_1", src="acc_2"),
    tx("tx_07", 2, 15000000, 1, "16:45", "فروش پروژه", "subj_3", project="prj_1", src="acc_3"),
    tx("tx_08", 3, -3000000, 1, "20:10", "انتقال وجه", src="acc_3", dst="acc_14"),
    tx("tx_09", 1, -320000, 2, "08:30", "سوخت", "subj_21", src="acc_2"),
    tx("tx_10", 1, -5600000, 2, "13:15", "تعمیر خانه", "subj_11", dept="dept_1"),
    tx("tx_11", 2, 8000000, 2, "15:00", "پاداش", "subj_3", src="acc_3"),
    tx("tx_12", 1, -1200000, 2, "19:25", "اینترنت و تلفن", "subj_12"),
    tx("tx_13", 1, -2750000, 3, "12:00", "خرید سوپرمارکت", "subj_12", person="prs_2"),
    tx("tx_14", 2, 2500000, 3, "17:35", "تسویه با علی", "subj_3", person="prs_1", src="acc_2"),
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
    "Subjects": [
        {"UID": "subj_1", "Title": "خانه و مسکن", "IsCost": 1, "ParentUID": None, "IsHidden": 0, "Icon": "home"},
        {"UID": "subj_11", "Title": "اجاره‌بها", "IsCost": 1, "ParentUID": "subj_1", "IsHidden": 0, "Icon": "rent"},
        {"UID": "subj_12", "Title": "قبوض و شارژ", "IsCost": 1, "ParentUID": "subj_1", "IsHidden": 0, "Icon": "receipt"},
        {"UID": "subj_2", "Title": "خودرو", "IsCost": 1, "ParentUID": None, "IsHidden": 0, "Icon": "car"},
        {"UID": "subj_21", "Title": "سوخت و بنزین", "IsCost": 1, "ParentUID": "subj_2", "IsHidden": 0, "Icon": "fuel"},
        {"UID": "subj_3", "Title": "حقوق و دستمزد", "IsCost": 0, "ParentUID": None, "IsHidden": 0, "Icon": "salary"},
    ],
    "Projects": [{"UID": "prj_1", "Title": "پروژه نمونه", "IsActive": 1, "Desc": ""}],
    "PersonGroups": [{"UID": "pg_1", "Title": "خانواده", "Desc": None}],
    "Persons": [
        {"UID": "prs_1", "FullName": "علی رضایی", "GroupUID": "pg_1", "Tel": "", "Mobile": "09120000000",
         "Email": "", "Address": "", "OldBalance": 0, "IsHidden": 0, "Desc": ""},
        {"UID": "prs_2", "FullName": "سارا محمدی", "GroupUID": "pg_1", "Tel": "", "Mobile": "09121111111",
         "Email": "", "Address": "", "OldBalance": 0, "IsHidden": 0, "Desc": ""},
    ],
    "Families": [],
    "Transactions": transactions, "TransactionDetails": [], "Invoices": [],
    # one loan whose next installment is due tomorrow → bell badge «۱», not overdue → no red dot on the loans tab
    "Depts": [{"UID": "dept_1", "Title": "وام نمونه", "Value": 1000000, "PaymentStartDate": now_ms + DAY, "PeriodDays": 30,
               "PeriodValue": 100000, "ReminderDaysBeforeDueDate": 2, "IsHidden": 0, "Desc": ""}],
    # pending own check on «ملت جاری» → «پس از چک‌ها: -135,989,008»
    "Checks": [{"UID": "chk_1", "Kind": "own", "Amount": 136000000, "DueDate": "1406/12/29", "AccountUID": "acc_1",
                "Status": "pending", "Deposits": [], "CreatedAt": now_ms}],
    "Tags": [], "Configs": [], "ExchangeRates": [],
}
json.dump(db, open(sys.argv[1] if len(sys.argv) > 1 else "sample_db.json", "w", encoding="utf-8"), ensure_ascii=False)
