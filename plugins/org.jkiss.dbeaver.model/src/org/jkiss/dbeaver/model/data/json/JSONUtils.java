/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.dbeaver.runtime.serialize.SerializerRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * JSON utils
 */
public class JSONUtils {

    private static final Log log = Log.getLog(JSONUtils.class);

    private static SimpleDateFormat dateFormat;

    static {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        dateFormat = new SimpleDateFormat(DBConstants.DEFAULT_ISO_TIMESTAMP_FORMAT);
        dateFormat.setTimeZone(tz);
    }

    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }

    public static Date parseDate(String str) {
        if (CommonUtils.isEmpty(str)) {
            return null;
        }
        try {
            return dateFormat.parse(str);
        } catch (ParseException e) {
            log.error("Error parsing date");
            return new Date(0L);
        }
    }

    public static String formatISODate(Date date) {
        return "ISODate(\"" + formatDate(date) + "\")";  //$NON-NLS-1$//$NON-NLS-2$
    }

    public static String escapeJsonString(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '"':
                case '\\':
                case '/':
                    result.append("\\").append(c);
                    break;
                default:
                    result.append(c);
                    break;
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
        if (!CommonUtils.isEmpty(list)) {
            json.name(tagName);
            json.beginArray();
            for (String include : CommonUtils.safeCollection(list)) {
                json.value(include);
            }
            json.endArray();
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

    public static void serializeCollection(@NotNull JsonWriter json, @NotNull Collection<?> list) throws IOException {
        json.beginArray();
        for (Object value : CommonUtils.safeCollection(list)) {
            if (value == null) {
                json.nullValue();
            } else if (value instanceof Number) {
                json.value((Number) value);
            } else if (value instanceof Boolean) {
                json.value((Boolean) value);
            } else if (value instanceof String) {
                json.value(value.toString());
            } else if (value instanceof Map) {
                serializeMap(json, (Map<String, ?>) value);
            } else if (value instanceof Collection) {
                serializeCollection(json, (Collection<?>) value);
            } else {
                json.value(value.toString());
            }
        }
        json.endArray();
    }

    public static void serializeMap(@NotNull JsonWriter json, @NotNull Map<String, ?> map) throws IOException {
        json.beginObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object propValue = entry.getValue();
            String fieldName = entry.getKey();
            if (propValue == null) {
                //field(json, fieldName, (String)null);
                //continue;
            } else if (propValue instanceof Number) {
                field(json, fieldName, (Number)propValue);
            } else if (propValue instanceof String) {
                String strValue = (String) propValue;
                if (!strValue.isEmpty()) {
                    field(json, fieldName, strValue);
                }
            } else if (propValue instanceof Boolean) {
                field(json, fieldName, (Boolean) propValue);
            } else if (propValue instanceof Collection) {
                serializeObjectList(json, fieldName, (Collection<?>) propValue);
            } else if (propValue instanceof Map) {
                serializeProperties(json, fieldName, (Map<String, ?>) propValue);
            } else {
                log.debug("Unsupported property type: " + propValue.getClass().getName());
                field(json, fieldName, propValue.toString());
            }
        }
        json.endObject();
    }

    public static <OBJECT_CONTEXT, OBJECT_TYPE> Map<String, Object> serializeObject(DBRRunnableContext runnableContext, OBJECT_CONTEXT context, @NotNull OBJECT_TYPE object) {
        DBPObjectSerializer<OBJECT_CONTEXT, OBJECT_TYPE> serializer = SerializerRegistry.getInstance().createSerializer(object);
        if (serializer == null) {
            log.error("No serializer found for object " + object.getClass().getName());
            return null;
        }
        Map<String, Object> state = new LinkedHashMap<>();

        Map<String, Object> location = new LinkedHashMap<>();
        serializer.serializeObject(runnableContext, context, object, location);
        state.put("type", SerializerRegistry.getInstance().getObjectType(object));
        state.put("location", location);

        return state;
    }

    public static <OBJECT_CONTEXT, OBJECT_TYPE> Object deserializeObject(@NotNull DBRRunnableContext runnableContext,  OBJECT_CONTEXT objectContext, @NotNull Map<String, Object> objectConfig) throws DBCException {
        String typeID = CommonUtils.toString(objectConfig.get("type"));
        DBPObjectSerializer<OBJECT_CONTEXT, OBJECT_TYPE> serializer = SerializerRegistry.getInstance().createSerializerByType(typeID);
        if (serializer == null) {
            log.error("No deserializer found for type " + typeID);
            return null;
        }
        Map<String, Object> location = getObject(objectConfig, "location");
        return serializer.deserializeObject(runnableContext, objectContext, location);
    }

    @NotNull
    public static Map<String, Object> parseMap(@NotNull Gson gson, @NotNull Reader reader) {
        return gson.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
    }

    @NotNull
    public static Map<String, Object> getObject(@NotNull Map<String, Object> map, @NotNull String name) {
        Map<String, Object> object = (Map<String, Object>) map.get(name);
        if (object == null) {
            return Collections.emptyMap();
        } else {
            return object;
        }
    }

    @NotNull
    public static Iterable<Map.Entry<String, Map<String, Object>>> getNestedObjects(@NotNull Map<String, Object> map, @NotNull String name) {
        Map<String, Map<String, Object>> object = (Map<String, Map<String, Object>>) map.get(name);
        if (object == null) {
            return Collections.emptyList();
        } else {
            return object.entrySet();
        }
    }

    public static <T> T getObjectProperty(Object object, String name) {
        if (object instanceof Map) {
            return (T) ((Map) object).get(name);
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

    @NotNull
    public static List<Map<String, Object>> getObjectList(@NotNull Map<String, Object> map, @NotNull String name) {
        Object value = map.get(name);
        if (value instanceof List) {
            return  (List<Map<String, Object>>) value;
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
        if (propMap instanceof Map && !((Map) propMap).isEmpty()) {
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
        if (propMap instanceof Collection) {
            for (Object pe : (Collection) propMap) {
                result.add(CommonUtils.toString(pe));
            }
        }
        return result;
    }

}
