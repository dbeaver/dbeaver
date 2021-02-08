/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Freaking workaround for GSON silly numbers deserialization.
 * Originally from https://github.com/google/gson/issues/1084
 */
public final class JSONBestNumberSerializer implements JsonSerializer<Number>, JsonDeserializer<Number> {

    @Override
    public JsonElement serialize(Number src, Type srcType, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Number deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException {
        final String s = json.getAsString();
        if (s.contains(".") || s.contains("E")) {
            return Double.parseDouble(s);
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ex) {
                return Double.parseDouble(s);
            }
        }
    }

}