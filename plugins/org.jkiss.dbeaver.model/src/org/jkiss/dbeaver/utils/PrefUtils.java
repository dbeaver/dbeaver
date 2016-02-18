/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.utils;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Preferences utilities
 */
public class PrefUtils {

    static final Log log = Log.getLog(PrefUtils.class);

    public static void savePreferenceStore(DBPPreferenceStore store)
    {
        try {
            store.save();
        } catch (IOException e) {
            log.warn(e);
        }
    }

    public static void setDefaultPreferenceValue(DBPPreferenceStore store, String name, Object value)
    {
        if (CommonUtils.isEmpty(store.getDefaultString(name))) {
            store.setDefault(name, value.toString());
        }
    }

    public static Object getPreferenceValue(DBPPreferenceStore store, String propName, Class<?> valueType)
    {
        try {
            if (valueType == null || CharSequence.class.isAssignableFrom(valueType)) {
                final String str = store.getString(propName);
                return CommonUtils.isEmpty(str) ? null : str;
            } else if (valueType == Boolean.class || valueType == Boolean.TYPE) {
                return store.getBoolean(propName);
            } else if (valueType == Long.class || valueType == Long.TYPE) {
                return store.getLong(propName);
            } else if (valueType == Integer.class || valueType == Integer.TYPE ||
                valueType == Short.class || valueType == Short.TYPE ||
                valueType == Byte.class || valueType == Byte.TYPE) {
                return store.getInt(propName);
            } else if (valueType == Double.class || valueType == Double.TYPE) {
                return store.getDouble(propName);
            } else if (valueType == Float.class || valueType == Float.TYPE) {
                return store.getFloat(propName);
            } else if (valueType == BigInteger.class) {
                final String str = store.getString(propName);
                return str == null ? null : new BigInteger(str);
            } else if (valueType == BigDecimal.class) {
                final String str = store.getString(propName);
                return str == null ? null : new BigDecimal(str);
            }
        } catch (RuntimeException e) {
            log.error(e);
        }
        final String string = store.getString(propName);
        return CommonUtils.isEmpty(string) ? null : string;
    }

    public static void setPreferenceValue(DBPPreferenceStore store, String propName, Object value)
    {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequence) {
            store.setValue(propName, value.toString());
        } else if (value instanceof Boolean) {
            store.setValue(propName, (Boolean) value);
        } else if (value instanceof Long) {
            store.setValue(propName, (Long) value);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            store.setValue(propName, ((Number) value).intValue());
        } else if (value instanceof Double) {
            store.setValue(propName, (Double) value);
        } else if (value instanceof Float) {
            store.setValue(propName, (Float) value);
        } else {
            store.setValue(propName, value.toString());
        }
    }

    public static void setPreferenceDefaultValue(DBPPreferenceStore store, String propName, Object value)
    {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequence) {
            store.setDefault(propName, value.toString());
        } else if (value instanceof Boolean) {
            store.setDefault(propName, (Boolean) value);
        } else if (value instanceof Long) {
            store.setDefault(propName, (Long) value);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            store.setDefault(propName, ((Number) value).intValue());
        } else if (value instanceof Double) {
            store.setDefault(propName, (Double) value);
        } else if (value instanceof Float) {
            store.setDefault(propName, (Float) value);
        } else {
            store.setDefault(propName, value.toString());
        }
    }
}
