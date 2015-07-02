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
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.utils.Base64;

/**
 * Base64 formatter
 */
public class BinaryFormatterBase64 implements DBDBinaryFormatter {

    @Override
    public String getId()
    {
        return "base64";
    }

    @Override
    public String getTitle()
    {
        return "Base64";
    }

    @Override
    public String toString(byte[] bytes, int offset, int length)
    {
        return Base64.encode(bytes, offset, length);
    }

    @Override
    public byte[] toBytes(String string)
    {
        return Base64.decode(string);
    }

}
