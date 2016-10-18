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
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * CLOB handler
 */
public class OracleCLOBValueHandler extends JDBCContentValueHandler {

    public static final OracleCLOBValueHandler INSTANCE = new OracleCLOBValueHandler();

    @Override
    public void writeStreamValue(@NotNull DBCSession session, @NotNull DBSTypedObject type, @NotNull DBDContent object, @NotNull Writer writer) throws DBCException, IOException {
        if (!object.isNull()) {
            writer.write("TO_CLOB(");
        }
        super.writeStreamValue(session, type, object, writer);
        if (!object.isNull()) {
            writer.write(")");
        }
    }
}
