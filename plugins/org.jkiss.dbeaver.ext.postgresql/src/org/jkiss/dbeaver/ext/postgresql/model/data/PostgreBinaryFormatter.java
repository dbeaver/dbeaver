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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * PostgreBinaryFormatter
 */
public class PostgreBinaryFormatter extends BinaryFormatterHex {

    public static final PostgreBinaryFormatter INSTANCE = new PostgreBinaryFormatter();
    private static final String HEX_PREFIX = "decode('";
    private static final String HEX_POSTFIX = "','hex')";

    @Override
    public String getId()
    {
        return "pghex";
    }

    @Override
    public String getTitle()
    {
        return "PostgreSQL Hex";
    }

    @Override
    public String toString(byte[] bytes, int offset, int length)
    {
        return HEX_PREFIX + new String(toHexChars(bytes, offset, length)) + HEX_POSTFIX;
    }

    @Override
    public byte[] toBytes(String string)
    {
        if (string.startsWith(HEX_PREFIX)) {
            string = string.substring(
                HEX_PREFIX.length(),
                string.length() - HEX_POSTFIX.length());
        }
        return super.toBytes(string);
    }

}
