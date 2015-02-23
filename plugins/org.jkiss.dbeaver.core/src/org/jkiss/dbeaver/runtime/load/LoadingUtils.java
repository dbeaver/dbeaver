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

package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.utils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loading utils
 */
public class LoadingUtils {

    static final Log log = Log.getLog(LoadingUtils.class);

    public static Object extractPropertyValue(DBRProgressMonitor monitor, Object object, String propertyName)
        throws DBException
    {
        // Read property using reflection
        if (object == null) {
            return null;
        }
        try {
            Method getter = findPropertyReadMethod(object.getClass(), propertyName);
            if (getter == null) {
                log.warn("Could not find property '" + propertyName + "' read method in '" + object.getClass().getName() + "'");
                return null;
            }
            Class<?>[] paramTypes = getter.getParameterTypes();
            if (paramTypes.length == 0) {
                // No params - just read it
                return getter.invoke(object);
            } else if (paramTypes.length == 1 && paramTypes[0] == DBRProgressMonitor.class) {
                // Read with progress monitor
                return getter.invoke(object, monitor);
            } else {
                log.warn("Could not read property '" + propertyName + "' - bad method signature: " + getter.toString());
                return null;
            }
        }
        catch (IllegalAccessException ex) {
            log.warn("Error accessing items " + propertyName, ex);
            return null;
        }
        catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof DBException) {
                throw (DBException) ex.getTargetException();
            }
            throw new DBException("Can't read " + propertyName, ex.getTargetException());
        }
    }

    public static Method findPropertyReadMethod(Class<?> clazz, String propertyName)
    {
        String methodName = BeanUtils.propertyNameToMethodName(propertyName);
        return findPropertyGetter(clazz, "get" + methodName, "is" + methodName);
    }

    private static Method findPropertyGetter(Class<?> clazz, String getName, String isName)
    {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (
                (!Modifier.isPublic(method.getModifiers())) ||
                (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) ||
                (method.getReturnType().equals(void.class)))
            {
                // skip
            } else if (method.getName().equals(getName) || (method.getName().equals(isName) && method.getReturnType().equals(boolean.class))) {
                // If it matches the get name, it's the right method
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0 || (parameterTypes.length == 1 && parameterTypes[0] == DBRProgressMonitor.class)) {
                    return method;
                }
            }
        }
        return clazz == Object.class ? null : findPropertyGetter(clazz.getSuperclass(), getName, isName);
    }

    public static <RESULT> LoadingJob<RESULT> createService(
        ILoadService<RESULT> loadingService,
        ILoadVisualizer<RESULT> visualizer)
    {
        return new LoadingJob<RESULT>(loadingService, visualizer);
    }

}
