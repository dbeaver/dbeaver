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

/**
 * Hex formatter.
 * Formats binary data to hex with preceding 0x
 */
public class BinaryFormatterHexNative extends BinaryFormatterHex {

    public static final BinaryFormatterHexNative INSTANCE = new BinaryFormatterHexNative();

    private static final String HEX_PREFIX = "0x";
    private static final String HEX_PREFIX2 = "0X";

    @Override
    public String getId()
    {
        return "hex_native";
    }

    @Override
    public String getTitle()
    {
        return "Hex";
    }

    @Override
    public String toString(byte[] bytes, int offset, int length)
    {
        return HEX_PREFIX + super.toString(bytes, offset, length);
    }

    @Override
    public byte[] toBytes(String string)
    {
        if (string.startsWith(HEX_PREFIX) || string.startsWith(HEX_PREFIX2)) {
            string = string.substring(2);
        }
        return super.toBytes(string);
    }

}
