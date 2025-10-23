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
package org.jkiss.dbeaver.registry.task;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.StandardConstants;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TaskUtils {
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm['Z']")
        .withZone(StandardConstants.ZONE_ID_UTC);

    private static final Log log = Log.getLog(TaskUtils.class);
    static final String RUN_LOG_PREFIX = "run_";
    static final String RUN_LOG_EXT = "log";


    public static List<TaskRunImpl> loadRunStatistics(Path metaFile, Gson gson) {
        if (!Files.exists(metaFile)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(metaFile)) {
            var statistics = gson.fromJson(reader, RunStatistics.class);
            if (statistics == null) {
                log.error("Null task run statistics returned");
                return new ArrayList<>();
            }
            return statistics.getRuns();
        } catch (Exception e) {
            log.error("Error reading task run statistics", e);
            return new ArrayList<>();
        }
    }

    @NotNull
    public static ZonedDateTime parseDateTime(@NotNull String text) {
        var accessor = DATE_TIME_FORMATTER.parseBest(text, ZonedDateTime::from, LocalDateTime::from);
        if (accessor instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault());
        } else {
            return (ZonedDateTime) accessor;
        }
    }

    @NotNull
    public static String formatDateTime(@NotNull ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(DATE_TIME_FORMATTER);
    }

    public static String buildRunLogFileName(String runId) {
        return RUN_LOG_PREFIX + runId + "." + RUN_LOG_EXT;
    }

    @NotNull
    public static String formatDisplayDateTime(@Nullable ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return "N/A";
        }
        return DISPLAY_DATE_TIME_FORMATTER.format(toSystemZonedDateTime(zonedDateTime));
    }

    @NotNull
    public static ZonedDateTime toSystemZonedDateTime(@NotNull ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
    }

    /**
     * An adapter that conveniently wraps {@link #parseDateTime(String)} and {@link #formatDateTime(ZonedDateTime)}
     * helper methods for use with Gson.
     */
    public static final class ZonedDateTimeAdapter extends TypeAdapter<ZonedDateTime> {
        @Override
        public void write(JsonWriter out, ZonedDateTime value) throws IOException {
            out.value(formatDateTime(value));
        }

        @Override
        public ZonedDateTime read(JsonReader in) throws IOException {
            return parseDateTime(in.nextString());
        }
    }
}
