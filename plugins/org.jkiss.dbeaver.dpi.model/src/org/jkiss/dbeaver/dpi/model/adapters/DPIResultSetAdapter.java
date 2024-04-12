/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.dpi.model.adapters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.dpi.DPIResultSet;
import org.jkiss.dbeaver.model.impl.dpi.DPIResultSetColumn;
import org.jkiss.dbeaver.model.impl.dpi.DPIServerSmartProxyDataReceiver;

import java.io.IOException;
import java.util.List;

public class DPIResultSetAdapter extends AbstractTypeAdapter<DBCResultSet> {
    private static final String META = "meta";
    private static final String SESSION = "session";
    private static final String STATEMENT = "statement";
    private static final String ROWS = "rows";
    private final Gson gson;

    public DPIResultSetAdapter(DPIContext context, Gson gson) {
        super(context);
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter jsonWriter, DBCResultSet resultSet) throws IOException {
        DPIResultSet dpiResultSet;
        if (resultSet instanceof DPIResultSet) {
            dpiResultSet = (DPIResultSet) resultSet;
        } else {
            try (var dataReceiver = new DPIServerSmartProxyDataReceiver()) {
                DBCSession session = resultSet.getSession();
                dataReceiver.fetchStart(session, resultSet, 0, 0);
                while (resultSet.nextRow()) {
                    dataReceiver.fetchRow(session, resultSet);
                }
                dataReceiver.fetchEnd(session, resultSet);
                dpiResultSet = dataReceiver.getDpiResultSet();
            } catch (Exception e) {
                throw new IOException("Failed to fetch data from result set: " + e.getMessage(), e);
            }
        }
        jsonWriter.beginObject();

        jsonWriter.name(META);
        jsonWriter.value(gson.toJson(dpiResultSet.getMetaColumns()));
        jsonWriter.name(SESSION);
        jsonWriter.value(gson.toJson(dpiResultSet.getSession()));
        jsonWriter.name(STATEMENT);
        jsonWriter.value(gson.toJson(dpiResultSet.getSourceStatement()));
        jsonWriter.name(ROWS);
        jsonWriter.value(gson.toJson(dpiResultSet.getAllRows()));

        jsonWriter.endObject();
    }

    @Override
    public DBCResultSet read(JsonReader jsonReader) throws IOException {
        DBCSession session = null;
        DBCStatement statement = null;
        List<DPIResultSetColumn> meta = null;
        List<Object[]> rows = null;
        jsonReader.beginObject();
        while (jsonReader.peek() == JsonToken.NAME) {
            String attrName = jsonReader.nextName();
            switch (attrName) {
                case META:
                    meta = gson.fromJson(
                        jsonReader.nextString(),
                        new TypeToken<List<DPIResultSetColumn>>() {
                        }.getType()
                    );
                    break;
                case SESSION:
                    session = gson.fromJson(jsonReader.nextString(), DBCSession.class);
                    break;
                case STATEMENT:
                    statement = gson.fromJson(jsonReader.nextString(), DBCStatement.class);
                    break;
                case ROWS:
                    rows = gson.fromJson(
                        jsonReader.nextString(),
                        new TypeToken<List<Object[]>>() {
                        }.getType()
                    );
                    break;
            }
        }
        jsonReader.endObject();

        return new DPIResultSet(session, statement, meta, rows);
    }
}
