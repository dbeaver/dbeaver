/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
 *
 * Contributors:
 *    Stefan Uhrig - initial implementation
 */
package org.jkiss.dbeaver.ui.gis.panel;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

import java.io.IOException;

public class DBDContentAdapter extends TypeAdapter<DBDContent> {

    @Override
    public DBDContent read(JsonReader reader) throws IOException {
        throw new IOException("Reading is not supported");
    }

    @Override
    public void write(JsonWriter writer, DBDContent content) throws IOException {
        if (content == null) {
            writer.nullValue();
            return;
        }
        String value = content.getDisplayString(DBDDisplayFormat.UI);
        writer.value(value);
    }

}
