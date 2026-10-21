package com.myaccounting.app.domain

/** Port of src/data/iranianBanks.ts (ids, names, logo file, BIN prefixes, search keywords). */
data class IranianBank(val id: String, val name: String, val shortName: String, val category: String, val file: String?, val cardPrefixes: List<String>, val keywords: List<String>)

object IranianBanks {
    val all: List<IranianBank> = listOf(
        IranianBank("melli", "بانک ملی ایران", "ملی", "state", "Melli.svg", listOf("603799"), listOf("ملی", "bmi", "melli")),
        IranianBank("mellat", "بانک ملت", "ملت", "state", "Mellat.svg", listOf("610433", "991975"), listOf("ملت", "mellat")),
        IranianBank("saderat", "بانک صادرات ایران", "صادرات", "state", "Saderat.svg", listOf("603769", "903769"), listOf("صادرات", "bsi", "saderat")),
        IranianBank("tejarat", "بانک تجارت", "تجارت", "state", "Tejarat.svg", listOf("627353", "585983"), listOf("تجارت", "tejarat")),
        IranianBank("sepah", "بانک سپه", "سپه", "state", "Sepah.svg", listOf("589210"), listOf("سپه", "sepah")),
        IranianBank("keshavarzi", "بانک کشاورزی", "کشاورزی", "state", "Keshavarzi.svg", listOf("603770", "639217"), listOf("کشاورزی", "keshavarzi", "agri")),
        IranianBank("maskan", "بانک مسکن", "مسکن", "state", "Maskan.svg", listOf("628023"), listOf("مسکن", "maskan")),
        IranianBank("refah", "بانک رفاه کارگران", "رفاه", "state", "Refah.svg", listOf("589463"), listOf("رفاه", "refah")),
        IranianBank("sanat_madan", "بانک صنعت و معدن", "صنعت و معدن", "state", "Sanat_Madan.svg", listOf("627961"), listOf("صنعت و معدن", "صنعت")),
        IranianBank("tosee_taavon", "بانک توسعه تعاون", "توسعه تعاون", "state", "Tosee_Taavon.svg", listOf("502908"), listOf("توسعه تعاون", "تعاون")),
        IranianBank("tosee_saderat", "بانک توسعه صادرات ایران", "توسعه صادرات", "state", "Tosee_Saderat.svg", listOf("627648", "207177"), listOf("توسعه صادرات")),
        IranianBank("postbank", "پست بانک ایران", "پست بانک", "state", "Postbank.svg", listOf("627760"), listOf("پست بانک", "پست")),
        IranianBank("pasargad", "بانک پاسارگاد", "پاسارگاد", "private", "Pasargad.svg", listOf("502229", "639347"), listOf("پاسارگاد", "pasargad")),
        IranianBank("saman", "بانک سامان", "سامان", "private", "Saman.svg", listOf("621986"), listOf("سامان", "saman")),
        IranianBank("parsian", "بانک پارسیان", "پارسیان", "private", "Parsian.svg", listOf("622106", "639194"), listOf("پارسیان", "parsian")),
        IranianBank("eghtesad_novin", "بانک اقتصاد نوین", "اقتصاد نوین", "private", "Eghtesad_Novin.svg", listOf("627412"), listOf("اقتصاد نوین", "نوین")),
        IranianBank("shahr", "بانک شهر", "شهر", "private", "Shahr.svg", listOf("504706", "502806"), listOf("شهر", "shahr")),
        IranianBank("ayandeh", "بانک آینده", "آینده", "private", "Ayandeh.svg", listOf("636214"), listOf("آینده", "ayandeh", "ابانك", "آبانک")),
        IranianBank("sina", "بانک سینا", "سینا", "private", "Sina.svg", listOf("639346"), listOf("سینا", "sina")),
        IranianBank("dey", "بانک دی", "دی", "private", "Dey.svg", listOf("502938"), listOf("دی", "dey")),
        IranianBank("karafarin", "بانک کارآفرین", "کارآفرین", "private", "Karafarin.svg", listOf("627488"), listOf("کارآفرین", "کارافرین")),
        IranianBank("gardeshgari", "بانک گردشگری", "گردشگری", "private", "Gardeshgari.svg", listOf("505416"), listOf("گردشگری", "توبانک")),
        IranianBank("iran_zamin", "بانک ایران زمین", "ایران زمین", "private", "Iran_Zamin.svg", listOf("505785"), listOf("ایران زمین")),
        IranianBank("sarmayeh", "بانک سرمایه", "سرمایه", "private", "Sarmayeh.svg", listOf("639607"), listOf("سرمایه")),
        IranianBank("khavar_mianeh", "بانک خاورمیانه", "خاورمیانه", "private", "Khavar_Mianeh.svg", listOf("585983"), listOf("خاورمیانه", "خاور میانه", "بانکینو")),
        IranianBank("resalat", "بانک قرض‌الحسنه رسالت", "رسالت", "gharzolhasane", "Resalat.svg", listOf("504172"), listOf("رسالت", "resalat", "قرض الحسنه رسالت")),
        IranianBank("mehr_iran", "بانک قرض‌الحسنه مهر ایران", "مهر ایران", "gharzolhasane", "Mehr_Iran.svg", listOf("606373"), listOf("مهر ایران", "مهر", "qmb")),
        IranianBank("mehr_eghtesad", "بانک مهر اقتصاد", "مهر اقتصاد", "gharzolhasane", "Mehr_Eghtesad.svg", listOf("639370"), listOf("مهر اقتصاد")),
        IranianBank("blu", "بلو بانک (سامان)", "بلو بانک", "neobank", null, listOf("621986"), listOf("بلو", "blu", "بلوبانک")),
        IranianBank("vpod", "ویپاد (پاسارگاد)", "ویپاد", "neobank", null, listOf("502229"), listOf("ویپاد", "vpod", "ترابانک")),
        IranianBank("bankino", "بانکینو (خاورمیانه)", "بانکینو", "neobank", null, listOf("585983"), listOf("بانکینو", "bankino")),
        IranianBank("tobank", "توبانک (گردشگری)", "توبانک", "neobank", null, listOf("505416"), listOf("توبانک", "tobank")),
        IranianBank("mofid", "مفید کارت (کارگزاری مفید)", "مفید کارت", "neobank", null, listOf(), listOf("مفید", "مفید کارت", "کارگزاری مفید")),
        IranianBank("agah", "کارت آگاه (بورس)", "آگاه", "neobank", null, listOf(), listOf("آگاه", "بورس")),
        IranianBank("ansar", "بانک انصار (سپه)", "انصار", "institute", "Ansar.svg", listOf("627381"), listOf("انصار")),
        IranianBank("ghavamin", "بانک قوامین (سپه)", "قوامین", "institute", "Ghavamin.svg", listOf("639599"), listOf("قوامین")),
        IranianBank("hekmat", "بانک حکمت ایرانیان (سپه)", "حکمت", "institute", "Hekmat.svg", listOf("636949"), listOf("حکمت")),
        IranianBank("kosar", "موسسه اعتباری کوثر (سپه)", "کوثر", "institute", "Kosar.svg", listOf("505801"), listOf("کوثر")),
        IranianBank("melall", "موسسه اعتباری ملل (عسکریه)", "ملل", "institute", "Melall.svg", listOf("606256"), listOf("ملل", "عسکریه")),
        IranianBank("caspian", "موسسه اعتباری کاسپین", "کاسپین", "institute", "Caspian.svg", listOf(), listOf("کاسپین")),
        IranianBank("noor", "موسسه اعتباری نور", "نور", "institute", "Noor.svg", listOf(), listOf("نور")),
        IranianBank("bank_markazi", "بانک مرکزی جمهوری اسلامی ایران", "بانک مرکزی", "state", "Bank_Markazi.svg", listOf(), listOf("مرکزی", "بانک مرکزی", "cbi")),
        IranianBank("cash", "کیف پول نقدی / صندوق", "صندوق نقدی", "other", null, listOf(), listOf("نقد", "کیف پول", "صندوق", "پول نقد")),
        IranianBank("crypto", "کیف پول رمزارز / تتر / صرافی", "رمزارز", "other", null, listOf(), listOf("تتر", "بیتکوین", "رمز", "ارز دیجیتال", "کریپتو", "نوبیتکس", "والکس")),
    )

    val byId: Map<String, IranianBank> = all.associateBy { it.id }

    fun findByCardNumber(cardNumber: String?): IranianBank? {
        if (cardNumber.isNullOrEmpty()) return null
        val clean = cardNumber.filter { it in '0'..'9' }
        if (clean.length < 6) return null
        val bin = clean.substring(0, 6)
        return all.firstOrNull { it.cardPrefixes.contains(bin) }
    }

    /** Name search with the same rules as findBankByQuery (id equal, name/shortName contains, keyword either way). */
    fun findByQuery(query: String?): IranianBank? {
        if (query.isNullOrEmpty()) return null
        val q = query.trim().lowercase()
        return all.firstOrNull { b ->
            b.id == q || b.name.lowercase().contains(q) || b.shortName.lowercase().contains(q) ||
                b.keywords.any { kw -> q.contains(kw) || kw.contains(q) }
        }
    }
}
