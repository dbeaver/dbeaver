/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.messages.ModelMessages;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.StringJoiner;

/**
 * Formats a duration in a human-readable form.
 * <p>
 * The following formats are supported:
 * <ul>
 *     <li>{@link DurationFormat#SHORT} - Short duration style.
 *     For example, the format might be '2m 56s', or '1.5s'/</li>
 *     <li>{@link DurationFormat#MEDIUM} - Medium duration style, similar to short.
 *     For example, the format might be '2m 56s', or '1.512s'.</li>
 *     <li>{@link DurationFormat#LONG} - Long duration style.
 *     For example, the format might be '1 hour, 30 minutes, 5 seconds, 100 milliseconds'.</li>
 * </ul>
 */
public final class DurationFormatter {
    private static final Format LONG_HOURS_FORMAT = new MessageFormat(ModelMessages.duration_formatter_hours);
    private static final Format LONG_MINUTES_FORMAT = new MessageFormat(ModelMessages.duration_formatter_minutes);
    private static final Format LONG_SECONDS_FORMAT = new MessageFormat(ModelMessages.duration_formatter_seconds);
    private static final Format LONG_MILLISECONDS_FORMAT = new MessageFormat(ModelMessages.duration_formatter_milliseconds);
    private static final Format SHORT_SECONDS_FORMAT = new DecimalFormat("#.#s");
    private static final Format MEDIUM_SECONDS_FORMAT = new DecimalFormat("#.###s");

    private DurationFormatter() {
    }

    @NotNull
    public static String format(@NotNull Duration duration, @NotNull DurationFormat format) {
        return switch (format) {
            case LONG -> formatLong(duration);
            case MEDIUM -> formatShortOrMedium(duration, true);
            case SHORT -> formatShortOrMedium(duration, false);
        };
    }

    @NotNull
    private static String formatShortOrMedium(@NotNull Duration duration, boolean medium) {
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();
        int millis = duration.toMillisPart();

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else if (seconds >= 10) {
            return String.format("%ds", seconds);
        } else {
            var format = medium ? MEDIUM_SECONDS_FORMAT : SHORT_SECONDS_FORMAT;
            return format.format(seconds + millis * 0.001);
        }
    }

    @NotNull
    private static String formatLong(@NotNull Duration duration) {
        StringJoiner joiner = new StringJoiner(", ");

        long hours = duration.toHours();
        if (hours > 0) {
            joiner.add(LONG_HOURS_FORMAT.format(new Object[]{hours}));
        }

        int minutes = duration.toMinutesPart();
        if (minutes > 0) {
            joiner.add(LONG_MINUTES_FORMAT.format(new Object[]{minutes}));
        }

        int seconds = duration.toSecondsPart();
        if (seconds > 0) {
            joiner.add(LONG_SECONDS_FORMAT.format(new Object[]{seconds}));
        }

        if (joiner.length() == 0) {
            joiner.add(LONG_MILLISECONDS_FORMAT.format(new Object[]{duration.toMillis()}));
        }

        return joiner.toString();
    }
}
