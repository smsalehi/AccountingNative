package com.myaccounting.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.myaccounting.app.R
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/** A preset subject icon (PRESET_CATEGORY_ICONS of CategoryIcon.tsx). */
data class PresetIcon(val id: String, val title: String, @DrawableRes val icon: Int, val lucide: String)

object CategoryIcons {
    val PRESETS = listOf(
        PresetIcon("home", "خانه و مسکن", R.drawable.ic_lucide_home, "home"),
        PresetIcon("utensils", "غذا و رستوران", R.drawable.ic_lucide_utensils, "utensils"),
        PresetIcon("coffee", "کافه و نوشیدنی", R.drawable.ic_lucide_coffee, "coffee"),
        PresetIcon("receipt", "قبوض و شارژ", R.drawable.ic_lucide_receipt, "receipt"),
        PresetIcon("shirt", "پوشاک و مد", R.drawable.ic_lucide_shirt, "shirt"),
        PresetIcon("car", "خودرو", R.drawable.ic_lucide_car, "car"),
        PresetIcon("fuel", "سوخت و بنزین", R.drawable.ic_lucide_fuel, "fuel"),
        PresetIcon("bus", "رفت‌وآمد و حمل‌ونقل", R.drawable.ic_lucide_bus, "bus"),
        PresetIcon("flight", "سفر و گردشگری", R.drawable.ic_lucide_plane, "plane"),
        PresetIcon("gamepad", "سرگرمی و بازی", R.drawable.ic_lucide_gamepad_2, "gamepad-2"),
        PresetIcon("film", "سینما و فیلم", R.drawable.ic_lucide_film, "film"),
        PresetIcon("graduation", "آموزش و تحصیل", R.drawable.ic_lucide_graduation_cap, "graduation-cap"),
        PresetIcon("book", "کتاب و پژوهش", R.drawable.ic_lucide_book_open, "book-open"),
        PresetIcon("health", "پزشکی و سلامت", R.drawable.ic_lucide_heart_pulse, "heart-pulse"),
        PresetIcon("pill", "دارو و درمان", R.drawable.ic_lucide_pill, "pill"),
        PresetIcon("sofa", "لوازم منزل", R.drawable.ic_lucide_sofa, "sofa"),
        PresetIcon("gadget", "موبایل و دیجیتال", R.drawable.ic_lucide_smartphone, "smartphone"),
        PresetIcon("fitness", "ورزش و باشگاه", R.drawable.ic_lucide_dumbbell, "dumbbell"),
        PresetIcon("gift", "کادو و هدیه", R.drawable.ic_lucide_gift, "gift"),
        PresetIcon("baby", "فرزند و کودک", R.drawable.ic_lucide_baby, "baby"),
        PresetIcon("shopping", "خرید و فروشگاه", R.drawable.ic_lucide_shopping_cart, "shopping-cart"),
        PresetIcon("salary", "حقوق و دستمزد", R.drawable.ic_lucide_banknote, "banknote"),
        PresetIcon("investment", "سود و سرمایه‌گذاری", R.drawable.ic_lucide_trending_up, "trending-up"),
        PresetIcon("piggy", "پس‌انداز", R.drawable.ic_lucide_piggy_bank, "piggy-bank"),
        PresetIcon("rent", "اجاره‌بها", R.drawable.ic_lucide_building, "building"),
        PresetIcon("coins", "یارانه و نقدی", R.drawable.ic_lucide_coins, "coins"),
        PresetIcon("repair", "تعمیرات و ابزار", R.drawable.ic_lucide_wrench, "wrench"),
        PresetIcon("internet", "اینترنت و مخابرات", R.drawable.ic_lucide_wifi, "wifi"),
        PresetIcon("other", "متفرقه", R.drawable.ic_lucide_sparkles, "sparkles"),
    )

    private val byId = PRESETS.associateBy { it.id }

    @DrawableRes
    fun drawable(iconId: String?): Int = byId[iconId]?.icon ?: 0

