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
package org.jkiss.dbeaver.model.dpi.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPRemoteObject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DPI utils
 */
public class DPISerializer {

    private static final Log log = Log.getLog(DPISerializer.class);
    public static final String ATTR_OBJECT_ID = "id";
    public static final String ATTR_OBJECT_TYPE = "type";
    private static final String ATTR_PROPS = "properties";

    public static Gson createSerializer(DPIContext context) {
        return new GsonBuilder()
            .registerTypeAdapterFactory(new DPITypeAdapterFactory(context))
            .create();
    }

    static final class DPITypeAdapterFactory implements TypeAdapterFactory {
        private final DPIContext context;
        public DPITypeAdapterFactory(DPIContext context) {
            this.context = context;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            if (typeToken.getType() instanceof Class<?>) {
                DBPRemoteObject annotation = ((Class<?>) typeToken.getType()).getAnnotation(DBPRemoteObject.class);
                if (annotation != null) {
                    return new DPIObjectTypeAdapter<T>(context, typeToken.getType());
                }
            }
            return null;
        }
    };

    static class DPIObjectTypeAdapter<T> extends TypeAdapter<T> {

        private final DPIContext context;
        private final Type type;
        public DPIObjectTypeAdapter(DPIContext context, Type type) {
            this.context = context;
            this.type = type;
        }

        @Override
        public void write(JsonWriter jsonWriter, Object t) throws IOException {
            boolean oldObject = context.hasObject(t);

            jsonWriter.beginObject();
            jsonWriter.name(ATTR_OBJECT_ID);
            jsonWriter.value(context.getOrCreateObjectId(t));
            if (!oldObject) {
                jsonWriter.name(ATTR_OBJECT_TYPE);
                jsonWriter.value(t.getClass().getName());

                // Serialize properties
                PropertyCollector pc = new PropertyCollector(t, false);
                pc.collectProperties();
                if (pc.getProperties().length > 0) {
                    Map<String, Object> props = new LinkedHashMap<>();
                    for (DBPPropertyDescriptor prop : pc.getProperties()) {
                        props.put(prop.getId(), pc.getPropertyValue(null, prop.getId()));
                    }
                    JSONUtils.serializeProperties(jsonWriter, ATTR_PROPS, props);
                }
            }
            jsonWriter.endObject();
        }

        @Override
        public T read(JsonReader jsonReader) throws IOException {
            jsonReader.beginObject();
            String objectId = null;
            String objectType = null;
            if (ATTR_OBJECT_ID.equals(jsonReader.nextName())) {
                objectId = jsonReader.nextString();
            }
            if (jsonReader.peek() == JsonToken.NAME && ATTR_OBJECT_TYPE.equals(jsonReader.nextName())) {
                objectType = jsonReader.nextString();
            }
            if (jsonReader.peek() == JsonToken.NAME && ATTR_PROPS.equals(jsonReader.nextName())) {
                System.out.println("PROPS");
            }
            jsonReader.endObject();

            if (objectId == null) {
                throw new IOException("Object ID is not specified in DPI response");
            }
            Object object = context.getObject(objectId);
            if (object != null) {
                return (T)object;
            }
            if (!(type instanceof Class<?>)) {
                throw new IOException("Non class result: " + type);
            }
            Class<?> theClass = (Class<?>) type;
            Class<?>[] allInterfaces = (theClass).getInterfaces();
            if (allInterfaces.length > 0) {
                allInterfaces = ArrayUtils.add(Class.class, allInterfaces, theClass);
            } else {
                allInterfaces = new Class[] {theClass};
            }
            DPIClientHandler objectHandler = new DPIClientHandler(context, objectId, objectType);
            object = Proxy.newProxyInstance(getClass().getClassLoader(), allInterfaces, objectHandler);
            context.addObject(objectId, object);
            return (T)object;
        }

    }

    private static class DPIClientHandler implements InvocationHandler {
        private final DPIContext context;
        private final String objectId;
        private final String objectType;

        public DPIClientHandler(DPIContext context, String objectId, String objectType) {
            this.context = context;
            this.objectId = objectId;
            this.objectType = objectType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return BeanUtils.handleObjectMethod(proxy, method, args);
            }
            DPIController controller = context.getDpiController();
            if (controller == null) {
                throw new DBException("No DPI controller in client context");
            }
            // If method is property read or state read the try lookup in the context cache first

            throw new DBException("Not supported method (" + method + " at " + objectId + "@" + objectType + ")");
        }
    }

}
