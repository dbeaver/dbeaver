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
package org.jkiss.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.utils.DurationFormat;
import org.jkiss.dbeaver.utils.DurationFormatter;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.Locale;

public class DurationFormatterTest extends DBeaverUnitTest {
    private static Locale originalLocale;

    @BeforeClass
    public static void beforeClass() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterClass
    public static void afterClass() {
        Locale.setDefault(originalLocale);
        originalLocale = null;
    }

    @Test
    public void testFormatShort() {
        assertDurationEquals("1h 1m 1s", Duration.ofHours(1).plusMinutes(1).plusSeconds(1), DurationFormat.SHORT);
        assertDurationEquals("1h 1m 0s", Duration.ofHours(1).plusMinutes(1).plusMillis(500), DurationFormat.SHORT);
        assertDurationEquals("11h 41m 9s", Duration.ofSeconds(42069), DurationFormat.SHORT);
        assertDurationEquals("1s", Duration.ofSeconds(1), DurationFormat.SHORT);
        assertDurationEquals("1.5s", Duration.ofMillis(1500), DurationFormat.SHORT);
        assertDurationEquals("0.5s", Duration.ofMillis(500), DurationFormat.SHORT);
        assertDurationEquals("0s", Duration.ofMillis(25), DurationFormat.SHORT);
        assertDurationEquals("0s", Duration.ofMillis(1), DurationFormat.SHORT);
        assertDurationEquals("10s", Duration.ofSeconds(10), DurationFormat.SHORT);
        assertDurationEquals("1m 1s", Duration.ofMinutes(1).plusSeconds(1), DurationFormat.SHORT);
        assertDurationEquals("1m 0s", Duration.ofMinutes(1), DurationFormat.SHORT);
    }

    @Test
    public void testFormatMedium() {
        assertDurationEquals("1h 1m 1s", Duration.ofHours(1).plusMinutes(1).plusSeconds(1), DurationFormat.MEDIUM);
        assertDurationEquals("1h 1m 0s", Duration.ofHours(1).plusMinutes(1).plusMillis(500), DurationFormat.MEDIUM);
        assertDurationEquals("11h 41m 9s", Duration.ofSeconds(42069), DurationFormat.MEDIUM);
        assertDurationEquals("1s", Duration.ofSeconds(1), DurationFormat.MEDIUM);
        assertDurationEquals("1.512s", Duration.ofMillis(1512), DurationFormat.MEDIUM);
        assertDurationEquals("0.5s", Duration.ofMillis(500), DurationFormat.MEDIUM);
        assertDurationEquals("0.025s", Duration.ofMillis(25), DurationFormat.MEDIUM);
        assertDurationEquals("0.001s", Duration.ofMillis(1), DurationFormat.MEDIUM);
        assertDurationEquals("10s", Duration.ofSeconds(10), DurationFormat.MEDIUM);
        assertDurationEquals("1m 1s", Duration.ofMinutes(1).plusSeconds(1), DurationFormat.MEDIUM);
        assertDurationEquals("1m 0s", Duration.ofMinutes(1), DurationFormat.MEDIUM);
    }

    @Test
    public void testFormatLong() {
        assertDurationEquals("1 hour", Duration.ofHours(1), DurationFormat.LONG);
        assertDurationEquals("1 hour, 1 minute, 1 second", Duration.ofHours(1).plusMinutes(1).plusSeconds(1), DurationFormat.LONG);
        assertDurationEquals("1 hour, 1 minute", Duration.ofHours(1).plusMinutes(1).plusMillis(500), DurationFormat.LONG);
        assertDurationEquals("11 hours, 41 minutes, 9 seconds", Duration.ofSeconds(42069), DurationFormat.LONG);
        assertDurationEquals("1 second", Duration.ofSeconds(1), DurationFormat.LONG);
        assertDurationEquals("500 milliseconds", Duration.ofMillis(500), DurationFormat.LONG);
        assertDurationEquals("10 seconds", Duration.ofSeconds(10), DurationFormat.LONG);
        assertDurationEquals("1 minute, 1 second", Duration.ofMinutes(1).plusSeconds(1), DurationFormat.LONG);
        assertDurationEquals("1 minute", Duration.ofMinutes(1), DurationFormat.LONG);
    }

    private static void assertDurationEquals(@NotNull String expected, @NotNull Duration duration, @NotNull DurationFormat format) {
        Assert.assertEquals(expected, DurationFormatter.format(duration, format));
    }
}
