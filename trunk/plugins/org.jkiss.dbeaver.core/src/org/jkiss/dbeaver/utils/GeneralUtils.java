/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * General non-ui utility methods
 */
public class GeneralUtils {

    static final Log log = Log.getLog(GeneralUtils.class);

    public static final String DEFAULT_FILE_CHARSET_NAME = "UTF-8";
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final Charset DEFAULT_FILE_CHARSET = UTF8_CHARSET;
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    public static final String[] byteToHex = new String[256];
    public static final char[] nibbleToHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static final Map<String, byte[]> BOM_MAP = new HashMap<String, byte[]>();
    static final char[] HEX_CHAR_TABLE = {
      '0', '1', '2', '3',
      '4', '5', '6', '7',
      '8', '9', 'a', 'b',
      'c', 'd', 'e', 'f'
    };
    private static final String BAD_DOUBLE_VALUE = "2.2250738585072012e-308"; //$NON-NLS-1$

    static {
        // Compose byte to hex map
        for (int i = 0; i < 256; ++i) {
            byteToHex[i] = Character.toString(nibbleToHex[i >>> 4]) + nibbleToHex[i & 0x0f];
        }
    }


    public static String getDefaultFileEncoding()
    {
        return System.getProperty("file.encoding", DEFAULT_FILE_CHARSET_NAME);
    }

    public static String getDefaultConsoleEncoding()
    {
        String consoleEncoding = System.getProperty("console.encoding");
        if (CommonUtils.isEmpty(consoleEncoding)) {
            consoleEncoding = getDefaultFileEncoding();
        }
        return consoleEncoding;
    }

    public static String getDefaultLineSeparator()
    {
        return System.getProperty("line.separator", "\n");
    }

    public static byte[] getCharsetBOM(String charsetName)
    {
        return BOM_MAP.get(charsetName.toUpperCase());
    }

    public static void writeByteAsHex(Writer out, byte b) throws IOException
    {
        int v = b & 0xFF;
        out.write(HEX_CHAR_TABLE[v >>> 4]);
        out.write(HEX_CHAR_TABLE[v & 0xF]);
    }

    public static void writeBytesAsHex(Writer out, byte[] buf, int off, int len) throws IOException
    {
        for (int i = 0; i < len; i++) {
            byte b = buf[off + i];
            int v = b & 0xFF;
            out.write(HEX_CHAR_TABLE[v >>> 4]);
            out.write(HEX_CHAR_TABLE[v & 0xF]);
        }
    }

    public static String getDefaultBinaryFileEncoding(DBPDataSource dataSource)
    {
        DBPPreferenceStore preferenceStore;
        if (dataSource == null) {
            preferenceStore = DBeaverCore.getGlobalPreferenceStore();
        } else {
            preferenceStore = dataSource.getContainer().getPreferenceStore();
        }
        String fileEncoding = preferenceStore.getString(DBeaverPreferences.CONTENT_HEX_ENCODING);
        if (CommonUtils.isEmpty(fileEncoding)) {
            fileEncoding = getDefaultFileEncoding();
        }
        return fileEncoding;
    }

    public static String convertToString(byte[] bytes, int offset, int length)
    {
        char[] chars = new char[length];
        for (int i = offset; i < offset + length; i++) {
            int b = bytes[i];
            if (b < 0) {
                b = -b + 127;
            }
            chars[i - offset] = (char) b;
        }
        return new String(chars);
    }

    /**
     * Converts string to byte array.
     * This is loosy algorithm because it gets only first byte from each char.
     *
     * @param strValue
     * @return
     */
    public static byte[] convertToBytes(String strValue)
    {
        int length = strValue.length();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int c = strValue.charAt(i) & 255;
            if (c > 127) {
                c = -(c - 127);
            }
            bytes[i] = (byte)c;
        }
        return bytes;
    }

    @Nullable
    public static Number convertStringToNumber(String text, Class<? extends Number> hintType, DBDDataFormatter formatter)
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
                return toDouble(text);
            } else if (hintType == BigInteger.class) {
                return new BigInteger(text);
            } else {
                return new BigDecimal(text);
            }
        } catch (NumberFormatException e) {
            log.debug("Bad numeric value '" + text + "' - " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                return (Number)formatter.parseValue(text, hintType);
            } catch (ParseException e1) {
                log.debug("Can't parse numeric value [" + text + "] using formatter", e);
                return null;
            }
        }
    }

    private static Number toDouble(String text)
    {
        if (text.equals(BAD_DOUBLE_VALUE)) {
            return Double.MIN_VALUE;
        }
        return Double.valueOf(text);
    }

    public static Object makeDisplayString(Object object)
    {
        if (object == null) {
            return ""; //$NON-NLS-1$
        }
        if (object instanceof Number) {
            return NumberFormat.getInstance().format(object);
        }
        Class<?> eClass = object.getClass();
        if (eClass.isArray()) {
            if (eClass == byte[].class)
                return Arrays.toString((byte[]) object);
            else if (eClass == short[].class)
                return Arrays.toString((short[]) object);
            else if (eClass == int[].class)
                return Arrays.toString((int[]) object);
            else if (eClass == long[].class)
                return Arrays.toString((long[]) object);
            else if (eClass == char[].class)
                return Arrays.toString((char[]) object);
            else if (eClass == float[].class)
                return Arrays.toString((float[]) object);
            else if (eClass == double[].class)
                return Arrays.toString((double[]) object);
            else if (eClass == boolean[].class)
                return Arrays.toString((boolean[]) object);
            else { // element is an array of object references
                return Arrays.deepToString((Object[]) object);
            }
        }
        return object;
    }

    public static Object convertString(String value, Class<?> valueType)
    {
        try {
            if (CommonUtils.isEmpty(value)) {
                return null;
            }
            if (valueType == null || CharSequence.class.isAssignableFrom(valueType)) {
                return value;
            } else if (valueType == Boolean.class || valueType == Boolean.TYPE) {
                return Boolean.valueOf(value);
            } else if (valueType == Long.class) {
                return Long.valueOf(value);
            } else if (valueType == Long.TYPE) {
                return Long.parseLong(value);
            } else if (valueType == Integer.class) {
                return new Integer(value);
            } else if (valueType == Integer.TYPE) {
                return Integer.parseInt(value);
            } else if (valueType == Short.class) {
                return Short.valueOf(value);
            } else if (valueType == Short.TYPE) {
                return Short.parseShort(value);
            } else if (valueType == Byte.class) {
                return Byte.valueOf(value);
            } else if (valueType == Byte.TYPE) {
                return Byte.parseByte(value);
            } else if (valueType == Double.class) {
                return Double.valueOf(value);
            } else if (valueType == Double.TYPE) {
                return Double.parseDouble(value);
            } else if (valueType == Float.class) {
                return Float.valueOf(value);
            } else if (valueType == Float.TYPE) {
                return Float.parseFloat(value);
            } else if (valueType == BigInteger.class) {
                return new BigInteger(value);
            } else if (valueType == BigDecimal.class) {
                return new BigDecimal(value);
            } else {
                return value;
            }
        } catch (RuntimeException e) {
            log.error(e);
            return value;
        }
    }

    public static Throwable getRootCause(Throwable ex) {
        for (Throwable e = ex; ; e = e.getCause()) {
            if (e.getCause() == null) {
                return e;
            }
        }
    }

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
                log.warn("Can't find property '" + propertyName + "' read method in '" + object.getClass().getName() + "'");
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
                log.warn("Can't read property '" + propertyName + "' - bad method signature: " + getter.toString());
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
}
