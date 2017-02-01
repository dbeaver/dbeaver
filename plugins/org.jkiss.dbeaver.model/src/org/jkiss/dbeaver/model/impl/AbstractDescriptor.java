/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

/**
 * EntityEditorDescriptor
 */
public abstract class AbstractDescriptor {

    private static final Log log = Log.getLog(AbstractDescriptor.class);

    public static final String VAR_OBJECT = "object";
    public static final String VAR_CONTEXT = "context";

    private static JexlEngine jexlEngine;

    public static Expression parseExpression(String exprString) throws DBException
    {
        synchronized (AbstractDescriptor.class) {
            if (jexlEngine == null) {
                jexlEngine = new JexlEngine(null, null, null, null);
                jexlEngine.setCache(100);
            }
        }
        try {
            return jexlEngine.createExpression(exprString);
        } catch (JexlException e) {
            throw new DBException("Bad expression", e);
        }
    }

    public class ObjectType {
        private static final String ATTR_NAME = "name";
        private static final String ATTR_IF = "if";
        private static final String ATTR_FORCE_CHECK = "forceCheck";

        private final String implName;
        private Class<?> implClass;
        private Expression expression;
        private boolean forceCheck;

        public ObjectType(String implName)
        {
            this.implName = implName;
        }

        public ObjectType(IConfigurationElement cfg)
        {
            this.implName = cfg.getAttribute(ATTR_NAME);
            String condition = cfg.getAttribute(ATTR_IF);
            if (!CommonUtils.isEmpty(condition)) {
                try {
                    this.expression = parseExpression(condition);
                } catch (DBException ex) {
                    log.warn("Can't parse object type expression: " + condition, ex); //$NON-NLS-1$
                }
            }
            String fcAttr = cfg.getAttribute(ATTR_FORCE_CHECK);
            if (!CommonUtils.isEmpty(fcAttr)) {
                forceCheck = CommonUtils.toBoolean(fcAttr);
            }
        }

        public String getImplName()
        {
            return implName;
        }

        public Class<?> getObjectClass()
        {
            return getObjectClass(Object.class);
        }

        public <T> Class<? extends T> getObjectClass(Class<T> type)
        {
            if (implName == null) {
                return null;
            }
            if (implClass == null) {
                implClass = AbstractDescriptor.this.getObjectClass(implName, type);
            }
            return (Class<? extends T>) implClass;
        }

        public <T> void checkObjectClass(Class<T> type)
            throws DBException
        {
            Class<? extends T> objectClass = getObjectClass(type);
            if (objectClass == null) {
                throw new DBException("Class '" + implName + "' not found");
            }
            if (!type.isAssignableFrom(objectClass)) {
                throw new DBException("Class '" + implName + "' do not implements '" + type.getName() + "'");
            }
        }

        public boolean appliesTo(Object object, Object context)
        {
            if (!matchesType(object.getClass())) {
                return false;
            }
            if (expression != null) {
                Object result = expression.evaluate(makeContext(object, context));
                return Boolean.TRUE.equals(result);
            }
            return true;
        }

        public <T> T createInstance(Class<T> type)
            throws DBException
        {
            if (implName == null) {
                throw new DBException("No implementation class name set for '" + type.getName() + "'");
            }
            Class<? extends T> objectClass = getObjectClass(type);
            if (objectClass == null) {
                throw new DBException("Can't load class '" + getImplName() + "'");
            }
            try {
                return objectClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new DBException("Can't instantiate class '" + getImplName() + "'", e);
            }
        }

        public boolean matchesType(Class<?> clazz)
        {
            if (!forceCheck && getContributorBundle().getState() != Bundle.ACTIVE) {
                return false;
            }
            getObjectClass();
            return implClass != null && implClass.isAssignableFrom(clazz);
        }

        private JexlContext makeContext(final Object object, final Object context)
        {
            return new JexlContext() {
                @Override
                public Object get(String name)
                {
                    return name.equals(VAR_OBJECT) ? object :
                        (name.equals(VAR_CONTEXT) ? context : null); //$NON-NLS-1$
                }

                @Override
                public void set(String name, Object value)
                {
                    log.warn("Set is not implemented"); //$NON-NLS-1$
                }

                @Override
                public boolean has(String name)
                {
                    return
                        name.equals(VAR_OBJECT) && object != null || //$NON-NLS-1$
                        name.equals(VAR_CONTEXT) && context != null; //$NON-NLS-1$
                }
            };
        }
    }

    private String pluginId;
    private Bundle originBundle;

    protected AbstractDescriptor(IConfigurationElement contributorConfig)
    {
        this.pluginId = contributorConfig.getContributor().getName();
    }

    protected AbstractDescriptor(String pluginId)
    {
        this.pluginId = pluginId;
    }

    public String getPluginId()
    {
        return pluginId;
    }

    public Bundle getContributorBundle()
    {
        if (originBundle == null) {
            originBundle = Platform.getBundle(pluginId);
        }
        return originBundle;
    }

    @NotNull
    protected DBPImage iconToImage(String icon, @NotNull DBPImage defIcon)
    {
        DBPImage result = iconToImage(icon);
        if (result == null) {
            return defIcon;
        } else {
            return result;
        }
    }

    @Nullable
    protected DBPImage iconToImage(String icon)
    {
        if (CommonUtils.isEmpty(icon)) {
            return null;
        } else if (icon.startsWith("#")) {
            // Predefined image
            return DBIcon.getImageById(icon.substring(1));
        } else {
            if (!icon.startsWith("platform:")) {
                icon = "platform:/plugin/" + pluginId + "/" + icon;
            }
            return new DBIcon(icon);
        }
    }

    public Class<?> getObjectClass(@NotNull String className)
    {
        return getObjectClass(className, null);
    }

    public <T> Class<T> getObjectClass(@NotNull String className, Class<T> type)
    {
        return getObjectClass(getContributorBundle(), className, type);
    }

    public static <T> Class<T> getObjectClass(@NotNull Bundle fromBundle, @NotNull String className, Class<T> type)
    {
        Class<?> objectClass;
        try {
            objectClass = fromBundle.loadClass(className);
        } catch (Throwable ex) {
            log.error("Can't determine object class '" + className + "'", ex);
            return null;
        }

        if (type != null && !type.isAssignableFrom(objectClass)) {
            log.error("Object class '" + className + "' doesn't match requested type '" + type.getName() + "'");
            return null;
        }
        return (Class<T>) objectClass;
    }

}