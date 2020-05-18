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
