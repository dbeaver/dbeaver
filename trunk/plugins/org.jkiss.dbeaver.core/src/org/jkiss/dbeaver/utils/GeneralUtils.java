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

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * General non-ui utility methods
 */
public class GeneralUtils {


    public static final String DEFAULT_FILE_CHARSET_NAME = "UTF-8";
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final Charset DEFAULT_FILE_CHARSET = UTF8_CHARSET;
    public static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    static final Map<String, byte[]> BOM_MAP = new HashMap<String, byte[]>();
    static final char[] HEX_CHAR_TABLE = {
      '0', '1', '2', '3',
      '4', '5', '6', '7',
      '8', '9', 'a', 'b',
      'c', 'd', 'e', 'f'
    };

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
        IPreferenceStore preferenceStore;
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
}
