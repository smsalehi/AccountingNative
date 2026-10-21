package com.myaccounting.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.ui.theme.toTopRightBrush

enum class Tab(val label: String, val icon: Int) {
    ACCOUNTS("حساب‌ها", R.drawable.ic_lucide_credit_card),
    TRANSACTIONS("تراکنش‌ها", R.drawable.ic_lucide_receipt_text),
    PERSONS("اشخاص", R.drawable.ic_lucide_users),
    DEPTS("وام‌ها", R.drawable.ic_lucide_layers),
    SETTINGS("تنظیمات", R.drawable.ic_lucide_settings),
}

/** Port of components/Navbar.tsx (mobile layout: logo + date on the start side, bell / settings / theme on the end side). */
@Composable
fun AppHeader(
    isDark: Boolean,
    urgentCount: Int,
    onBell: () -> Unit,
    onSettings: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val bw = cssBorder()
    val chipShape = RoundedCornerShape(rem(0.75f))
    val dateText = remember { JalaliDate.getShortJalaliDate(System.currentTimeMillis()) }

    Column(Modifier.fillMaxWidth().background(dk(Tw.White, Tw.Slate900))) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .height(rem(4f)).padding(horizontal = rem(1f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // logo + date chip
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f)), verticalAlignment = Alignment.CenterVertically) {
                val logoCorner = rem(1f)
                Box(
                    Modifier.size(rem(2.5f)).cssShadow(TwShadow.md(Tw.Sky500.a(0.25f)), logoCorner)
                        .drawBehind {
                            drawRoundRect(
                                toTopRightBrush(size, listOf(Tw.Sky600, Tw.Indigo600, Tw.Teal500)),
                                cornerRadius = CornerRadius(logoCorner.toPx()),
                            )
                        }
                        .padding(rem(0.375f)),
                ) { Image(painterResource(R.drawable.logo_app), null, Modifier.fillMaxSize()) }

                Row(
                    Modifier.background(dk(Tw.Slate100.a(0.8f), Tw.Slate800.a(0.8f)), chipShape)
                        .border(bw, dk(Tw.Slate200.a(0.7f), Tw.Slate700.a(0.6f)), chipShape)
                        .padding(horizontal = rem(0.75f) + bw, vertical = rem(0.375f) + bw),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_calendar, 1f, dk(Tw.Sky600, Tw.Sky400))
                    TwText(dateText, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Bold, trackingEm = -0.025f, maxLines = 1)
                }
            }

            // bell / settings / theme
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f)), verticalAlignment = Alignment.CenterVertically) {
                // the badge is `absolute`: it must not take part in the button's layout, so it is a sibling of the padded icon
                Box(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onBell)) {
                    Box(Modifier.padding(rem(0.5f) + bw)) { LucideIcon(R.drawable.ic_lucide_bell, 1f, dk(Tw.Slate600, Tw.Slate300)) }
                    if (urgentCount > 0) {
                        Box(
                            Modifier.align(Alignment.TopStart) // physical right edge in RTL
                                // `absolute -top-1 -right-1` is relative to the PADDING box: the 1px transparent border sits outside it
                                .absoluteOffset(x = rem(0.25f) - bw, y = -rem(0.25f) + bw)
                                .defaultMinSize(minWidth = 18.dp)
                                .height(rem(1f))
                                .background(Tw.Rose600, CircleShape)
                                .padding(horizontal = rem(0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            TwText(JalaliDate.toPersianDigits(urgentCount), 0.625f, 0.9375f, Tw.White, weight = FW.Black, maxLines = 1)
                        }
                    }
                }

                Row(
                    Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onSettings)
                        .background(dk(Tw.Slate100, Tw.Slate800), chipShape)
                        .border(bw, dk(Tw.Slate200.a(0.8f), Tw.Slate700), chipShape)
                        .padding(horizontal = rem(0.75f) + bw, vertical = rem(0.375f) + bw),
                    verticalAlignment = Alignment.CenterVertically,
                ) { LucideIcon(R.drawable.ic_lucide_settings, 1f, dk(Tw.Slate500, Tw.Slate400)) }

                Box(
                    Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onToggleTheme)
                        .padding(rem(0.5f) + bw),
                ) {
                    if (isDark) LucideIcon(R.drawable.ic_lucide_sun, 1f, Tw.Amber400)
                    else LucideIcon(R.drawable.ic_lucide_moon, 1f, Tw.Slate600)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(bw).background(dk(Tw.Slate200.a(0.8f), Tw.Slate800)))
    }
}

/** Port of the mobile bottom bar in components/Navigation.tsx (4 tabs + centre FAB). */
@Composable
fun BottomNav(
    active: Tab,
    deptsAttention: Int,
    onTab: (Tab) -> Unit,
    onFab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val none = remember { MutableInteractionSource() }

    @Composable
    fun TabButton(tab: Tab) {
        val isActive = active == tab
        val color = if (isActive) dk(Tw.Sky600, Tw.Sky400) else dk(Tw.Slate500, Tw.Slate400)
        Box(Modifier.clickable(none, null) { onTab(tab) }) {
            Column(
                Modifier.padding(rem(0.25f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(rem(0.125f)),
            ) {
                LucideIcon(tab.icon, 1.25f, color)
                TwText(tab.label, 0.625f, 0.9375f, color, weight = if (isActive) FW.Bold else FW.Medium, maxLines = 1)
            }
            if (tab == Tab.DEPTS && deptsAttention > 0) {
                Box(
                    Modifier.align(Alignment.TopStart) // physical right edge in RTL: `right-1`
                        .absoluteOffset(x = -rem(0.25f))
                        .size(rem(0.5f)).background(Tw.Rose500, CircleShape),
                )
            }
        }
    }

    Column(modifier.fillMaxWidth().background(dk(Tw.White, Tw.Slate900))) {
        Box(Modifier.fillMaxWidth().height(cssBorder()).background(dk(Tw.Slate200, Tw.Slate800)))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = rem(0.25f), vertical = rem(0.375f)),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabButton(Tab.ACCOUNTS)
            TabButton(Tab.TRANSACTIONS)
            // FAB: w-11 h-11, -mt-2 (the margin shifts it up by half of 0.5rem inside the centred row)
            Box(
                Modifier.offset(y = -rem(0.25f)).size(rem(2.75f))
                    .cssShadow(TwShadow.lg(Tw.Sky500.a(0.3f)), rem(1.375f))
                    .drawBehind {
                        drawRoundRect(toTopRightBrush(size, listOf(Tw.Sky600, Tw.Indigo600)), cornerRadius = CornerRadius(size.minDimension / 2f))
                    }
                    .clickable(none, null, onClick = onFab),
                contentAlignment = Alignment.Center,
            ) { LucideIcon(R.drawable.ic_lucide_plus, 1.25f, Tw.White) }
            TabButton(Tab.PERSONS)
            TabButton(Tab.DEPTS)
        }
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)))
    }
}
