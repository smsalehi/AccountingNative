package com.myaccounting.app.domain

import java.text.NumberFormat
import java.util.Locale

/**
 * Pure port of excelExport.ts + pdfExport.ts: identical CSV and report-HTML bytes.
 * Delivery (folder picking / SAF write) lives in data/ExportStore.kt.
 */
object ExportFiles {

    /** currency.ts currencyUnitLabel. */
    fun currencyUnitLabel(unit: CurrencyUnit): String = when (unit) {
        CurrencyUnit.USD -> "دلار"
        CurrencyUnit.RIAL -> "ریال"
        CurrencyUnit.TOMAN -> "تومان"
    }

    /** pdfExport.ts fmtNum: fa-IR number; USD always with 2 fraction digits. */
    private fun fmtNum(v: Double, unit: CurrencyUnit): String {
        val nf = NumberFormat.getNumberInstance(Locale("fa", "IR"))
        if (unit == CurrencyUnit.USD) {
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
        }
        return nf.format(v)
    }

    /** excelExport.ts csvText: quotes are doubled (commas stay inside quotes). */
    private fun csvText(v: String?): String = (v ?: "").replace("\"", "\"\"")

    /** The desc cell also rewrites ASCII commas to Persian ones. */
    private fun csvDesc(v: String?): String =
        (v ?: "").replace(",", "،").replace("\"", "\"\"")

    fun todayFileName(base: String, extension: String): String =
        "${base}_${JalaliDate.toPersianDigits(java.time.LocalDate.now().toString())}.$extension"

    // --- CSV (Excel) ---------------------------------------------------------------------------

    private fun csvHeader(unit: String): String =
        "ردیف,تاریخ شمسی,نوع تراکنش,سرفصل / دسته‌بندی,حساب بانکی,مبلغ ($unit),شرح\n"

    /**
     * exportTransactionsToExcel (the whole DB): type 0 falls back to «انتقال» here.
     */
    fun transactionsCsv(db: AccountingDatabase, unit: CurrencyUnit): String {
        val unitLabel = currencyUnitLabel(unit)
        val sb = StringBuilder("\uFEFF").append(csvHeader(unitLabel))
        val subjectTitle = db.subjects.associate { it.uid to it.title }
        val accountTitle = db.accounts.associate { it.uid to it.title }
        db.transactions.forEachIndexed { index, t ->
            val typeTitle = when (t.transactionType) {
                2 -> "درآمد"
                1 -> "هزینه"
                else -> "انتقال"
            }
            val subject = (t.subjectUid?.let(subjectTitle::get))?.let(::csvText) ?: "سایر"
            val account = (t.sourceAccountUid?.let(accountTitle::get))?.let(::csvText) ?: "-"
            val displayVal = Currency.toDisplayNumber(Math.abs(t.totalValue), unit)
            sb.append("${index + 1},\"${t.persianDate}\",\"$typeTitle\",\"$subject\",\"$account\",$displayVal,\"${csvDesc(t.desc)}\"\n")
        }
        return sb.toString()
    }

    /**
     * exportTransactionsToExcelFiltered: type 0 shows as «دریافت از شخص».
     */
    fun transactionsCsvFiltered(transactions: List<Transaction>, db: AccountingDatabase, unit: CurrencyUnit): String {
        val unitLabel = currencyUnitLabel(unit)
        val sb = StringBuilder("\uFEFF").append(csvHeader(unitLabel))
        val subjectTitle = db.subjects.associate { it.uid to it.title }
        val accountTitle = db.accounts.associate { it.uid to it.title }
        transactions.forEachIndexed { index, t ->
            val typeTitle = when (t.transactionType) {
                2 -> "درآمد"
                1 -> "هزینه"
                0 -> "دریافت از شخص"
                else -> "انتقال"
            }
            val subject = (t.subjectUid?.let(subjectTitle::get))?.let(::csvText) ?: "سایر"
            val account = (t.sourceAccountUid?.let(accountTitle::get))?.let(::csvText) ?: "-"
            val displayVal = Currency.toDisplayNumber(Math.abs(t.totalValue), unit)
            sb.append("${index + 1},\"${t.persianDate}\",\"$typeTitle\",\"$subject\",\"$account\",$displayVal,\"${csvDesc(t.desc)}\"\n")
        }
        return sb.toString()
    }

    // --- PDF (print-ready HTML report) ----------------------------------------------------------

