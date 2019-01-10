/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
