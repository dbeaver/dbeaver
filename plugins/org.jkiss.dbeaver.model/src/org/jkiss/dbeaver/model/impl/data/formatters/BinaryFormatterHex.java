/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
