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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataType;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * YashanDBDataType
 */
public class YashanDBDataType extends OracleDataType {

    public YashanDBDataType(DBSObject owner, ResultSet dbResult) {
        super(owner, dbResult);
        this.methodCache = this.hasMethods ? new YashanDBMethodCache() : null;
    }

    public YashanDBDataType(DBSObject owner, String typeName, boolean persisted) {
        super(owner, typeName, persisted);
        this.methodCache = new YashanDBMethodCache();
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options)
            throws DBCException {
        if (flagPredefined) {
            return "-- Source code not available";
        }
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = YashanDBUtils.getSource(monitor, this, false, true);
        }
        return sourceDeclaration;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sourceDefinition == null && monitor != null) {
            sourceDefinition = YashanDBUtils.getSource(monitor, this, true, false);
        }
        return sourceDefinition;
    }

    class YashanDBMethodCache extends MethodCache {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataType owner)
                throws SQLException {
            // YashanDB custom
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT *" + " FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_TYPE_METHODS "
                    + " m\n" + "WHERE m.OWNER=? AND m.TYPE_NAME=?\n" + "ORDER BY m.METHOD_NO");
            dbStat.setString(1, YashanDBDataType.this.parent.getName());
            dbStat.setString(2, getName());
            return dbStat;
        }

        @Override
        protected YashanDBDataTypeMethod fetchObject(@NotNull JDBCSession session, @NotNull OracleDataType owner,
                                                     @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBDataTypeMethod(session.getProgressMonitor(), YashanDBDataType.this, resultSet);
        }
    }
}
