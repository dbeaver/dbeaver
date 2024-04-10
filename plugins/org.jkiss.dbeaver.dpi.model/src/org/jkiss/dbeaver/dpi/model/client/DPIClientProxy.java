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
package org.jkiss.dbeaver.dpi.model.client;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.dpi.model.adapters.DPISerializer;
import org.jkiss.dbeaver.model.dpi.*;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.rest.RestProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Arrays;
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
    private Map<String, Object> objectContainers;
    private Map<String, Object> propertyValues;
    private Map<Class<?>, Object> factoryObjects;

    public DPIClientProxy(
        @NotNull DPIContext context,
        @NotNull Class<?>[] allInterfaces,
        @NotNull String objectId,
        @Nullable String objectType,
        @Nullable String objectToString,
        @Nullable Integer objectHashCode,
        @Nullable Map<String, Object> objectContainers,
        @Nullable Map<String, Object> propertyValues) {
        this.context = context;
        this.objectId = objectId;
        this.objectType = objectType;
        this.objectToString = objectToString;
        this.objectHashCode = objectHashCode;
        this.objectContainers = objectContainers;
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

        DPIContainer containerAnno = DPISerializer.getMethodAnno(method, DPIContainer.class);
        if (containerAnno != null) {
            if (containerAnno.root()) {
                return context.getRootObject();
            } else if (objectContainers != null) {
                Object container = objectContainers.get(methodName);
                if (container != null) {
                    if (container == SELF_REFERENCE) {
                        return objectInstance;
                    }
                    return unwrapObjectValue(container);
                }
            }
        }

        boolean isElement = DPISerializer.getMethodAnno(method, DPIElement.class) != null ||
            method.getDeclaringClass().getAnnotation(DPIElement.class) != null;
        if (isElement && propertyValues != null) {
            Object result = propertyValues.get(getElementKey(method, args));
            if (result == null && method.getParameterTypes().length == 0) {
                // Try property
                result = propertyValues.get(BeanUtils.getPropertyNameFromGetter(method.getName()));
            }
            if (result != null) {
                return unwrapObjectValue(result);
            }
        }

        Property propAnnotation = method.getAnnotation(Property.class);
        if (propAnnotation != null && propertyValues != null) {
            Object result = propertyValues.get(getPropertyKey(method, propAnnotation));
            if (result != null) {
                return unwrapObjectValue(result);
            }
        }

        // If method is property read or state read the try lookup in the context cache first
        DPIFactory dpiFactory = DPISerializer.getMethodAnno(method, DPIFactory.class);
        Class<?> dpiFactoryClass = null;
        if (dpiFactory != null) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0] == Class.class) {
                // Return type is specified in the first factory parameter
                dpiFactoryClass = (Class<?>) args[0];

                if (factoryObjects != null) {
                    Object cachedResult = factoryObjects.get(dpiFactoryClass);
                    if (cachedResult != null) {
                        return unwrapObjectValue(cachedResult);
                    }
                }
            }
        }
        Type returnType = dpiFactoryClass != null ? dpiFactoryClass : method.getGenericReturnType();

        Object result = invokeRemoteMethod(methodName, args, returnType);

        if (propAnnotation != null) {
            // Cache property value
            cachePropertyValue(getPropertyKey(method, propAnnotation), wrapObjectValue(result));
        } else if (dpiFactoryClass != null) {
            // Cache factory result
            if (factoryObjects == null) {
                factoryObjects = new HashMap<>();
            }
            factoryObjects.put(dpiFactoryClass, wrapObjectValue(result));
        } else if (isElement) {
            cachePropertyValue(getElementKey(method, args), wrapObjectValue(result));
        } else if (containerAnno != null) {
            if (objectContainers == null) {
                objectContainers = new HashMap<>();
            }
            objectContainers.put(methodName, wrapObjectValue(result));
        }

        return result;
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
        boolean expectSmartProxy = args != null && Arrays.stream(args).anyMatch(
            argument -> (argument != null && DPISerializer.isSmartObject(argument.getClass()))
        );
        if (controller instanceof RestProxy restProxy) {
            restProxy.setNextCallResultType(expectSmartProxy ? DPISmartObjectResponse.class : returnType);
        }
        try {
            log.debug(MessageFormat.format("Call method: {0} object: {1}", methodName, objectId));
            var result = controller.callMethod(this.objectId, methodName, args);
            if (expectSmartProxy && result instanceof DPISmartObjectResponse smartResponse) {
                var gson = context.getGson();
                result = gson.fromJson(gson.toJson(smartResponse.getMethodInvocationResult()), returnType);
                for (DPISmartObjectWrapper smartObject : smartResponse.getSmartObjects()) {
                    Object realObject = args[smartObject.getArgumentNumber()];
                    if (smartObject.getProxyObject() != null && smartObject.getProxyObject() instanceof DPISmartCallback
                        dpiClientSmartObject) {
                        dpiClientSmartObject.callback(realObject);
                    }
                }
            }
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

    private static String getElementKey(Method method, Object[] args) {
        if (!ArrayUtils.isEmpty(args)) {
            StringBuilder buf = new StringBuilder(method.getName());
            for (Object arg : args) {
                if (arg instanceof DBRProgressMonitor) {
                    continue;
                }
                buf.append(":").append(arg);
            }
            return buf.toString();
        } else {
            return method.getName();
        }
    }

}
