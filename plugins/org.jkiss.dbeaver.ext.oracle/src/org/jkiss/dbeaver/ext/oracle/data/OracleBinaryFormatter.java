/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;

/**
 * OracleBinaryFormatter
 */
public class OracleBinaryFormatter extends BinaryFormatterHex {

    public static final OracleBinaryFormatter INSTANCE = new OracleBinaryFormatter();
    private static final String HEX_PREFIX = "'";
    private static final String HEX_POSTFIX = "'";

    @NotNull
    @Override
    public String getId()
    {
        return "orahex";
    }

    @NotNull
    @Override
    public String getTitle()
    {
        return "Oracle Hex";
    }

    @NotNull
    @Override
    public String toString(@NotNull byte[] bytes, int offset, int length)
    {
        return HEX_PREFIX + super.toString(bytes, offset, length) + HEX_POSTFIX;
    }

    @NotNull
    @Override
    public byte[] toBytes(@NotNull String string)
    {
        if (string.startsWith(HEX_PREFIX)) {
            string = string.substring(
                HEX_PREFIX.length(),
                string.length() - HEX_POSTFIX.length());
        }
        return super.toBytes(string);
    }

}
