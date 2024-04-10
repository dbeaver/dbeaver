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
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.dpi.model.client.DPIClientProxy;
import org.jkiss.dbeaver.dpi.model.client.DPISmartObjectWrapper;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.dpi.*;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.utils.BeanUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.sql.SQLException;
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

    public static Gson createServerSerializer(DPIContext context) {
        Map<Class<?>, AdapterCreator> adapters = new HashMap<>(getCommonAdapters());
        return new GsonBuilder()
            .registerTypeAdapterFactory(new DPITypeAdapterFactory(context, adapters))
            .create();
    }

    public static Gson createClientSerializer(@NotNull DPIContext context) {
        Map<Class<?>, AdapterCreator> adapters = new HashMap<>(getCommonAdapters());

        return new GsonBuilder()
            .registerTypeAdapterFactory(new DPITypeAdapterFactory(context, adapters))
            .create();
    }

    private static Map<Class<?>, AdapterCreator> getCommonAdapters() {
        Map<Class<?>, AdapterCreator> common = new HashMap<>();
        common.put(DBRProgressMonitor.class, (context, gson) -> new DPIProgressMonitorAdapter(context));
        common.put(DPIClientObject.class, (context, gson) -> new DPIObjectRefAdapter(context));
        common.put(Throwable.class, (context, gson) -> new DPIThrowableAdapter(context));
        common.put(SQLDialect.class, (context, gson) -> new SQLDialectAdapter(context));
        common.put(TimeZone.class, (context, gson) -> new TimeZoneAdapter(context));
        common.put(DBCResultSet.class, DPIResultSetAdapter::new);
        common.put(DBDDataReceiver.class, SQLDataReceiverAdapter::new);
        common.put(DPISmartObjectWrapper.class, DPIServerSmartObjectsAdapter::new);
        return common;
    }


    @FunctionalInterface
    private interface AdapterCreator {
        TypeAdapter<?> createAdapter(@NotNull DPIContext context, @NotNull Gson gson);
    }


    static final class DPITypeAdapterFactory implements TypeAdapterFactory {
        private final DPIContext context;
        private final Map<Class<?>, AdapterCreator> classAdapters;

        public DPITypeAdapterFactory(DPIContext context, Map<Class<?>, AdapterCreator> classAdapters) {
            this.context = context;
            this.classAdapters = classAdapters;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {

            if (typeToken.getType() == Class.class) {
                return (TypeAdapter<T>) new DPIClassAdapter(context);
            } else if (typeToken.getType() instanceof Class<?>) {
                Class<?> theClass = (Class<?>) typeToken.getType();
                for (Map.Entry<Class<?>, AdapterCreator> entry : classAdapters.entrySet()) {
                    if (entry.getKey().isAssignableFrom(theClass)) {
                        return (TypeAdapter<T>) entry.getValue().createAdapter(context, gson);
                    }
                }

                if (getClassAnnotation(theClass, DPILocalObject.class) != null) {
                    return new DPILocalObjectAdapter<>(context);
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
            return null;
        }

    }

    @Nullable
    public static DPIObject getDPIAnno(@NotNull Class<?> type) {
        return getClassAnnotation(type, DPIObject.class);
    }

    public static boolean isSmartObject(@NotNull Class<?> type) {
        return getClassAnnotation(type, DPISmartObject.class) != null;
    }

    @Nullable
    public static <T extends Annotation> T getClassAnnotation(
        @NotNull Class<?> type,
        @NotNull Class<T> annotationClass
    ) {
        T annotation = type.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> i : type.getInterfaces()) {
            annotation = getClassAnnotation(i, annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        Class<?> superclass = type.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return null;
        }
        return getClassAnnotation(superclass, annotationClass);
    }

    public static <T extends Annotation> T getMethodAnno(@NotNull Method method, Class<T> annoType) {
        T annotation = method.getAnnotation(annoType);
        if (annotation == null) {
            annotation = getMethodAnno(method, annoType, method.getDeclaringClass());
        }
        return annotation;
    }

    @Nullable
    private static <T extends Annotation> T getMethodAnno(
        @NotNull Method method,
        Class<T> annoType,
        Class<?> methodClass
    ) {
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

    static class DPIThrowableAdapter extends AbstractTypeAdapter<Throwable> {
        public DPIThrowableAdapter(DPIContext context) {
            super(context);
        }

        @Override
        public void write(JsonWriter jsonWriter, Throwable error) throws IOException {
            if (error == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.name("class");
                jsonWriter.value(error.getClass().getName());
                if (error.getMessage() != null) {
                    jsonWriter.name("message");
                    jsonWriter.value(error.getMessage());
                }
                jsonWriter.name("stacktrace");
                jsonWriter.value(Arrays.toString(error.getStackTrace()));
                if (error instanceof SQLException) {
                    SQLException sqlError = (SQLException) error;
                    jsonWriter.name("errorCode");
                    jsonWriter.value(sqlError.getErrorCode());
                    if (sqlError.getSQLState() != null) {
                        jsonWriter.name("sqlState");
                        jsonWriter.value(sqlError.getSQLState());
                    }
                }
            }
        }

        @Override
        public Throwable read(JsonReader jsonReader) throws IOException {
            String className = null;
            String message = null;
            String stacktrace = null;
            int errorCode = -1;
            String sqlState = null;
            while (jsonReader.peek() == JsonToken.NAME) {
                String attrName = jsonReader.nextName();
                switch (attrName) {
                    case "class":
                        className = jsonReader.nextString();
                        break;
                    case "message":
                        message = jsonReader.nextString();
                        break;
                    case "stacktrace":
                        stacktrace = jsonReader.nextString();
                        break;
                    case "errorCode":
                        errorCode = jsonReader.nextInt();
                        break;
                    case "sqlState":
                        sqlState = jsonReader.nextString();
                        break;
                }
            }
            try {
                Class<?> errorClass = Class.forName(className, true, context.getClassLoader());
                return new Exception();
                //errorClass.getConstructor()
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
        public void write(JsonWriter jsonWriter, DBRProgressMonitor aClass) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.endObject();
            // do-nothing
        }

        @Override
        public DBRProgressMonitor read(JsonReader jsonReader) throws IOException {
            jsonReader.beginObject();
            jsonReader.endObject();
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
                jsonWriter.nullValue();
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
            Map<String, Object> props = new LinkedHashMap<>();
            if (pc.getProperties().length > 0) {
                for (DBPPropertyDescriptor prop : pc.getProperties()) {
                    props.put(prop.getId(), pc.getPropertyValue(null, prop.getId()));
                }
            }
            for (Method m : t.getClass().getMethods()) {
                if (m.getParameterTypes().length == 0 && !m.isAnnotationPresent(Property.class)) {
                    DPIElement elemAnno = DPISerializer.getMethodAnno(m, DPIElement.class);
                    if (elemAnno != null && elemAnno.cache()) {
                        try {
                            Object propValue = m.invoke(t);
                            props.put(BeanUtils.getPropertyNameFromGetter(m.getName()), propValue);
                        } catch (Throwable e) {
                            log.debug("Error reading object element " + m);
                        }
                    }
                }
            }
            if (!props.isEmpty()) {
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
            if (jsonReader.peek() == JsonToken.NULL) {
                return null;
            }
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
                            Object objectContainer = containerId.equals(objectId) ? DPIClientProxy.SELF_REFERENCE : context.getObject(
                                containerId);
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
                                case BOOLEAN:
                                    propValue = jsonReader.nextBoolean();
                                    break;
                                case NUMBER:
                                    propValue = jsonReader.nextLong();
                                    break;
                                case STRING:
                                    propValue = jsonReader.nextString();
                                    break;
                                default:
                                    log.debug("Skip property '" + propName + "' value");
                                    jsonReader.skipValue();
                                    break;
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
                return (T) object;
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
            if (theClass.isInterface()) {
                allInterfaces.add(theClass);
            }
            Collections.addAll(allInterfaces, theClass.getInterfaces());
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
            return (T) object;
        }

        private ClassLoader getClassLoader() {
            return context.getClassLoader();
        }

    }

}
