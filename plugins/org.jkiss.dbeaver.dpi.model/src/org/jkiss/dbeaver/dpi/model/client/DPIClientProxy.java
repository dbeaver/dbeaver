/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.dpi.model.client;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.model.dpi.DPIClientObject;
import org.jkiss.dbeaver.model.dpi.DPIController;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.rest.RestProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class DPIClientProxy implements DPIClientObject, InvocationHandler {
    private static final Log log = Log.getLog(DPIClientProxy.class);

    public static final Object SELF_REFERENCE = new Object();
    public static final Object NULL_VALUE = new Object();

    private final DPIContext context;
    private final String objectId;
    private final String objectType;
    private final String objectToString;
    private final Integer objectHashCode;
    private final transient Object objectInstance;
    private Map<String, Object> propertyValues;

    public DPIClientProxy(
        @NotNull DPIContext context,
        @NotNull Class<?>[] allInterfaces,
        @NotNull String objectId,
        @Nullable String objectType,
        @Nullable String objectToString,
        @Nullable Integer objectHashCode,
        @Nullable Map<String, Object> propertyValues) {
        this.context = context;
        this.objectId = objectId;
        this.objectType = objectType;
        this.objectToString = objectToString;
        this.objectHashCode = objectHashCode;
        this.propertyValues = propertyValues;

        this.objectInstance = Proxy.newProxyInstance(
            context.getClassLoader(),
            allInterfaces,
            this);
    }

    @Override
    public String dpiObjectId() {
        return objectId;
    }

    @Override
    public String dpiObjectType() {
        return objectType;
    }

    @Override
    public ClassLoader dpiClassLoader() {
        return context.getClassLoader();
    }

    @Override
    public Object dpiPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String propertyName) throws DBException {
        Object value = propertyValues == null ? null : propertyValues.get(propertyName);
        if (value == NULL_VALUE) {
            return null;
        } else if (value != null) {
            return value;
        }
        if (monitor == null) {
            // Not read yet
            return null;
        }
        // Read lazy property
        DPIController controller = context.getDpiController();
        if (controller == null) {
            throw new DBException("No DPI controller in client context");
        }
        if (controller instanceof RestProxy) {
            // Try to get property class
            try {
                Type returnType = null;
                Class<?> localClass = dpiClassLoader().loadClass(dpiObjectType());
                Method getter = DBXTreeItem.findPropertyReadMethod(localClass, propertyName);
                if (getter != null) {
                    returnType = getter.getGenericReturnType();
                }
                ((RestProxy) controller).setNextCallResultType(returnType);
            } catch (ClassNotFoundException e) {
                log.debug("Cannot resolve local class '" + dpiObjectType() + "'");
            }
        }
        Object propValue = controller.readProperty(this.objectId, propertyName);

        cachePropertyValue(propertyName, wrapObjectValue(propValue));

        return propValue;
    }

    @Override
    public Object dpiObjectMethod(@Nullable DBRProgressMonitor monitor, @NotNull String methodName, @Nullable Object[] arguments) throws DBException {
        return invokeRemoteMethod(methodName, arguments, null);
    }

    public Object getObjectInstance() {
        return objectInstance;
    }

    @Override
    public String toString() {
        return objectToString;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (method.getDeclaringClass() == Object.class) {
            if (methodName.equals("toString") && objectToString != null) {
                return objectToString;
            } else if (methodName.equals("hashCode") && objectHashCode != null) {
                return objectHashCode;
            }
            return BeanUtils.handleObjectMethod(proxy, method, args);
        } else if (method.getDeclaringClass() == DPIClientObject.class) {
            switch (methodName) {
                case "dpiObjectId":
                    return dpiObjectId();
                case "dpiObjectType":
                    return dpiObjectType();
                case "dpiClassLoader":
                    return dpiClassLoader();
                case "dpiPropertyValue":
                    return dpiPropertyValue((DBRProgressMonitor) args[0], (String) args[1]);
                case "dpiObjectMethod":
                    return dpiObjectMethod((DBRProgressMonitor) args[0], (String) args[1], (Object[]) args[2]);
                default:
                    log.debug("Unsupported DPI method '" + methodName + "'");
            }
            return null;
        }

        Property propAnnotation = method.getAnnotation(Property.class);
        if (propAnnotation != null && propertyValues != null) {
            Object result = propertyValues.get(getPropertyKey(method, propAnnotation));
            if (result != null) {
                return unwrapObjectValue(result);
            }
        }

        return null;
    }

    private void cachePropertyValue(String propertyName, Object value) {
        if (propertyValues == null) {
            propertyValues = new HashMap<>();
        }
        propertyValues.put(propertyName, value);
    }

    private Object invokeRemoteMethod(@NotNull String methodName, @Nullable Object[] args, @Nullable Type returnType) throws DBException {
        DPIController controller = context.getDpiController();
        if (controller == null) {
            throw new DBException("No DPI controller in client context");
        }
        if (controller instanceof RestProxy restProxy) {
            restProxy.setNextCallResultType(returnType);
        }
        try {
            log.debug(MessageFormat.format("Call method: {0} object: {1}", methodName, objectId));
            var result = controller.callMethod(this.objectId, methodName, args);
            log.debug(MessageFormat.format("Return method result: {0} object: {1}", methodName, objectId));
            return result;
        } catch (Throwable e) {
            log.debug(MessageFormat.format("Method invocation error: {0} object: {1}", methodName, objectId));
            throw e;
        }
    }

    private static Object wrapObjectValue(Object result) {
        return result == null ? NULL_VALUE : result;
    }

    private static Object unwrapObjectValue(Object result) {
        return result == NULL_VALUE ? null : result;
    }

    private static String getPropertyKey(Method method, Property propAnnotation) {
        String propId = propAnnotation.id();
        if (CommonUtils.isEmpty(propId)) {
            propId = BeanUtils.getPropertyNameFromGetter(method.getName());
        }
        return propId;
    }

}
