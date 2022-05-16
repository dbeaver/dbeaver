/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimezoneUtils {

    private TimezoneUtils() {

    }

    @NotNull
    public static List<String> getTimezoneNames() {
        return Arrays.stream(TimeZone.getAvailableIDs()).sorted((o1, o2) -> {
            final int firstOffset = TimeZone.getTimeZone(o1).getRawOffset();
            final int secondOffset = TimeZone.getTimeZone(o2).getRawOffset();
            return Integer.compare(firstOffset, secondOffset);
        }).map(TimezoneUtils::addGMTTime).collect(Collectors.toList());
    }

    public static String addGMTTime(@NotNull String availableID) {
        String result;
        long hours = TimeUnit.MILLISECONDS.toHours(TimeZone.getTimeZone(availableID).getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(TimeZone.getTimeZone(availableID).getRawOffset())
            - TimeUnit.HOURS.toMinutes(hours);
        minutes = Math.abs(minutes);
        String hoursWithSymbol;
        if (hours < 0) {
            hoursWithSymbol = Long.toString(hours);
        } else {
            hoursWithSymbol = '+' +Long.toString(hours);
        }
        result = String.format("%s (GMT%s:%02d)", availableID, hoursWithSymbol, minutes);
        return result;
    }


    public static String extractTimezoneId(@NotNull String timezone) {
        return timezone.split(" ")[0];
    }

}
