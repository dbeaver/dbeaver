/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.internal.AIChatMessages;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public final class AIChatTimeUtils {
    private AIChatTimeUtils() {
    }

    @NotNull
    public static String format(@NotNull LocalDateTime time, boolean includeToday) {
        return format(time, LocalDate.now(), Locale.getDefault(Locale.Category.FORMAT), includeToday);
    }

    @NotNull
    public static String format(
        @NotNull LocalDateTime time,
        @NotNull LocalDate today,
        @NotNull Locale locale,
        boolean includeToday
    ) {
        String clock = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale));
        LocalDate date = time.toLocalDate();
        if (date.equals(today)) {
            return includeToday ? AIChatMessages.ai_chat_date_today + ", " + clock : clock;
        }
        String day = date.equals(today.minusDays(1)) ? AIChatMessages.ai_chat_date_yesterday
            : date.format(DateTimeFormatter.ofPattern(date.getYear() == today.getYear() ? "d MMM" : "d MMM uuuu", locale));
        return day + ", " + clock;
    }
}
