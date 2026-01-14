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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * DB value formatting utilities
 */
public final class DBValueFormatting {

    public static final DecimalFormat NATIVE_FLOAT_FORMATTER = new DecimalFormat("#.########", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    public static final DecimalFormat NATIVE_DOUBLE_FORMATTER = new DecimalFormat("#.################", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private static final Log log = Log.getLog(DBValueFormatting.class);

    static {
        //NATIVE_FLOAT_FORMATTER.setMaximumFractionDigits(NumberDataFormatter.MAX_FLOAT_FRACTION_DIGITS);
        NATIVE_FLOAT_FORMATTER.setDecimalSeparatorAlwaysShown(false);
        //NATIVE_FLOAT_FORMATTER.setRoundingMode(RoundingMode.UNNECESSARY);

        //NATIVE_DOUBLE_FORMATTER.setMaximumFractionDigits(340);
        NATIVE_DOUBLE_FORMATTER.setDecimalSeparatorAlwaysShown(false);
        //NATIVE_DOUBLE_FORMATTER.setRoundingMode(RoundingMode.UNNECESSARY);
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
        return switch (typedObject.getDataKind()) {
            case BOOLEAN -> DBIcon.TYPE_BOOLEAN;
            case STRING -> DBIcon.TYPE_STRING;
            case NUMERIC -> DBIcon.TYPE_NUMBER;
            case DATETIME -> DBIcon.TYPE_DATETIME;
            case BINARY -> DBIcon.TYPE_BINARY;
            case CONTENT -> {
                if (typeNameContains(typeName, DBConstants.TYPE_NAME_XML, DBConstants.TYPE_NAME_XML2)) {
                    yield DBIcon.TYPE_XML;
                } else if (typeNameContains(typeName, DBConstants.TYPE_NAME_CHAR, DBConstants.TYPE_NAME_CHAR2)) {
                    yield DBIcon.TYPE_TEXT;
                }
                yield DBIcon.TYPE_LOB;
            }
            case ARRAY -> DBIcon.TYPE_ARRAY;
            case STRUCT -> DBIcon.TYPE_STRUCT;
            case DOCUMENT -> DBIcon.TYPE_DOCUMENT;
            case REFERENCE -> DBIcon.TYPE_REFERENCE;
            case ROWID -> DBIcon.TYPE_ROWID;
            case OBJECT -> {
                if (typeNameContains(typeName, DBConstants.TYPE_NAME_UUID, DBConstants.TYPE_NAME_UUID2)) {
                    yield DBIcon.TYPE_UUID;
                } else if (typeNameContains(typeName, DBConstants.TYPE_NAME_JSON, DBConstants.TYPE_NAME_JSON2)) {
                    yield DBIcon.TYPE_JSON;
                }
                yield DBIcon.TYPE_OBJECT;
            }
            case ANY -> DBIcon.TYPE_ANY;
            default -> DBIcon.TYPE_UNKNOWN;
        };
    }

    private static boolean typeNameContains(String typeName, String patternLC, String patternUC) {
        return typeName != null &&
                (typeName.contains(patternLC) || typeName.contains(patternUC));
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
                if (object instanceof DBSEntity) {
                    if (DBUtils.isView((DBSEntity) object)) {
                        image = DBIcon.TREE_VIEW;
                    } else {
                        image = DBIcon.TREE_TABLE;
                    }
                } else if (object instanceof DBSEntityAssociation) {
                    image = DBIcon.TREE_ASSOCIATION;
                } else if (object instanceof DBSProcedure) {
                    image = DBIcon.TREE_PROCEDURE;
                } else if (object instanceof DBSPackage) {
                    image = DBIcon.TREE_PACKAGE;
                } else if (object instanceof DBSTrigger) {
                    image = DBIcon.TREE_TRIGGER;
                } else if (object instanceof DBSSchema s) {
                    if (s instanceof DBPSystemObject so && so.isSystem()) {
                        image = DBIcon.TREE_SCHEMA_SYSTEM;
                    } else {
                        image = DBIcon.TREE_SCHEMA;
                    }
                } else if (object instanceof DBSCatalog) {
                    image = DBIcon.TREE_DATABASE;
                } else {
                    image = DBIcon.TYPE_OBJECT;
                }
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
    public static Object convertStringToNumber(String text, Class<?> hintType, @NotNull DBDDataFormatter formatter, boolean validateValue) throws DBCException
    {
        if (text == null || text.isEmpty()) {
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
                return formatter.parseValue(text, hintType);
            } catch (ParseException e1) {
                if (validateValue) {
                    throw new DBCException("Can't parse numeric value [" + text + "] using formatter", e);
                }
                log.debug("Can't parse numeric value [" + text + "] using formatter: " + e.getMessage());
                return text;
            }
        }
    }

    public static String convertNumberToNativeString(Number value, boolean scientificNotation) {
        try {
            if (value instanceof BigDecimal) {
                return scientificNotation ?
                    value.toString() :
                    ((BigDecimal) value).toPlainString();
            } else {
                String strValue = value.toString();
                if (scientificNotation || strValue.indexOf('E') == -1) {
                    return strValue;
                }
                // We don't want exponential view
                if (value instanceof Float) {
                    return NATIVE_FLOAT_FORMATTER.format(value);
                } else if (value instanceof Double) {
                    return NATIVE_DOUBLE_FORMATTER.format(value);
                }
            }
        } catch (Exception e) {
            log.debug("Error converting number to string: " + e.getMessage());
        }
        return value.toString();
    }

    @Nullable
    public static Object convertDateToNumber(Date date, Class<?> hintType, @NotNull DBDDataFormatter formatter, boolean validateValue) throws DBCException
    {
        if (date == null) {
            return null;
        }
        try {
            if (hintType == Long.class) {
                try {
                    return date.getTime();
                } catch (NumberFormatException e) {
                    return (int) (date.getTime()/1000);
                }
            } else if (hintType == Integer.class) {
                return (int) (date.getTime()/1000);
            } else if (hintType == Float.class) {
                return Long.valueOf(date.getTime()).floatValue();
            } else if (hintType == Double.class) {
                return Long.valueOf(date.getTime()).doubleValue();
            } else {
                return new BigDecimal(date.getTime());
            }
        } catch (NumberFormatException e) {
            try {
                return formatter.parseValue(date.toString(), hintType);
            } catch (ParseException e1) {
                if (validateValue) {
                    throw new DBCException("Can't parse numeric value [" + date + "] using formatter", e);
                }
                log.debug("Can't parse numeric value [" + date + "] using formatter: " + e.getMessage());
                return date;
            }
        }
    }

    public static String formatBinaryString(@NotNull DBPDataSource dataSource, @NotNull byte[] data, @NotNull DBDDisplayFormat format) {
        return formatBinaryString(dataSource, data, format, false);
    }

    public static String formatBinaryString(@NotNull DBPDataSource dataSource, @NotNull byte[] data, @NotNull DBDDisplayFormat format, boolean forceLimit) {
        DBDBinaryFormatter formatter;
        if (format == DBDDisplayFormat.NATIVE) {
            formatter = dataSource.getSQLDialect().getNativeBinaryFormatter();
        } else {
            formatter = getBinaryPresentation(dataSource);
        }
        // Convert bytes to string
        int length = data.length;
        if (format == DBDDisplayFormat.UI || forceLimit) {
            int maxLength = dataSource.getContainer().getPreferenceStore().getInt(ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN);
            if (length > maxLength) {
                length = maxLength;
            }
        }
        String string = formatter.toString(data, 0, length);
        if (format == DBDDisplayFormat.NATIVE || length == data.length) {
            // Do not append ... for native formatter - it may contain expressions
            return string;
        }
        return string + "...";
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
        } else if (value instanceof CharSequence) {
            return value.toString();
        } else if (value.getClass().isArray()) {
            if (value.getClass().getComponentType() == Byte.TYPE) {
                byte[] bytes = (byte[]) value;
                int length = bytes.length;
                if (length > 2000) length = 2000;
                String string = CommonUtils.toHexString(bytes, 0, length);
                return bytes.length > 2000 ? string + "..." : string;
            } else {
                StringBuilder str = new StringBuilder("[");
                int length = Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    if (i > 0) str.append(", ");
                    str.append(getDefaultValueDisplayString(Array.get(value, i), format));
                }
                str.append("]");
                return str.toString();
            }
        } else if (value instanceof DBDComposite composite) {
            DBSAttributeBase[] attributes = composite.getAttributes();
            StringBuilder str = new StringBuilder("{");
            try {
                boolean first = true;
                for (DBSAttributeBase item : attributes) {
                    if (!first) str.append(", ");
                    first = false;
                    str.append(item.getName()).append(":");
                    Object attributeValue = composite.getAttributeValue(item);
                    str.append(getDefaultValueDisplayString(attributeValue, format));
                }
            } catch (DBCException e) {
                str.append(e.getMessage());
            }
            str.append("}");
            return str.toString();
        } else if (value instanceof Collection) {
            StringBuilder str = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Collection<?>)value) {
                if (!first) str.append(", ");
                first = false;
                str.append(getDefaultValueDisplayString(item, format));
            }
            str.append("]");
            return str.toString();
        } else if (value instanceof DBPNamedObject) {
            return ((DBPNamedObject) value).getName();
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
