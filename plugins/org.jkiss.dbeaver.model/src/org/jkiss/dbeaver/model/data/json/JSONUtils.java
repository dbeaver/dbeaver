/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.*;

/**
 * JSON utils
 */
public class JSONUtils {

    public static final String DEFAULT_INDENT = "\t";
    public static final String EMPTY_INDENT = "";
    private static final Log log = Log.getLog(JSONUtils.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd['T'HH:mm:ss['.'SSS]['Z']]")
        .withZone(ZoneId.of("UTC"));
    public static final Type MAP_TYPE_TOKEN = new TypeToken<Map<String, Object>>() {}.getType();
    public static final Gson GSON = new GsonBuilder().create();

    public static String formatDate(Date date) {
        try {
            if (date instanceof java.sql.Time) {
                return DateTimeFormatter.ISO_TIME.format(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.of("UTC")));
            } else if (date instanceof java.sql.Date) {
                return DateTimeFormatter.ISO_DATE.format(((java.sql.Date) date).toLocalDate());
            } else {
                return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")).format(DATE_TIME_FORMATTER);
            }
        } catch (Exception ex) {
            log.warn("Error formatting date to ISO-8601. Falling back to default string representation of " + date.getClass().getName(), ex);
            return date.toString();
        }
    }

    @Nullable
    public static Date parseDate(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer || value instanceof Long) {
            return new Date(((Number) value).longValue());
        }
        if (value instanceof String) {
            final TemporalAccessor accessor = DATE_TIME_FORMATTER.parse((String) value);
            final LocalDate localDate = accessor.query(TemporalQueries.localDate());
            final LocalTime localTime = accessor.query(TemporalQueries.localTime());
            if (localTime != null) {
                return Date.from(LocalDateTime.of(localDate, localTime).toInstant(ZoneOffset.UTC));
            } else {
                return Date.from(localDate.atStartOfDay().toInstant(ZoneOffset.UTC));
            }
        }
        throw new IllegalArgumentException("Cannot parse date from value '" + value + "'");
    }

    public static String formatISODate(Date date) {
        return "ISODate('" + formatDate(date) + "')";  //$NON-NLS-1$//$NON-NLS-2$
    }

    public static String escapeJsonString(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '\f' -> result.append("\\f");
                case '\b' -> result.append("\\b");
                case '"', '\\', '/' -> result.append("\\").append(c);
                default -> {
                    if ((int) c < 32) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        return result.toString();
    }

    @NotNull
    public static JsonWriter field(@NotNull JsonWriter json, @NotNull String name, @Nullable String value) throws IOException {
        json.name(name);
        if (value == null) json.nullValue(); else json.value(value);
        return json;
    }

    @NotNull
    public static JsonWriter field(@NotNull JsonWriter json, @NotNull String name, @Nullable Number value) throws IOException {
        json.name(name);
        if (value == null) json.nullValue(); else json.value(value);
        return json;
    }

    @NotNull
    public static JsonWriter fieldNE(@NotNull JsonWriter json, @NotNull String name, @Nullable String value) throws IOException {
        if (CommonUtils.isEmpty(value)) {
            return json;
        }
        json.name(name);
        json.value(value);
        return json;
    }

    @NotNull
    public static JsonWriter field(@NotNull JsonWriter json, @NotNull String name, long value) throws IOException {
        json.name(name);
        json.value(value);
        return json;
    }

    @NotNull
    public static JsonWriter field(@NotNull JsonWriter json, @NotNull String name, double value) throws IOException {
        json.name(name);
        json.value(value);
        return json;
    }

    @NotNull
    public static JsonWriter field(@NotNull JsonWriter json, @NotNull String name, boolean value) throws IOException {
        json.name(name);
        json.value(value);
        return json;
    }

    public static void serializeStringList(@NotNull JsonWriter json, @NotNull String tagName, @Nullable Collection<String> list) throws IOException {
        serializeStringList(json, tagName, list, true, false);
    }

    public static void serializeStringList(
        @NotNull JsonWriter json,
        @NotNull String tagName,
        @Nullable Collection<String> list,
        boolean compact,
        boolean force
    ) throws IOException {
        if (force || !CommonUtils.isEmpty(list)) {
            json.name(tagName);
            json.beginArray();
            if (compact) json.setIndent(EMPTY_INDENT);
            for (String include : CommonUtils.safeCollection(list)) {
                json.value(include);
            }
            json.endArray();
            if (compact) json.setIndent(DEFAULT_INDENT);
        }
    }

    public static void serializeObjectList(@NotNull JsonWriter json, @NotNull String tagName, @Nullable Collection<?> list) throws IOException {
        if (!CommonUtils.isEmpty(list)) {
            json.name(tagName);
            serializeCollection(json, list);
        }
    }

    public static void serializeProperties(@NotNull JsonWriter json, @NotNull String tagName, @Nullable Map<String, ?> properties) throws IOException {
        if (!CommonUtils.isEmpty(properties)) {
            json.name(tagName);
            serializeMap(json, properties);
        }
    }

    public static void serializeProperties(
        @NotNull JsonWriter json,
        @NotNull String tagName,
        @Nullable Map<String, ?> properties, boolean allowEmptyValues) throws IOException
    {
        if (!CommonUtils.isEmpty(properties)) {
            json.name(tagName);
            serializeMap(json, properties, allowEmptyValues);
        }
    }

    public static void serializeCollection(@NotNull JsonWriter json, @NotNull Collection<?> list) throws IOException {
        json.beginArray();
        for (Object value : CommonUtils.safeCollection(list)) {
            if (value == null) {
                json.nullValue();
            } else if (value instanceof Number numberValue) {
                json.value(numberValue);
            } else if (value instanceof Boolean boolValue) {
                json.value(boolValue);
            } else if (value instanceof String strValue) {
                json.value(strValue);
            } else if (value instanceof Map mapValue) {
                serializeMap(json, mapValue);
            } else if (value instanceof Collection colValue) {
                serializeCollection(json, colValue);
            } else {
                json.value(value.toString());
            }
        }
        json.endArray();
    }

    public static void serializeMap(@NotNull JsonWriter json, @NotNull Map<String, ?> map) throws IOException {
        serializeMap(json, map, false);
    }

    public static void serializeMap(@NotNull JsonWriter json, @NotNull Map<String, ?> map,
                                    boolean allowsEmptyValue) throws IOException {
        json.beginObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object propValue = entry.getValue();
            String fieldName = entry.getKey();
            if (propValue == null) {
                //field(json, fieldName, (String)null);
                //continue;
            } else if (propValue instanceof Number) {
                field(json, fieldName, (Number)propValue);
            } else if (propValue instanceof String strValue) {
                if (!strValue.isEmpty()) {
                    field(json, fieldName, strValue);
                } else if (allowsEmptyValue) {
                    field(json, fieldName, strValue);
                }
            } else if (propValue instanceof Boolean bool) {
                field(json, fieldName, bool);
            } else if (propValue instanceof Collection collectionValue) {
                serializeObjectList(json, fieldName, collectionValue);
            } else if (propValue instanceof Map mapValue) {
                serializeProperties(json, fieldName, mapValue, allowsEmptyValue);
            } else if (propValue instanceof Enum anEnum) {
                field(json, fieldName, anEnum.name());
            } else if (propValue instanceof URI uri) {
                field(json, fieldName, uri.toString());
            } else {
                log.debug("Unsupported JSON property '" + fieldName + "' type: " + propValue.getClass().getName() +
                    ". Serializing as string.");
                field(json, fieldName, propValue.toString());
            }
        }
        json.endObject();
    }

    public static <OBJECT_TYPE> OBJECT_TYPE deserializeObject(Map<String, Object> map, @NotNull Class<OBJECT_TYPE> type) throws DBCException {
        String json = GSON.toJson(map);
        return GSON.fromJson(json, type);
    }

    @NotNull
    public static Map<String, Object> parseMap(@NotNull Gson gson, @NotNull Reader reader) {
        Map<String, Object> result = gson.fromJson(reader, MAP_TYPE_TOKEN);
        if (result == null) {
            return new LinkedHashMap<>();
        }
        return result;
    }

    @NotNull
    public static Map<String, Object> getObject(@NotNull Map<String, Object> map, @NotNull String name) {
        Map<String, Object> object = (Map<String, Object>) map.get(name);
        return Objects.requireNonNullElseGet(object, LinkedHashMap::new);
    }

    @Nullable
    public static Map<String, Object> getObjectOrNull(@NotNull Map<String, Object> map, @NotNull String name) {
        return (Map<String, Object>) map.get(name);
    }

    @NotNull
    public static Iterable<Map.Entry<String, Map<String, Object>>> getNestedObjects(@NotNull Map<String, Object> map, @NotNull String name) {
        Map<String, Map<String, Object>> object = (Map<String, Map<String, Object>>) map.get(name);
        if (object == null) {
            return new ArrayList<>();
        } else {
            return object.entrySet();
        }
    }

    public static <T> T getObjectProperty(Object object, String name) {
        if (object instanceof Map map) {
            return (T) map.get(name);
        }
        log.error("Object " + object + " is not map");
        return null;
    }

    public static String getString(Map<String, Object> map, String name) {
        Object value = map.get(name);
        return value == null ? null : value.toString();
    }

    public static String getString(Map<String, Object> map, String name, String defValue) {
        Object value = map.get(name);
        return value == null ? defValue : value.toString();
    }

    /**
     * Returns timestamp value from the attributes map, if map contains key
     *
     * @param attributes Attributes map
     * @param name Name of the attribute
     * @return timestamp from the given string value
     */
    @NotNull
    public static Timestamp getTimestamp(@NotNull Map<String, Object> attributes, @NotNull String name) {
        if (attributes.containsKey(name)) {
            try {
                long inst = getLong(attributes, name, 0);
                if (inst != 0) {
                    return Timestamp.from(Instant.ofEpochMilli(inst));
                }
            } catch (Exception e) {
                log.debug("Can't parse timestamp value from " + name);
            }
        }
        return new Timestamp(0);
    }

    public static boolean getBoolean(Map<String, Object> map, String name) {
        return CommonUtils.toBoolean(map.get(name));
    }

    public static boolean getBoolean(Map<String, Object> map, String name, boolean defaultValue) {
        return CommonUtils.getBoolean(map.get(name), defaultValue);
    }

    public static int getInteger(Map<String, Object> map, String name) {
        return CommonUtils.toInt(map.get(name));
    }

    public static int getInteger(Map<String, Object> map, String name, int defaultValue) {
        return CommonUtils.toInt(map.get(name), defaultValue);
    }

    public static long getLong(Map<String, Object> map, String name, long defaultValue) {
        return CommonUtils.toLong(map.get(name), defaultValue);
    }

    public static Double getDouble(@NotNull Map<String, Object> map, String name) {
        return CommonUtils.toDouble(map.get(name));
    }

    @NotNull
    public static List<Map<String, Object>> getObjectList(@NotNull Map<String, Object> map, @NotNull String name) {
        Object value = map.get(name);
        if (value instanceof List list) {
            return  (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }

    @NotNull
    public static List<String> getStringList(@NotNull Map<String, Object> map, @NotNull String name) {
        Object value = map.get(name);
        if (value instanceof List list) {
            return  (List<String>) list;
        }
        return Collections.emptyList();
    }

    @Nullable
    public static Map<String, Object> deserializeProperties(Map<String, Object> map, String name) {
        Object propMap = map.get(name);
        if (propMap instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?,?> pe : ((Map<?, ?>) propMap).entrySet()) {
                result.put(CommonUtils.toString(pe.getKey()), pe.getValue());
            }
            return result;
        } else {
            return null;
        }
    }

    @NotNull
    public static Map<String, String> deserializeStringMap(Map<String, Object> map, String name) {
        Map<String, String> result = new LinkedHashMap<>();
        Object propMap = map.get(name);
        if (propMap instanceof Map) {
            for (Map.Entry<?,?> pe : ((Map<?, ?>) propMap).entrySet()) {
                result.put(CommonUtils.toString(pe.getKey()), CommonUtils.toString(pe.getValue()));
            }
        }
        return result;
    }

    @Nullable
    public static Map<String, String> deserializeStringMapOrNull(Map<String, Object> map, String name) {
        Object propMap = map.get(name);
        if (propMap instanceof Map mapVal && !mapVal.isEmpty()) {
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<?,?> pe : ((Map<?, ?>) propMap).entrySet()) {
                result.put(CommonUtils.toString(pe.getKey()), CommonUtils.toString(pe.getValue()));
            }
            return result;
        }
        return null;
    }

    @NotNull
    public static List<String> deserializeStringList(Map<String, Object> map, String name) {
        List<String> result = new ArrayList<>();
        Object propMap = map.get(name);
        if (propMap instanceof Collection colValue) {
            for (Object pe : colValue) {
                result.add(CommonUtils.toString(pe));
            }
        }
        return result;
    }

}
