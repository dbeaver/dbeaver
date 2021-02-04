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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.Writer;

/**
 * CLOB handler
 */
public class OracleCLOBValueHandler extends JDBCContentValueHandler {

    public static final OracleCLOBValueHandler INSTANCE = new OracleCLOBValueHandler();
    public static final int MAX_PART_SIZE = 4000;

    @Override
    public void writeStreamValue(DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull DBSTypedObject type, @NotNull DBDContent object, @NotNull Writer writer) throws DBCException, IOException {
        DBDContentStorage contents = object.getContents(monitor);
        if (DBUtils.isNullValue(contents)) {
            writer.write("NULL");
            return;
        }
        String strValue = ContentUtils.getContentStringValue(monitor, object);
        String[] parts = splitString(strValue);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i > 0) writer.write("||");
            writer.write("TO_CLOB('");
            writer.write(part.replace("'", "''"));
            writer.write("')");
        }
    }

    private static String[] splitString(String strValue) {
        int partCount = strValue.length() / MAX_PART_SIZE;
        if (strValue.length() % MAX_PART_SIZE > 0) partCount++;
        String[] parts = new String[partCount];
        for (int i = 0; i < partCount; i++) {
            int startOffset = i * MAX_PART_SIZE;
            int endOffset = strValue.length() < startOffset + MAX_PART_SIZE ? strValue.length() : startOffset + MAX_PART_SIZE;
            parts[i] = strValue.substring(startOffset, endOffset);
        }
        return parts;
    }
}