    /**
     * pdfExport.ts generatePdfHtml, byte-identical (Vazirmatn/Tahoma stack, sky header bar,
     * dark table head, zebra rows, dashed footer).
     */
    private fun generatePdfHtml(
        title: String,
        subtitle: String?,
        summaryCards: List<Card>,
        headers: List<String>,
        rows: List<List<String>>,
    ): String {
        val currentDate = JalaliDate.getFullJalaliDate(System.currentTimeMillis())

        val cardsHtml = if (summaryCards.isNotEmpty()) {
            val inner = summaryCards.joinToString("") { c ->
                "\n          <div class=\"card\">\n" +
                    "            <span class=\"card-label\">${c.label}</span>\n" +
                    "            <span class=\"card-value\" style=\"color: ${c.color};\">${c.value}</span>\n" +
                    "          </div>\n        "
            }
            "\n      <div class=\"summary-cards\">\n        $inner\n      </div>\n    "
        } else ""

        val tableHeadersHtml = headers.joinToString("") { "<th>$it</th>" }
        val tableRowsHtml = rows.mapIndexed { idx, row ->
            val cells = row.mapIndexed { cIdx, cell ->
                if (cIdx == row.size - 1) "<td style=\"font-weight: 600;\">$cell</td>" else "<td>$cell</td>"
            }.joinToString("")
            "\n    <tr>\n" +
                "      <td style=\"text-align: center; font-weight: bold; width: 40px;\">${JalaliDate.toPersianDigits(idx + 1)}</td>\n" +
                "      $cells\n" +
                "    </tr>\n  "
        }.joinToString("")

        val subtitleHtml = if (subtitle != null) "<p>$subtitle</p>" else ""

        return """<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$title - $currentDate</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Vazirmatn', 'Tahoma', sans-serif; direction: rtl; background: #ffffff; color: #1e293b; padding: 24px; font-size: 12px; line-height: 1.5; }
    .header { display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid #0ea5e9; padding-bottom: 16px; margin-bottom: 20px; }
    .header-title { display: flex; align-items: center; gap: 12px; }
    .app-badge { background: #0ea5e9; color: #ffffff; padding: 6px 12px; border-radius: 8px; font-weight: 900; font-size: 14px; }
    .title-text h1 { font-size: 18px; font-weight: 900; color: #0f172a; }
    .title-text p { font-size: 11px; color: #64748b; }
    .header-date { text-align: left; font-size: 11px; color: #475569; }
    .summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; margin-bottom: 20px; }
    .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 12px; display: flex; flex-direction: column; gap: 4px; }
    .card-label { font-size: 10px; color: #64748b; }
    .card-value { font-size: 14px; font-weight: 900; }
    table { width: 100%; border-collapse: collapse; margin-top: 10px; border-radius: 8px; overflow: hidden; font-size: 11px; }
    th { background: #0f172a; color: #ffffff; padding: 10px 12px; text-align: right; font-weight: 700; border: 1px solid #0f172a; }
    td { padding: 9px 12px; border: 1px solid #e2e8f0; color: #334155; }
    tr:nth-child(even) { background: #f8fafc; }
    .footer { margin-top: 30px; padding-top: 12px; border-top: 1px dashed #cbd5e1; display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8; }
    @media print { body { padding: 0; } }
  </style>
</head>
<body>
  <div class="header">
    <div class="header-title">
      <div class="app-badge">حسابداری</div>
      <div class="title-text">
        <h1>$title</h1>
        $subtitleHtml
      </div>
    </div>
    <div class="header-date">
      <div>تاریخ گزارش: <strong>$currentDate</strong></div>
      <div style="margin-top: 2px;">وضعیت: گزارش رسمی</div>
    </div>
  </div>
  $cardsHtml
  <table>
    <thead><tr><th style="width: 40px; text-align: center;">#</th>$tableHeadersHtml</tr></thead>
    <tbody>$tableRowsHtml</tbody>
  </table>
  <div class="footer">
    <span>تولید شده توسط سیستم حسابداری هوشمند</span>
    <span>صفحه ۱ از ۱</span>
  </div>
</body>
</html>"""
    }

    private data class Card(val label: String, val value: String, val color: String)

    private fun transactionsHtml(
        transactions: List<Transaction>,
        db: AccountingDatabase,
        unit: CurrencyUnit,
        title: String,
        subtitle: String,
    ): String {
        val unitLabel = currencyUnitLabel(unit)
        val totalIncome = Math.abs(transactions.filter { it.transactionType == 2 }.sumOf { Math.abs(it.totalValue) })
        val totalExpense = Math.abs(transactions.filter { it.transactionType == 1 }.sumOf { Math.abs(it.totalValue) })
        val subjectsById = db.subjects.associateBy { it.uid }
        val accountsById = db.accounts.associateBy { it.uid }

        val headers = listOf("تاریخ", "نوع", "سرفصل", "حساب بانکی", "مبلغ ($unitLabel)", "شرح")
        val rows = transactions.map { t ->
            val typeStr = when (t.transactionType) {
                2 -> "درآمد"
                1 -> "هزینه"
                0 -> "دریافت از شخص"
                else -> "انتقال"
            }
            val subject = t.subjectUid?.let(subjectsById::get)?.title ?: "سایر"
            val account = t.sourceAccountUid?.let(accountsById::get)?.title ?: "-"
            listOf(
                JalaliDate.toPersianDigits(t.persianDate),
                typeStr,
                subject,
                account,
                fmtNum(Math.abs(t.totalValue), unit),
                t.desc ?: "-",
            )
        }

        return generatePdfHtml(
            title = title,
            subtitle = subtitle,
            summaryCards = listOf(
                Card("کل درآمدها", fmtNum(totalIncome, unit) + " " + unitLabel, "#059669"),
                Card("کل هزینه‌ها", fmtNum(totalExpense, unit) + " " + unitLabel, "#e11d48"),
                Card("تراز خالص", fmtNum(totalIncome - totalExpense, unit) + " " + unitLabel, "#0284c7"),
            ),
            headers = headers,
            rows = rows,
        )
    }

    fun transactionsPdfHtml(db: AccountingDatabase, unit: CurrencyUnit): String =
        transactionsHtml(
            db.transactions, db, unit,
            title = "گزارش کامل تراکنش‌های مالی",
            subtitle = "شامل ${JalaliDate.toPersianDigits(db.transactions.size)} تراکنش ثبت‌شده در سیستم",
        )

    fun transactionsPdfHtmlFiltered(transactions: List<Transaction>, db: AccountingDatabase, unit: CurrencyUnit): String =
        transactionsHtml(
            transactions, db, unit,
            title = "گزارش تراکنش‌های فیلتر شده",
            subtitle = "شامل ${JalaliDate.toPersianDigits(transactions.size)} تراکنش از نتایج فیلتر",
        )
}
