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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * DB value formatting utilities
 */
public final class DBValueFormatting {

    public static final DecimalFormat NATIVE_DECIMAL_FORMATTER = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private static final Log log = Log.getLog(DBValueFormatting.class);

    static {
        DBValueFormatting.NATIVE_DECIMAL_FORMATTER.setMaximumFractionDigits(340);
    }

    @NotNull
    public static DBPImage getTypeImage(@NotNull DBSTypedObject typedObject)
    {
        if (typedObject instanceof DBSTypedObjectEx) {
            DBSDataType dataType = ((DBSTypedObjectEx) typedObject).getDataType();
            if (dataType instanceof DBPImageProvider) {
                DBPImage image = ((DBPImageProvider) dataType).getObjectImage();
                if (image != null) {
                    return image;
                }
            }
        }
        return getDefaultTypeImage(typedObject);
    }

    @NotNull
    public static DBPImage getDefaultTypeImage(DBSTypedObject typedObject) {
        String typeName = typedObject.getTypeName();
        switch (typedObject.getDataKind()) {
            case BOOLEAN:
                return DBIcon.TYPE_BOOLEAN;
            case STRING:
                return DBIcon.TYPE_STRING;
            case NUMERIC:
                return DBIcon.TYPE_NUMBER;
            case DATETIME:
                return DBIcon.TYPE_DATETIME;
            case BINARY:
                return DBIcon.TYPE_BINARY;
            case CONTENT:
                if (typeName.contains("XML") || typeName.contains("xml")) {
                    return DBIcon.TYPE_XML;
                } else if (typeName.contains("CHAR") || typeName.contains("char")) {
                    return DBIcon.TYPE_TEXT;
                }
                return DBIcon.TYPE_LOB;
            case ARRAY:
                return DBIcon.TYPE_ARRAY;
            case STRUCT:
                return DBIcon.TYPE_STRUCT;
            case DOCUMENT:
                return DBIcon.TYPE_DOCUMENT;
            case REFERENCE:
                return DBIcon.TYPE_REFERENCE;
            case ROWID:
                return DBIcon.TYPE_ROWID;
            case OBJECT:
                if (typeName.contains(DBConstants.TYPE_NAME_UUID) || typeName.contains(DBConstants.TYPE_NAME_UUID2)) {
                    return DBIcon.TYPE_UUID;
                }
                return DBIcon.TYPE_OBJECT;
            case ANY:
                return DBIcon.TYPE_ANY;
            default:
                return DBIcon.TYPE_UNKNOWN;
        }
    }

    @NotNull
    public static DBPImage getObjectImage(DBPObject object)
    {
        return getObjectImage(object, true);
    }

    @Nullable
    public static DBPImage getObjectImage(DBPObject object, boolean useDefault)
    {
        DBPImage image = null;
        if (object instanceof DBPImageProvider) {
            image = ((DBPImageProvider)object).getObjectImage();
        }
        if (image == null) {
            if (object instanceof DBSTypedObject) {
                image = getTypeImage((DBSTypedObject) object);
            }
            if (image == null && useDefault) {
                image = DBIcon.TYPE_OBJECT;
            }
        }
        return image;
    }

    @NotNull
    public static DBDBinaryFormatter getBinaryPresentation(@NotNull DBPDataSource dataSource)
    {
        String id = dataSource.getContainer().getPreferenceStore().getString(ModelPreferences.RESULT_SET_BINARY_PRESENTATION);
        if (id != null) {
            DBDBinaryFormatter formatter = getBinaryPresentation(id);
            if (formatter != null) {
                return formatter;
            }
        }
        return DBConstants.BINARY_FORMATS[0];
    }

    @Nullable
    public static DBDBinaryFormatter getBinaryPresentation(String id)
    {
        for (DBDBinaryFormatter formatter : DBConstants.BINARY_FORMATS) {
            if (formatter.getId().equals(id)) {
                return formatter;
            }
        }
        return null;
    }

