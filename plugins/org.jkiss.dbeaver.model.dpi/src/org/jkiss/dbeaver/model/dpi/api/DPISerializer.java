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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DPIContainer;
import org.jkiss.dbeaver.model.DPIObject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.dpi.client.DPIClientProxy;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.utils.ArrayUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DPI utils
 */
public class DPISerializer {

    private static final Log log = Log.getLog(DPISerializer.class);
    public static final String ATTR_OBJECT_ID = "id";
    public static final String ATTR_OBJECT_TYPE = "type";
    private static final String ATTR_OBJECT_STRING = "string";
    private static final String ATTR_OBJECT_HASH = "hash";
    private static final String ATTR_PROPS = "properties";
    private static final String ATTR_CONTAINERS = "containers";

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
                DPIObject annotation = getDPIAnno(((Class<?>)typeToken.getType()));
                if (annotation != null) {
                    return new DPIObjectTypeAdapter<T>(context, typeToken.getType());
                }
            } else if (typeToken.getType() instanceof WildcardType) {
                Type[] upperBounds = ((WildcardType) typeToken.getType()).getUpperBounds();
                if (upperBounds.length > 0 && upperBounds[0] instanceof Class) {
                    DPIObject annotation = getDPIAnno((Class<?>) upperBounds[0]);
                    if (annotation != null) {
                        return new DPIObjectTypeAdapter<T>(context, typeToken.getType());
                    }
                }
            }
            return null;
        }

        private static <T> DPIObject getDPIAnno(@NotNull Class<?> type) {
            DPIObject annotation = type.getAnnotation(DPIObject.class);
            if (annotation != null) {
                return annotation;
            }
            for (Class<?> i : type.getInterfaces()) {
                annotation = getDPIAnno(i);
                if (annotation != null) {
                    return annotation;
                }
            }
            Class<?> superclass = type.getSuperclass();
            if (superclass == null || superclass == Object.class) {
                return null;
            }
            return getDPIAnno(superclass);
        }

    }

    static class DPIObjectTypeAdapter<T> extends TypeAdapter<T> {

        private final DPIContext context;
        private final Type type;
        public DPIObjectTypeAdapter(DPIContext context, Type type) {
            this.context = context;
            this.type = type;
        }

        @Override
        public void write(JsonWriter jsonWriter, Object t) throws IOException {
            if (t == null) {
                return;
            }
            boolean oldObject = context.hasObject(t);

            jsonWriter.beginObject();
            jsonWriter.name(ATTR_OBJECT_ID);
            jsonWriter.value(context.getOrCreateObjectId(t));
            if (!oldObject) {
                jsonWriter.name(ATTR_OBJECT_TYPE);
                jsonWriter.value(t.getClass().getName());
                jsonWriter.name(ATTR_OBJECT_STRING);
                jsonWriter.value(t.toString());
                jsonWriter.name(ATTR_OBJECT_HASH);
                jsonWriter.value(t.hashCode());

                {
                    // Save containers references
                    boolean hasContainers = false;
                    for (Method method : t.getClass().getMethods()) {
                        DPIContainer anno = method.getAnnotation(DPIContainer.class);
                        if (anno != null && !anno.root() && method.getParameterTypes().length == 0) {
                            Object container;
                            try {
                                container = method.invoke(t);
                            } catch (Throwable e) {
                                log.warn("Error reading DPI container", e);
                                continue;
                            }
                            if (container != null) {
                                String containerId = context.getObjectId(container);
                                if (containerId != null) {
                                    if (!hasContainers) {
                                        jsonWriter.name(ATTR_CONTAINERS);
                                        jsonWriter.beginObject();
                                        hasContainers = true;
                                    }
                                    jsonWriter.name(method.getName());
                                    jsonWriter.value(containerId);
                                }
                            }
                        }
                    }
                    if (hasContainers) {
                        jsonWriter.endObject();
                    }
                }

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
            String objectToString = null;
            Integer objectHashCode = null;
            Map<String, Object> objectContainers = null;
            Map<String, Object> objectProperties = null;
            if (ATTR_OBJECT_ID.equals(jsonReader.nextName())) {
                objectId = jsonReader.nextString();
            }
            if (jsonReader.peek() == JsonToken.NAME && ATTR_OBJECT_TYPE.equals(jsonReader.nextName())) {
                objectType = jsonReader.nextString();
            }
            if (jsonReader.peek() == JsonToken.NAME && ATTR_OBJECT_STRING.equals(jsonReader.nextName())) {
                objectToString = jsonReader.nextString();
            }
            if (jsonReader.peek() == JsonToken.NAME && ATTR_OBJECT_HASH.equals(jsonReader.nextName())) {
                objectHashCode = jsonReader.nextInt();
            }
            if (jsonReader.peek() == JsonToken.NAME && ATTR_CONTAINERS.equals(jsonReader.nextName())) {
                jsonReader.beginObject();
                while (jsonReader.peek() == JsonToken.NAME) {
                    String containerName = jsonReader.nextName();
                    String containerId = jsonReader.nextString();
                    Object objectContainer = context.getObject(containerId);
                    if (objectContainer != null) {
                        if (objectContainers == null) {
                            objectContainers = new HashMap<>();
                        }
                        objectContainers.put(containerName, objectContainer);
                    } else {
                        log.debug("DPI container '" + containerName + "'='" + containerId + "' not found in DPI context");
                    }
                }
                jsonReader.endObject();
            }
            if (jsonReader.peek() == JsonToken.NAME && ATTR_PROPS.equals(jsonReader.nextName())) {
                jsonReader.beginObject();
                while (jsonReader.peek() == JsonToken.NAME) {
                    String propName = jsonReader.nextName();
                    Object propValue = null;
                    switch (jsonReader.peek()) {
                        case BOOLEAN: propValue = jsonReader.nextBoolean(); break;
                        case NUMBER: propValue = jsonReader.nextLong(); break;
                        case STRING: propValue = jsonReader.nextString(); break;
                        default:
                            log.debug("Skip property '" + propName + "' value");
                            jsonReader.skipValue(); break;
                    }
                    if (objectProperties == null) {
                        objectProperties = new LinkedHashMap<>();
                    }
                    objectProperties.put(propName, propValue);
                }
                jsonReader.endObject();
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
            DPIClientProxy objectHandler = new DPIClientProxy(
                context,
                objectId,
                objectType,
                objectToString,
                objectHashCode,
                objectContainers);
            object = Proxy.newProxyInstance(getClass().getClassLoader(), allInterfaces, objectHandler);
            context.addObject(objectId, object);
            return (T)object;
        }

    }

}
