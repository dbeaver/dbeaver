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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.utils.Base64;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * General non-ui utility methods
 */
public class GeneralUtils {

    private static final Log log = Log.getLog(GeneralUtils.class);

    public static final String UTF8_ENCODING = "UTF-8";
    public static final String DEFAULT_ENCODING = UTF8_ENCODING;

    public static final Charset UTF8_CHARSET = Charset.forName(UTF8_ENCODING);
    public static final Charset DEFAULT_FILE_CHARSET = UTF8_CHARSET;
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");

    public static final String[] byteToHex = new String[256];
    public static final char[] nibbleToHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static final Map<String, byte[]> BOM_MAP = new HashMap<>();
    static final char[] HEX_CHAR_TABLE = {
      '0', '1', '2', '3',
      '4', '5', '6', '7',
      '8', '9', 'a', 'b',
      'c', 'd', 'e', 'f'
    };

    static {
        // Compose byte to hex map
        for (int i = 0; i < 256; ++i) {
            byteToHex[i] = Character.toString(nibbleToHex[i >>> 4]) + nibbleToHex[i & 0x0f];
        }
    }

    public static Pattern VAR_PATTERN = Pattern.compile("(\\$\\{([\\w\\.\\-]+)\\})", Pattern.CASE_INSENSITIVE);

    /**
     * Default encoding (UTF-8)
     */
    public static String getDefaultFileEncoding()
    {
        return UTF8_ENCODING;
        //return System.getProperty("file.encoding", DEFAULT_FILE_CHARSET_NAME);
    }

    public static String getDefaultLocalFileEncoding()
    {
        return System.getProperty(StandardConstants.ENV_FILE_ENCODING, getDefaultFileEncoding());
    }


    public static String getDefaultConsoleEncoding()
    {
        String consoleEncoding = System.getProperty(StandardConstants.ENV_CONSOLE_ENCODING);
        if (CommonUtils.isEmpty(consoleEncoding)) {
            consoleEncoding = System.getProperty(StandardConstants.ENV_FILE_ENCODING);
        }
        if (CommonUtils.isEmpty(consoleEncoding)) {
            consoleEncoding = getDefaultFileEncoding();
        }
        return consoleEncoding;
    }

    public static String getDefaultLineSeparator()
    {
        return System.getProperty(StandardConstants.ENV_LINE_SEPARATOR, "\n");
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

    public static String convertToString(byte[] bytes, int offset, int length)
    {
        char[] chars = new char[length];
        for (int i = offset; i < offset + length; i++) {
            int b = bytes[i];
            if (b < 0) b = -b + 127;
            if (b < 32) b = 32;
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

    public static IStatus makeInfoStatus(String message) {
        return new Status(
            IStatus.INFO,
            ModelPreferences.PLUGIN_ID,
            message,
            null);
    }

    public interface IVariableResolver {
        String get(String name);
    }

    public static class MapResolver implements IVariableResolver {
        private final Map<String, Object> variables;

        public MapResolver(Map<String, Object> variables) {
            this.variables = variables;
        }

        @Override
        public String get(String name) {
            Object value = variables.get(name);
            return value == null ? null : CommonUtils.toString(value);
        }
    }

    public static String variablePattern(String name) {
        return "${" + name + "}";
    }

    public static String replaceVariables(String string, IVariableResolver resolver) {
        try {
            Matcher matcher = VAR_PATTERN.matcher(string);
            int pos = 0;
            while (matcher.find(pos)) {
                pos = matcher.end();
                String varName = matcher.group(2);
                String varValue = resolver.get(varName);
                if (varValue != null) {
                    if (matcher.start() == 0 && matcher.end() == string.length() - 1) {
                        string = varValue;
                    } else {
                        string = string.substring(0, matcher.start()) + varValue + string.substring(matcher.end());
                    }
                    matcher = VAR_PATTERN.matcher(string);
                    pos = 0;
                }
            }
            return string;
        } catch (Exception e) {
            log.warn("Error matching regex", e);
            return string;
        }
    }

    public static String[] parseCommandLine(String commandLine) {
        StringTokenizer st = new StringTokenizer(commandLine);
        String[] args = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            args[i] = st.nextToken();
        }
        return args;
    }

    public static IStatus makeExceptionStatus(Throwable ex)
    {
        return makeExceptionStatus(IStatus.ERROR, ex);
    }

    public static IStatus makeExceptionStatus(int severity, Throwable ex)
    {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return new Status(
                severity,
                ModelPreferences.PLUGIN_ID,
                getExceptionMessage(ex),
                ex);
        } else {
            if (ex instanceof DBException && CommonUtils.equalObjects(ex.getMessage(), cause.getMessage())) {
                // Skip empty duplicate DBException
                return makeExceptionStatus(cause);
            }
            return new MultiStatus(
                ModelPreferences.PLUGIN_ID,
                0,
                new IStatus[]{makeExceptionStatus(severity, cause)},
                getExceptionMessage(ex),
                ex);
        }
    }

    public static IStatus makeExceptionStatus(String message, Throwable ex)
    {
        return makeExceptionStatus(IStatus.ERROR, message, ex);
    }

    public static IStatus makeExceptionStatus(int severity, String message, Throwable ex)
    {
        return new MultiStatus(
            ModelPreferences.PLUGIN_ID,
            0,
            new IStatus[]{makeExceptionStatus(severity, ex)},
            message,
            null);
    }

    public static IStatus getRootStatus(IStatus status) {
        IStatus[] children = status.getChildren();
        if (children == null || children.length == 0) {
            return status;
        } else {
            return getRootStatus(children[0]);
        }
    }

    public static String getStatusText(IStatus status) {
        String text = status.getMessage();
        IStatus[] children = status.getChildren();
        if (children != null && children.length > 0) {
            for (IStatus child : children) {
                text += "\n" + getStatusText(child);
            }
        }
        return text;
    }

    /**
     * Returns first non-null and non-empty message from this exception or it's cause
     */
    public static String getFirstMessage(Throwable ex)
    {
        for (Throwable e = ex; e != null; e = e.getCause()) {
            String message = e.getMessage();
            if (!CommonUtils.isEmpty(message)) {
                return message;
            }
        }
        return null;
    }

    @NotNull
    public static String getExceptionMessage(@NotNull Throwable ex)
    {
/*
        StringBuilder msg = new StringBuilder(*/
/*CommonUtils.getShortClassName(ex.getClass())*//*
);
        msg.append(ex.getClass().getSimpleName());
        if (ex.getMessage() != null) {
            msg.append(": ").append(ex.getMessage());
        }
        return msg.toString().trim();
*/
        return ex.getMessage();
    }

    @NotNull
    public static String serializeObject(@NotNull Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream os = new ObjectOutputStream(baos)) {
                os.writeObject(object);
            }
            return Base64.encode(baos.toByteArray());
        } catch (Throwable e) {
            log.warn("Error serializing object [" + object + "]", e);
            return "";
        }
    }

    public static Object deserializeObject(String text) {
        try {
            final byte[] bytes = Base64.decode(text);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try (ObjectInputStream is = new ObjectInputStream(bais)) {
                return is.readObject();
            }
        } catch (Throwable e) {
            log.warn("Error deserializing object [" + text + "]", e);
            return null;
        }
    }

    public static File getMetadataFolder() {
        return Platform.getLogFileLocation().toFile().getParentFile();
    }

}