    public static String getDefaultBinaryFileEncoding(@NotNull DBPDataSource dataSource)
    {
        DBPPreferenceStore preferenceStore = dataSource.getContainer().getPreferenceStore();
        String fileEncoding = preferenceStore.getString(ModelPreferences.CONTENT_HEX_ENCODING);
        if (CommonUtils.isEmpty(fileEncoding)) {
            fileEncoding = GeneralUtils.getDefaultFileEncoding();
        }
        return fileEncoding;
    }

    @Nullable
    public static Number convertStringToNumber(String text, Class<?> hintType, @NotNull DBDDataFormatter formatter)
    {
        if (text == null || text.length() == 0) {
            return null;
        }
        try {
            if (hintType == Long.class) {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException e) {
                    return new BigInteger(text);
                }
            } else if (hintType == Integer.class) {
                return Integer.valueOf(text);
            } else if (hintType == Short.class) {
                return Short.valueOf(text);
            } else if (hintType == Byte.class) {
                return Byte.valueOf(text);
            } else if (hintType == Float.class) {
                return Float.valueOf(text);
            } else if (hintType == Double.class) {
                return Double.valueOf(text);
            } else if (hintType == BigInteger.class) {
                return new BigInteger(text);
            } else {
                return new BigDecimal(text);
            }
        } catch (NumberFormatException e) {
            try {
                return (Number)formatter.parseValue(text, hintType);
            } catch (ParseException e1) {
                log.debug("Can't parse numeric value [" + text + "] using formatter: " + e.getMessage());
                return null;
            }
        }
    }

    public static String convertNumberToNativeString(Number value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        } else if (value instanceof Float || value instanceof Double) {
            return NATIVE_DECIMAL_FORMATTER.format(value);
        } else {
            return value.toString();
        }

    }

    public static String getBooleanString(boolean propertyValue) {
        return propertyValue ? DBConstants.BOOLEAN_PROP_YES : DBConstants.BOOLEAN_PROP_NO;
    }

    public static String formatBinaryString(@NotNull DBPDataSource dataSource, @NotNull byte[] data, @NotNull DBDDisplayFormat format) {
        DBDBinaryFormatter formatter;
        if (format == DBDDisplayFormat.NATIVE && dataSource instanceof SQLDataSource) {
            formatter = ((SQLDataSource) dataSource).getSQLDialect().getNativeBinaryFormatter();
        } else {
            formatter = getBinaryPresentation(dataSource);
        }
        // Convert bytes to string
        int length = data.length;
        if (format == DBDDisplayFormat.UI) {
            int maxLength = dataSource.getContainer().getPreferenceStore().getInt(ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN);
            if (length > maxLength) {
                length = maxLength;
            }
        }
        String string = formatter.toString(data, 0, length);
        if (length == data.length) {
            return string;
        }
        return string + "..." + " [" + data.length + "]";
    }

    @NotNull
    public static String getDefaultValueDisplayString(@Nullable Object value, @NotNull DBDDisplayFormat format)
    {
        if (DBUtils.isNullValue(value)) {
            if (format == DBDDisplayFormat.UI) {
                return DBConstants.NULL_VALUE_LABEL;
            } else {
                return "";
            }
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        if (value.getClass().isArray()) {
            if (value.getClass().getComponentType() == Byte.TYPE) {
                byte[] bytes = (byte[]) value;
                return CommonUtils.toHexString(bytes, 0, 2000);
            } else {
                return GeneralUtils.makeDisplayString(value).toString();
            }
        }
        String className = value.getClass().getName();
        if (className.startsWith("java.lang") || className.startsWith("java.util")) {
            // Standard types just use toString
            return value.toString();
        }
        // Unknown types print their class name
        boolean hasToString;
        try {
            hasToString = value.getClass().getMethod("toString").getDeclaringClass() != Object.class;
        } catch (Throwable e) {
            log.debug(e);
            hasToString = false;
        }
        if (hasToString) {
            return value.toString();
        } else {
            return "[" + value.getClass().getSimpleName() + "]";
        }
    }
}
