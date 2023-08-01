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
package org.jkiss.dbeaver.dpi.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.dpi.model.client.DPIClientProxy;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DPIClientObject;
import org.jkiss.dbeaver.model.DPIContainer;
import org.jkiss.dbeaver.model.DPIObject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

/**
 * DPI utils
 */
public class DPISerializer {

    private static final Log log = Log.getLog(DPISerializer.class);
    public static final String ATTR_OBJECT_ID = "id";
    public static final String ATTR_ROOT = "root";
    public static final String ATTR_OBJECT_TYPE = "type";
    private static final String ATTR_OBJECT_STRING = "string";
    private static final String ATTR_OBJECT_HASH = "hash";
    private static final String ATTR_INTERFACES = "interfaces";
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
                Class<?> theClass = (Class<?>) typeToken.getType();
                if (DBRProgressMonitor.class.isAssignableFrom(theClass)) {
                    return (TypeAdapter<T>) new DPIProgressMonitorAdapter(context);
                } else if (DPIClientObject.class.isAssignableFrom(theClass)) {
                    return (TypeAdapter<T>) new DPIObjectRefAdapter(context);
                }

                DPIObject annotation = getDPIAnno(theClass);
                if (annotation != null) {
                    return new DPIObjectTypeAdapter<>(context, typeToken.getType());
                }
            } else if (typeToken.getType() instanceof WildcardType) {
                Type[] upperBounds = ((WildcardType) typeToken.getType()).getUpperBounds();
                if (upperBounds.length > 0 && upperBounds[0] instanceof Class) {
                    DPIObject annotation = getDPIAnno((Class<?>) upperBounds[0]);
                    if (annotation != null) {
                        return new DPIObjectTypeAdapter<>(context, typeToken.getType());
                    }
                }
            }
            if (typeToken.getType() == Class.class) {
                return (TypeAdapter<T>) new DPIClassAdapter(context);
            }
            return null;
        }

    }


    public static <T> DPIObject getDPIAnno(@NotNull Class<?> type) {
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

    public static <T extends Annotation> T getMethodAnno(@NotNull Method method, Class<T> annoType) {
        T annotation = method.getAnnotation(annoType);
        if (annotation == null) {
            annotation = getMethodAnno(method, annoType, method.getDeclaringClass());
        }
        return annotation;
    }

    @Nullable
    private static <T extends Annotation> T getMethodAnno(@NotNull Method method, Class<T> annoType, Class<?> methodClass) {
        for (Class<?> mi : methodClass.getInterfaces()) {
            if (mi.getAnnotation(DPIObject.class) != null) {
                try {
                    Method iMethod = mi.getMethod(method.getName(), method.getParameterTypes());
                    T anno = iMethod.getAnnotation(annoType);
                    if (anno != null) {
                        return anno;
                    }
                } catch (NoSuchMethodException e) {
                    // ignore
                }
            }
            T anno = getMethodAnno(method, annoType, mi);
            if (anno != null) {
                return anno;
            }
        }
        return null;
    }

    static abstract class AbstractTypeAdapter<T> extends TypeAdapter<T> {
        protected final DPIContext context;

        public AbstractTypeAdapter(DPIContext context) {
            this.context = context;
        }
    }

    static class DPIObjectRefAdapter extends AbstractTypeAdapter<DPIClientObject> {
        public DPIObjectRefAdapter(DPIContext context) {
            super(context);
        }

        @Override
        public void write(JsonWriter jsonWriter, DPIClientObject object) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name(ATTR_OBJECT_ID);
            jsonWriter.value(object.dpiObjectId());
            jsonWriter.endObject();
        }

        @Override
        public DPIClientObject read(JsonReader jsonReader) throws IOException {
            throw new IOException("DPI object reference deserialization is not supported");
        }
    }

    static class DPIClassAdapter extends AbstractTypeAdapter<Class> {
        public DPIClassAdapter(DPIContext context) {
            super(context);
        }

        @Override
        public void write(JsonWriter jsonWriter, Class aClass) throws IOException {
            jsonWriter.value(aClass.getName());
        }

        @Override
        public Class<?> read(JsonReader jsonReader) throws IOException {
            String className = jsonReader.nextString();
            try {
                return Class.forName(className, true, context.getClassLoader());
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    static class DPIProgressMonitorAdapter extends AbstractTypeAdapter<DBRProgressMonitor> {
        public DPIProgressMonitorAdapter(DPIContext context) {
            super(context);
        }

        @Override
        public void write(JsonWriter jsonWriter, DBRProgressMonitor aClass) {
            // do-nothing
        }

        @Override
        public DBRProgressMonitor read(JsonReader jsonReader) throws IOException {
            return context.getProgressMonitor();
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
            } else if (t instanceof DBPDataSourceContainer) {
                jsonWriter.beginObject();
                jsonWriter.name(ATTR_OBJECT_ID);
                jsonWriter.value(ATTR_ROOT);
                jsonWriter.endObject();
                return;
            }
            boolean oldObject = context.hasObject(t);

            jsonWriter.beginObject();
            jsonWriter.name(ATTR_OBJECT_ID);
            jsonWriter.value(context.getOrCreateObjectId(t));
            if (!oldObject) {
                serializeObjectInfo(jsonWriter, t);
                serializeContainers(jsonWriter, t);
                serializeProperties(jsonWriter, t);
            }
            jsonWriter.endObject();
        }

        private void serializeObjectInfo(JsonWriter jsonWriter, Object t) throws IOException {
            jsonWriter.name(ATTR_OBJECT_TYPE);
            jsonWriter.value(t.getClass().getName());
            jsonWriter.name(ATTR_OBJECT_STRING);
            jsonWriter.value(t.toString());
            jsonWriter.name(ATTR_OBJECT_HASH);
            jsonWriter.value(t.hashCode());
            {
                jsonWriter.name(ATTR_INTERFACES);
                jsonWriter.beginArray();
                for (Class<?> theClass = t.getClass(); theClass != null && theClass != Object.class; theClass = theClass.getSuperclass()) {
                    for (Class<?> theInt : theClass.getInterfaces()) {
                        jsonWriter.value(theInt.getName());
                    }
                }
                jsonWriter.endArray();
            }
        }

        private void serializeProperties(JsonWriter jsonWriter, Object t) throws IOException {
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

        private void serializeContainers(JsonWriter jsonWriter, Object t) throws IOException {
            // Save containers references
            Set<String> collectedContainers = null;
            boolean hasContainers = false;
            for (Method method : t.getClass().getMethods()) {
                if (collectedContainers != null && collectedContainers.contains(method.getName())) {
                    continue;
                }
                DPIContainer anno = getMethodAnno(method, DPIContainer.class);
                if (anno != null && !anno.root() && method.getParameterTypes().length == 0) {
                    Object container;
                    try {
                        container = method.invoke(t);
                    } catch (Throwable e) {
                        log.warn("Error reading DPI container", e);
                        continue;
                    }
                    if (container != null) {
                        if (collectedContainers == null) {
                            collectedContainers = new HashSet<>();
                        }
                        collectedContainers.add(method.getName());
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

        @Override
        public T read(JsonReader jsonReader) throws IOException {
            jsonReader.beginObject();
            String objectId = null;
            String objectType = null;
            String objectToString = null;
            Integer objectHashCode = null;
            Set<Class<?>> allInterfaces = new LinkedHashSet<>();
            Map<String, Object> objectContainers = null;
            Map<String, Object> objectProperties = null;
            if (ATTR_OBJECT_ID.equals(jsonReader.nextName())) {
                objectId = jsonReader.nextString();
            }
            if (ATTR_ROOT.equals(objectId)) {
                jsonReader.endObject();
                return (T) context.getRootObject();
            }
            while (jsonReader.peek() == JsonToken.NAME) {
                String attrName = jsonReader.nextName();
                switch (attrName) {
                    case ATTR_OBJECT_TYPE:
                        objectType = jsonReader.nextString();
                        break;
                    case ATTR_OBJECT_STRING:
                        objectToString = jsonReader.nextString();
                        break;
                    case ATTR_OBJECT_HASH:
                        objectHashCode = jsonReader.nextInt();
                        break;
                    case ATTR_INTERFACES: {
                        jsonReader.beginArray();
                        while (jsonReader.peek() == JsonToken.STRING) {
                            String intName = jsonReader.nextString();
                            try {
                                Class<?> intClass = Class.forName(intName, true, getClassLoader());
                                allInterfaces.add(intClass);
                            } catch (Exception e) {
                                log.debug("Interface '" + intName + "' cannot be resolved");
                            }
                        }
                        jsonReader.endArray();
                        break;
                    }
                    case ATTR_CONTAINERS: {
                        jsonReader.beginObject();
                        while (jsonReader.peek() == JsonToken.NAME) {
                            String containerName = jsonReader.nextName();
                            String containerId = jsonReader.nextString();
                            Object objectContainer = containerId.equals(objectId) ? DPIClientProxy.SELF_REFERENCE : context.getObject(containerId);
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
                        break;
                    }
                    case ATTR_PROPS: {
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
                        break;
                    }
                }
            }
            jsonReader.endObject();

            if (objectId == null) {
                throw new IOException("Object ID is not specified in DPI response");
            }
            Object object = context.getObject(objectId);
            if (object != null) {
                return (T)object;
            }
            Class<?> theClass;
            if (type instanceof Class<?>) {
                theClass = (Class<?>) type;
            } else if (type instanceof WildcardType) {
                Type[] upperBounds = ((WildcardType) type).getUpperBounds();
                if (upperBounds.length == 1 && upperBounds[0] instanceof Class<?>) {
                    theClass = (Class<?>) upperBounds[0];
                } else {
                    throw new IOException("Unrecognized upper bounds '" + Arrays.toString(upperBounds) + "', deserialization not supported (" + type + ")");
                }
            } else {
                throw new IOException("Non class result, deserialization not supported (" + type + ")");
            }
            allInterfaces.add(theClass);
            Collections.addAll(allInterfaces, (theClass).getInterfaces());
            allInterfaces.add(DPIClientObject.class);
            DPIClientProxy objectHandler = new DPIClientProxy(
                context,
                allInterfaces.toArray(new Class<?>[0]),
                objectId,
                objectType,
                objectToString,
                objectHashCode,
                objectContainers,
                objectProperties);
            object = objectHandler.getObjectInstance();
            context.addObject(objectId, object);
            return (T)object;
        }

        private ClassLoader getClassLoader() {
            return context.getClassLoader();
        }

    }

}
