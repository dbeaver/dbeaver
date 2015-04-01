/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverIcons;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;

/**
 * EntityEditorDescriptor
 */
public abstract class AbstractDescriptor {

    static final Log log = Log.getLog(AbstractDescriptor.class);

    public static final String VAR_OBJECT = "object";
    public static final String VAR_CONTEXT = "context";

    protected class ObjectType {
        private final String implName;
        private Class<?> implClass;
        private Expression expression;

        public ObjectType(String implName)
        {
            this.implName = implName;
        }

        ObjectType(IConfigurationElement cfg)
        {
            this.implName = cfg.getAttribute(RegistryConstants.ATTR_NAME);
            String condition = cfg.getAttribute(RegistryConstants.ATTR_IF);
            if (!CommonUtils.isEmpty(condition)) {
                try {
                    this.expression = RuntimeUtils.parseExpression(condition);
                } catch (DBException ex) {
                    log.warn("Can't parse object type expression: " + condition, ex); //$NON-NLS-1$
                }
            }
        }

        public String getImplName()
        {
            return implName;
        }

        public Class getObjectClass()
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
            } catch (InstantiationException e) {
                throw new DBException("Can't instantiate class '" + getImplName() + "'", e);
            } catch (IllegalAccessException e) {
                throw new DBException("Can't instantiate class '" + getImplName() + "'", e);
            }
        }

        public boolean matchesType(Class<?> clazz)
        {
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
        return Platform.getBundle(pluginId);
    }

    protected Image iconToImage(String icon)
    {
        if (CommonUtils.isEmpty(icon)) {
            return null;
        } else if (icon.startsWith("#")) {
            // Predefined image
            return DBeaverIcons.getImage(icon.substring(1));
        } else {
            URL iconPath = getContributorBundle().getEntry(icon);
            if (iconPath != null) {
                try {
                    iconPath = FileLocator.toFileURL(iconPath);
                }
                catch (IOException ex) {
                    log.error(ex);
                    return null;
                }
                ImageDescriptor descriptor = ImageDescriptor.createFromURL(iconPath);
                return descriptor.createImage();
            }
        }
        return null;
    }

    public Class<?> getObjectClass(String className)
    {
        return getObjectClass(className, null);
    }

    public <T> Class<T> getObjectClass(String className, Class<T> type)
    {
        return getObjectClass(getContributorBundle(), className, type);
    }

    public static <T> Class<T> getObjectClass(Bundle fromBundle, String className, Class<T> type)
    {
        Class<?> objectClass = null;
        try {
            objectClass = DBeaverActivator.getInstance().getBundle().loadClass(className);
        } catch (Throwable ex) {
            // do nothing
            //log.warn("Can't load object class '" + className + "'", ex);
        }

        if (objectClass == null) {
            try {
                objectClass = fromBundle.loadClass(className);
            } catch (Throwable ex) {
                log.error("Can't determine object class '" + className + "'", ex);
                return null;
            }
        }
        if (type != null && !type.isAssignableFrom(objectClass)) {
            log.error("Object class '" + className + "' doesn't match requested type '" + type.getName() + "'");
            return null;
        }
        return (Class<T>) objectClass;
    }

}