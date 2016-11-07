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
 * Formats binary data to hex with preceding x'0123456789ABCDEF'
 */
public class BinaryFormatterHexString extends BinaryFormatterHex {

    public static final BinaryFormatterHexString INSTANCE = new BinaryFormatterHexString();

    private static final String HEX_PREFIX = "x'";
    private static final String HEX_POSTFIX = "'";

    @Override
    public String getId()
    {
        return "hex_string";
    }

    @Override
    public String getTitle()
    {
        return "Hex";
    }

    @Override
    public String toString(byte[] bytes, int offset, int length)
    {
        return HEX_PREFIX + super.toString(bytes, offset, length) + HEX_POSTFIX;
    }

    @Override
    public byte[] toBytes(String string)
    {
        if (string.startsWith(HEX_PREFIX) || string.endsWith(HEX_POSTFIX)) {
            string = string.substring(HEX_PREFIX.length(), string.length() - HEX_PREFIX.length() - HEX_POSTFIX.length());
        }
        return super.toBytes(string);
    }

}
