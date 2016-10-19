/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
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
        if (contents == null) {
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
