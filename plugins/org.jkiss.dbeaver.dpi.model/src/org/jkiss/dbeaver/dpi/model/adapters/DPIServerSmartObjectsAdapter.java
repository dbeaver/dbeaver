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
package org.jkiss.dbeaver.dpi.model.adapters;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.dpi.model.client.DPISmartObjectWrapper;
import org.jkiss.dbeaver.model.dpi.DPISmartCallback;

import java.io.IOException;

public class DPIServerSmartObjectsAdapter extends AbstractTypeAdapter<DPISmartObjectWrapper> {
    private final Gson gson;

    public DPIServerSmartObjectsAdapter(@NotNull DPIContext context, Gson gson) {
        super(context);
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter jsonWriter, DPISmartObjectWrapper objectWrapper) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("callbackClass");
        jsonWriter.value(gson.toJson(objectWrapper.getSmartProxyClassName()));
        jsonWriter.name("object");
        jsonWriter.value(gson.toJson(objectWrapper.getProxyObject()));
        jsonWriter.name("argumentNumber");
        jsonWriter.value(objectWrapper.getArgumentNumber());
        jsonWriter.endObject();
    }

    @Override
    public DPISmartObjectWrapper read(JsonReader jsonReader) throws IOException {
        Class<?> clazz = null;
        String serializedObject = null;
        int argumentNumber = 0;
        jsonReader.beginObject();
        while (jsonReader.peek() == JsonToken.NAME) {
            String attrName = jsonReader.nextName();
            switch (attrName) {
                case "callbackClass":
                    String className = jsonReader.nextString();
                    clazz = gson.fromJson(className, Class.class);
                    break;
                case "object":
                    serializedObject = jsonReader.nextString();
                    break;
                case "argumentNumber":
                    argumentNumber = jsonReader.nextInt();
                    break;
            }
        }
        jsonReader.endObject();

        if (clazz == null) {
            throw new IOException("Invalid json, class attribute not found");
        }
        DPISmartCallback object = null;
        if (serializedObject != null) {
            object = (DPISmartCallback) gson.fromJson(serializedObject, clazz);
        }
        return new DPISmartObjectWrapper(clazz, argumentNumber, object);
    }
}
