/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.ui.editors.binary.HexUtils;

/**
 * Hex formatter
 */
public class BinaryFormatterHex implements DBDBinaryFormatter {

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
        char[] chars = new char[length * 2];
        for (int i = offset; i < offset + length; i++) {
            String hex = HexUtils.byteToHex[bytes[i] & 0x0ff];
            chars[i * 2] = hex.charAt(0);
            chars[i * 2 + 1] = hex.charAt(1);
        }
        return new String(chars);
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
