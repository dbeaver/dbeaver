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
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * Hex formatter
 */
public class BinaryFormatterHex implements DBDBinaryFormatter {

    public static final BinaryFormatterHex INSTANCE = new BinaryFormatterHex();

    @Override
    public String getId()
    {
        return "hex";
    }

    @Override
    public String getTitle()
    {
        return "Hex";
    }

    @Override
    public String toString(byte[] bytes, int offset, int length)
    {
        return new String(toHexChars(bytes, offset, length));
    }

    protected static char[] toHexChars(byte[] bytes, int offset, int length) {
        char[] chars = new char[length * 2];
        for (int i = 0; i < length; i++) {
            String hex = GeneralUtils.byteToHex[bytes[offset + i] & 0x0ff];
            chars[i * 2] = hex.charAt(0);
            chars[i * 2 + 1] = hex.charAt(1);
        }
        return chars;
    }

    @Override
    public byte[] toBytes(String string)
    {
        int length = string.length();
        if (length > 0 && length % 2 != 0) {
            length--;
        }
        byte bytes[] = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                + Character.digit(string.charAt(i + 1), 16));
        }
        return bytes;
    }

}
