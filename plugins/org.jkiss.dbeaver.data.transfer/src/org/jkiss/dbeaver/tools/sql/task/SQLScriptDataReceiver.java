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
package org.jkiss.dbeaver.tools.sql.task;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * SQLScriptDataReceiver
 */
public class SQLScriptDataReceiver implements DBDDataReceiver {

    private Integer rowSize;
    private Writer dumpWriter;
    private List<? extends DBCAttributeMetaData> attributes;

    @Override
    public void fetchStart(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, long offset, long maxRows) throws DBCException {
        if (resultSet == null) {
            return;
        }
        if (dumpWriter != null) {
            DBCResultSetMetaData rsMeta = resultSet.getMeta();
            attributes = rsMeta.getAttributes();
            rowSize = attributes.size();
            try {
                dumpWriter.append("Columns:\t");

                for (DBCAttributeMetaData attribute : attributes) {
                    dumpWriter.append(attribute.getLabel() + "\t");
                }
                dumpWriter.append("\n");
            } catch (IOException e1) {
                throw new DBCException("IOException writing to dumpWriter", e1);
            }
        }
    }

    @Override
    public void fetchRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) throws DBCException {
        if (resultSet == null) {
            return;
        }
        if (dumpWriter != null) {
            try {
                for (int i = 0; i < rowSize; i++) {
                    if (resultSet.getAttributeValue(i) != null) {
                    	DBCAttributeMetaData type = attributes.get(i);
                    	DBDValueHandler valueHandler = DBUtils.findValueHandler(session, type);
                    	Object object = valueHandler.fetchValueObject(session, resultSet, type, i);
                        dumpWriter.append(object.toString()).append("\t");
                    } else {
                        dumpWriter.append("NULL\t");
                    }
                }
                dumpWriter.append("\n");
            } catch (IOException e) {
                throw new DBCException("IOException writing to dumpWriter", e);
            }
        }
    }

    @Override
    public void fetchEnd(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) throws DBCException {
        if (dumpWriter != null) {
            try {
                dumpWriter.flush();
            } catch (IOException e) {
                throw new DBCException("IOException writing to dumpWriter", e);
            }
        }
    }

    @Override
    public void close() {

    }

    public void setDumpWriter(Writer writer) {
        this.dumpWriter = writer;
    }

    public Writer getDumpWriter() {
        return dumpWriter;
    }
}
