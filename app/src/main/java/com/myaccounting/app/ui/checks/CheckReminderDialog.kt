package com.myaccounting.app.ui.checks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.myaccounting.app.R
import com.myaccounting.app.data.CheckReminders
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Notifications
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwCheckbox
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwSelectField
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/**
 * ChecksView.tsx ReminderSettingsModal (ModalShell): enabled toggle, reminder hour and the
 * exact-alarm warning. Prefs changes are persisted by the caller and rescheduling happens
 * app-wide on every change.
 */
@Composable
fun CheckReminderDialog(
    prefs: Notifications.CheckReminderPrefs,
    onChange: (Notifications.CheckReminderPrefs) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    // isExactAlarmGranted(): null = unknown, false = missing
    val exactGranted = remember { CheckReminders.canScheduleExact(context) }

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 28f,
        maxHeightFraction = 0.92f,
        shapeRem = 1f,
        scrimColor = Tw.Slate900.a(0.6f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = rem(0.75f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                TwText("یادآوری چک‌ها", 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                TwText("نوتیفیکیشن اندروید؛ بدون نیاز به اینترنت", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400, iconRem = 1.25f)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = rem(1f)),
            verticalArrangement = Arrangement.spacedBy(rem(1f)),
        ) {
            // یادآوری فعال باشد + checkbox (accent-amber-600)
            Row(
                Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) { onChange(prefs.copy(enabled = !prefs.enabled)) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwText("یادآوری فعال باشد", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Bold, maxLines = 1)
                TwCheckbox(checked = prefs.enabled) { onChange(prefs.copy(enabled = !prefs.enabled)) }
            }

            Column {
                Box(Modifier.fillMaxWidth().padding(bottom = rem(0.25f))) {
                    TwText("ساعت یادآوری:", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
                }
                TwSelectField(
                    selectedUid = prefs.hour.toString(),
                    options = (0..23).map { h -> h.toString() to (JalaliDate.toPersianDigits(h.toString().padStart(2, '0')) + ":۰۰") },
                    placeholder = "انتخاب ساعت",
                    onSelect = { it.toIntOrNull()?.let { h -> onChange(prefs.copy(hour = h)) } },
                )
                TwText(
                    "برای هر چک ۳ روز قبل، ۱ روز قبل و روز مهلت یادآوری می‌آید. مهلت چک خودت = سررسید و برای چک اشخاص = مهلت واریز.",
                    0.6875f, 1.25f, Tw.Slate400,
                    modifier = Modifier.fillMaxWidth().padding(top = rem(0.375f)),
                )
            }

            if (exactGranted == false) {
                val boxShape = RoundedCornerShape(rem(0.75f))
                Column(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.Amber50, Tw.Amber950.a(0.4f)), boxShape)
                        .border(cssBorder(), dk(Tw.Amber200, Tw.Amber900), boxShape)
                        .padding(rem(0.75f)),
                    verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
                ) {
                    TwText(
                        "مجوز «آلارم دقیق» فعال نیست؛ یادآوری‌ها ممکن است با کمی تأخیر برسند.",
                        0.6875f, 1.25f, dk(Tw.Amber800, Tw.Amber300),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        Modifier.background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                            .clickable(remember { MutableInteractionSource() }, null) { CheckReminders.openExactAlarmSettings(context) }
                            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
                        horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LucideIcon(R.drawable.ic_lucide_calendar, 0.875f, Tw.White)
                        TwText("باز کردن تنظیمات", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                    }
                }
            }

            TwText(
                "اگر گوشی (مثل شیائومی یا سامسونگ) نوتیفیکیشن‌ها را دیر می‌رساند، در تنظیمات گوشی برنامه را از «بهینه‌سازی باتری» مستثنی کن.",
                0.6875f, 1.25f, Tw.Slate400,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
