/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * An adapter that support serialization and deserialization of {@link Date} objects that also
 * specifies system time zone for reliability
 */
public class GsonDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    public static final GsonDateAdapter INSTANCE = new GsonDateAdapter();
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm[Z]");

    private GsonDateAdapter() {
        // prevents instantiation
    }

    @Override
    public Date deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        final TemporalAccessor accessor = FORMATTER.parseBest(element.getAsString(), ZonedDateTime::from, LocalDateTime::from);
        if (accessor instanceof ZonedDateTime) {
            return Date.from(((ZonedDateTime) accessor).toInstant());
        } else {
            return Date.from(((LocalDateTime) accessor).atZone(ZoneId.systemDefault()).toInstant());
        }
    }

    @Override
    public JsonElement serialize(Date date, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault())));
    }
}
