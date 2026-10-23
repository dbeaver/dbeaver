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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.IOException;
import java.util.Map;

public class JsonSchemaAdapter extends TypeAdapter<Object> {
    private static final TypeAdapter<Object> adapter = new Gson().getAdapter(Object.class);

    @Override
    public void write(@NotNull JsonWriter writer, @Nullable Object value) throws IOException {
        boolean serializeNulls = writer.getSerializeNulls();
        try {
            if (value instanceof Map<?, ?>) {
                // null is meaningful in schema keywords such as const and default
                writer.setSerializeNulls(true);
            }
            adapter.write(writer, value);
        } finally {
            writer.setSerializeNulls(serializeNulls);
        }
    }

    @Nullable
    @Override
    public Object read(@NotNull JsonReader reader) throws IOException {
        return adapter.read(reader);
    }
}
