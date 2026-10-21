package com.myaccounting.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.myaccounting.app.R
import com.myaccounting.app.domain.IranianBanks
import com.myaccounting.app.ui.theme.LocalIsDark
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/** CSS `dark:brightness-125 dark:contrast-125` = c' = (c·1.25 − 0.5)·1.25 + 0.5 = 1.5625·c − 0.125 on each colour channel. */
private val DarkLogoFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            1.5625f, 0f, 0f, 0f, -31.875f,
            0f, 1.5625f, 0f, 0f, -31.875f,
            0f, 0f, 1.5625f, 0f, -31.875f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/** Port of components/BankLogo.tsx, size "md" (w-9 h-9 p-1.5, rounded-xl). */
@Composable
fun BankLogo(bankName: String?, bankUid: String?) {
    val name = (bankName ?: "").trim()
    fun match(term: String) = name.lowercase().contains(term.lowercase())

    val shape = RoundedCornerShape(rem(0.75f))
    val bw = cssBorder()
    val sizeRem = 2.25f
    val padRem = 0.375f

    @Composable
    fun Solid(bg: Color, @DrawableRes res: Int) {
        Box(
            Modifier.size(rem(sizeRem)).cssShadow(TwShadow.xs(), rem(0.75f)).clip(shape).background(bg).padding(rem(padRem)),
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
    }

    @Composable
    fun Tinted(bg: Color, border: Color, tint: Color, @DrawableRes res: Int) {
        Box(
            Modifier.size(rem(sizeRem)).cssShadow(TwShadow.xs(), rem(0.75f)).background(bg, shape).border(bw, border, shape).clip(shape)
                .padding(rem(padRem) + bw),
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(tint)) }
    }

    @Composable
    fun FileLogo(@DrawableRes res: Int) {
        Box(
            Modifier.size(rem(sizeRem)).cssShadow(TwShadow.xs(), rem(0.75f))
                .background(dk(Tw.White, Tw.Slate800), shape)
                .border(bw, dk(Tw.Slate200.a(0.8f), Tw.Slate700.a(0.8f)), shape)
                .clip(shape).padding(rem(padRem) + bw),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                colorFilter = if (LocalIsDark.current) DarkLogoFilter else null,
            )
        }
    }

    val explicit = bankUid?.let { IranianBanks.byId[it] }
    val explicitRes = explicit?.file?.let { BankLogoDrawables[it] }
    when {
        explicitRes != null -> FileLogo(explicitRes)
        match("مفید") -> Solid(Color(0xFF047857), R.drawable.bank_fb_mofid)
        match("آگاه") || match("بورس") -> Solid(Color(0xFF3730A3), R.drawable.bank_fb_agah)
        match("بلو") || match("blu") -> Solid(Color(0xFF0066FF), R.drawable.bank_fb_blu)
        match("ویپاد") || match("vpod") -> Solid(Color(0xFF059669), R.drawable.bank_fb_vpod)
        match("بانکینو") || match("bankino") -> Solid(Color(0xFF4F46E5), R.drawable.bank_fb_bankino)
        match("توبانک") || match("tobank") -> Solid(Color(0xFFDC2626), R.drawable.bank_fb_tobank)
        match("رمز") || match("تتر") || match("بیتکوین") || match("ارز دیجیتال") || match("crypto") || match("usdt") ->
            Tinted(Tw.Amber500.a(0.1f), Tw.Amber500.a(0.2f), dk(Tw.Amber600, Tw.Amber400), R.drawable.bank_fb_crypto)
        match("کیف") || match("نقد") || match("پول") || match("صندوق") ->
            Tinted(Tw.Emerald500.a(0.1f), Tw.Emerald500.a(0.2f), dk(Tw.Emerald600, Tw.Emerald400), R.drawable.bank_fb_cash)
        else -> {
            val byName = IranianBanks.findByQuery(name)
            val res = byName?.file?.let { BankLogoDrawables[it] }
            if (res != null) FileLogo(res)
            else Tinted(dk(Tw.Slate100, Tw.Slate800), dk(Tw.Slate200, Tw.Slate700), dk(Tw.Slate600, Tw.Slate300), R.drawable.bank_fb_generic)
        }
    }
}