    /** `getCategoryIconComponent`: an explicit iconId wins, otherwise the Persian title decides. */
    @DrawableRes
    fun forSubject(title: String = "", iconId: String? = null): Int {
        if (!iconId.isNullOrEmpty()) byId[iconId]?.let { return it.icon }
        val t = title.lowercase().trim()
        fun has(vararg words: String) = words.any { t.contains(it) }
        return when {
            has("غذا", "خوراک", "سوپر") -> R.drawable.ic_lucide_utensils_crossed
            has("رستوران", "ناهار", "شام") -> R.drawable.ic_lucide_utensils
            has("کافی", "کافه", "قهوه") -> R.drawable.ic_lucide_coffee
            has("قبض", "شارژ", "آب", "برق", "گاز") -> R.drawable.ic_lucide_receipt
            has("پوشاک", "لباس", "کفش", "مد") -> R.drawable.ic_lucide_shirt
            has("خودرو", "ماشین", "موتور", "تعمیرگاه") -> R.drawable.ic_lucide_car
            has("سوخت", "بنزین", "گازوئیل") -> R.drawable.ic_lucide_fuel
            has("رفت", "اسنپ", "تپسی", "اتوبوس", "مترو", "تاکسی") -> R.drawable.ic_lucide_bus
            has("سفر", "بلیط", "هواپیما", "هتل") -> R.drawable.ic_lucide_plane
            has("سرگرمی", "تفریح", "بازی", "گیم") -> R.drawable.ic_lucide_gamepad_2
            has("فیلم", "سینما", "کنسرت") -> R.drawable.ic_lucide_film
            has("موسیقی", "ساز") -> R.drawable.ic_lucide_music
            has("آموزش", "تحصیل", "دانشگاه", "مدرسه", "کلاس") -> R.drawable.ic_lucide_graduation_cap
            has("کتاب", "دفتر", "تحریر") -> R.drawable.ic_lucide_book_open
            has("پزشک", "درمان", "بیمارستان", "دکتر", "سلامت") -> R.drawable.ic_lucide_heart_pulse
            has("دارو", "داروخانه") -> R.drawable.ic_lucide_pill
            has("لوازم منزل", "مبلمان", "فرش") -> R.drawable.ic_lucide_sofa
            has("تلویزیون", "لوازم برقی") -> R.drawable.ic_lucide_tv
            has("منزل", "خانه", "مسکن", "آپارتمان") -> R.drawable.ic_lucide_home
            has("ورزش", "باشگاه", "استخر") -> R.drawable.ic_lucide_dumbbell
            has("کودک", "فرزند", "نوزاد") -> R.drawable.ic_lucide_baby
            has("هدیه", "کادو", "پاداش") -> R.drawable.ic_lucide_gift
            has("حقوق", "دستمزد", "معاش") -> R.drawable.ic_lucide_banknote
            has("فروش", "مغازه", "کالا", "اموال") -> R.drawable.ic_lucide_shopping_bag
            has("سود", "سرمایه", "بورس", "سهام") -> R.drawable.ic_lucide_trending_up
            has("اجاره", "رهن", "مستاجر") -> R.drawable.ic_lucide_building
            has("یارانه", "معیشتی") -> R.drawable.ic_lucide_coins
            has("پس‌انداز", "قلک") -> R.drawable.ic_lucide_piggy_bank
            has("اینترنت", "بسته", "وای‌فای", "وایفای") -> R.drawable.ic_lucide_wifi
            has("تعمیر", "سرویس", "تاسیسات") -> R.drawable.ic_lucide_wrench
            else -> R.drawable.ic_lucide_tag
        }
    }
}

enum class CategoryIconSize(val box: Float, val pad: Float, val icon: Float) {
    XS(1.5f, 0.25f, 0.75f),     // w-6 h-6 p-1   — icon w-3
    SM(1.75f, 0.375f, 0.875f),  // w-7 h-7 p-1.5 — icon w-3.5
    MD(2f, 0.375f, 1f),         // w-8 h-8 p-1.5 — icon w-4
    LG(2.5f, 0.5f, 1.25f),      // w-10 h-10 p-2 — icon w-5
}

/**
 * CategoryIcon.tsx: `w-7 h-7 rounded-xl shadow-2xs` with the rose/emerald/sky tint of the direction
 * (isCost 1 = expense, 0 = income, null = neutral).
 */
@Composable
fun CategoryIcon(
    title: String = "",
    iconId: String? = null,
    isCost: Int? = null,
    size: CategoryIconSize = CategoryIconSize.MD,
    modifier: Modifier = Modifier,
) {
    val id = CategoryIcons.forSubject(title, iconId)
    val bg = when (isCost) {
        1 -> dk(Tw.Rose50, Tw.Rose950.a(0.5f))
        0 -> dk(Tw.Emerald50, Tw.Emerald950.a(0.5f))
        else -> dk(Tw.Sky50, Tw.Sky950.a(0.5f))
    }
    val fg = when (isCost) {
        1 -> dk(Tw.Rose600, Tw.Rose400)
        0 -> dk(Tw.Emerald600, Tw.Emerald400)
        else -> dk(Tw.Sky600, Tw.Sky400)
    }
    val border = when (isCost) {
        1 -> dk(Tw.Rose200.a(0.6f), Tw.Rose900.a(0.4f))
        0 -> dk(Tw.Emerald200.a(0.6f), Tw.Emerald900.a(0.4f))
        else -> dk(Tw.Sky200.a(0.6f), Tw.Sky900.a(0.4f))
    }
    val shape = RoundedCornerShape(rem(0.75f))
    Box(
        modifier.size(rem(size.box)).background(bg, shape).border(cssBorder(), border, shape).padding(rem(size.pad)),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(id, size.icon, fg)
    }
}

/** `text-rose-600 dark:text-rose-400` of the category rows. */
@Composable
fun categoryTint(isCost: Int): Color = if (isCost == 1) dk(Tw.Rose600, Tw.Rose400) else dk(Tw.Emerald600, Tw.Emerald400)
